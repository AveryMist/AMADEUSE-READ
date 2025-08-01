package org.koitharu.kotatsu.history.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.prefs.observeAsFlow
import org.koitharu.kotatsu.core.prefs.observeAsStateFlow
import org.koitharu.kotatsu.core.ui.util.ReversibleAction
import org.koitharu.kotatsu.core.util.ext.calculateTimeAgo
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.flattenLatest
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.history.domain.HistoryListQuickFilter
import org.koitharu.kotatsu.history.domain.MarkAsReadUseCase
import org.koitharu.kotatsu.history.domain.model.MangaWithHistory
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.list.domain.ListSortOrder
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.domain.QuickFilterListener
import org.koitharu.kotatsu.list.domain.ReadingProgress
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.InfoModel
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.toErrorState
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.history.ui.model.CarouselSectionModel
import org.koitharu.kotatsu.history.ui.model.WelcomeHeaderModel
import org.koitharu.kotatsu.suggestions.domain.SuggestionRepository
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val PAGE_SIZE = 16

@HiltViewModel
class HistoryListViewModel @Inject constructor(
	private val repository: HistoryRepository,
	private val localRepository: LocalMangaRepository,
	private val suggestionRepository: SuggestionRepository,
	private val sourcesRepository: MangaSourcesRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	settings: AppSettings,
	private val mangaListMapper: MangaListMapper,
	private val markAsReadUseCase: MarkAsReadUseCase,
	private val quickFilter: HistoryListQuickFilter,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler), QuickFilterListener by quickFilter {

	private val appSettings = settings

	private val sortOrder: StateFlow<ListSortOrder> = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_HISTORY_ORDER,
		valueProducer = { historySortOrder },
	)

	override val listMode = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_LIST_MODE_HISTORY,
		valueProducer = { historyListMode },
	)

	private val isGroupingEnabled = settings.observeAsFlow(
		key = AppSettings.KEY_HISTORY_GROUPING,
		valueProducer = { isHistoryGroupingEnabled },
	).combine(sortOrder) { g, s ->
		g && s.isGroupingSupported()
	}

	private val limit = MutableStateFlow(PAGE_SIZE)
	private val isPaginationReady = AtomicBoolean(false)

	val isStatsEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_STATS_ENABLED,
		valueProducer = { isStatsEnabled },
	)

	override val content = combine(
		quickFilter.appliedOptions,
		observeHistory(),
		observeLocalManga(),
		observeSuggestions(),
		isGroupingEnabled,
		observeListModeWithTriggers(),
		settings.observeAsFlow(AppSettings.KEY_INCOGNITO_MODE) { isIncognitoModeEnabled },
	) { array ->
		val filters = array[0] as Set<ListFilterOption>
		val historyList = array[1] as List<MangaWithHistory>
		val localList = array[2] as List<Manga>
		val suggestions = array[3] as List<Manga>
		val grouped = array[4] as Boolean
		val mode = array[5] as ListMode
		val incognito = array[6] as Boolean
		mapCombinedList(historyList, localList, suggestions, grouped, mode, filters, incognito)
	}.distinctUntilChanged().onEach {
		isPaginationReady.set(true)
	}.catch { e ->
		emit(listOf(e.toErrorState(canRetry = false)))
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun clearHistory(minDate: Instant?) {
		launchJob(Dispatchers.Default) {
			val stringRes = if (minDate == null) {
				repository.clear()
				R.string.history_cleared
			} else {
				repository.deleteAfter(minDate.toEpochMilli())
				R.string.removed_from_history
			}
			onActionDone.call(ReversibleAction(stringRes, null))
		}
	}

	fun removeNotFavorite() {
		launchJob(Dispatchers.Default) {
			repository.deleteNotFavorite()
			onActionDone.call(ReversibleAction(R.string.removed_from_history, null))
		}
	}

	fun removeFromHistory(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = repository.delete(ids)
			onActionDone.call(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	fun markAsRead(items: Set<Manga>) {
		launchLoadingJob(Dispatchers.Default) {
			markAsReadUseCase(items)
		}
	}

	fun requestMoreItems() {
		if (isPaginationReady.compareAndSet(true, false)) {
			limit.value += PAGE_SIZE
		}
	}

	private fun observeHistory() = combine(
		sortOrder,
		quickFilter.appliedOptions.combineWithSettings(),
		limit,
	) { order, filters, limit ->
		isPaginationReady.set(false)
		repository.observeAllWithHistory(order, filters, limit)
	}.flattenLatest()

	private fun observeLocalManga(): Flow<List<Manga>> {
		return flow {
			val mangaList = mutableListOf<Manga>()
			localRepository.getRawListAsFlow()
				.map { localManga -> localManga.manga }
				.collect { manga ->
					mangaList.add(manga)
				}
			emit(mangaList)
		}
	}

	private fun observeSuggestions(): Flow<List<Manga>> {
		return flow {
			try {
				val suggestions = suggestionRepository.getRandomList(20)
				// Si pas de suggestions générées, essayer de créer des suggestions de base
				if (suggestions.isEmpty()) {
					val basicSuggestions = generateBasicSuggestionsForHome()
					emit(basicSuggestions)
				} else {
					emit(suggestions)
				}
			} catch (e: Exception) {
				emit(emptyList<Manga>())
			}
		}
	}

	private suspend fun generateBasicSuggestionsForHome(): List<Manga> {
		return runCatchingCancellable {
			// Essayer de récupérer quelques manga populaires des sources activées
			val sources = sourcesRepository.getEnabledSources().take(2)
			val suggestions = mutableListOf<Manga>()

			for (source in sources) {
				try {
					val repository = mangaRepositoryFactory.create(source)
					// Récupérer les manga populaires (premier ordre de tri disponible)
					val sortOrders = repository.sortOrders
					val popularOrder = sortOrders.firstOrNull()
					val list = repository.getList(
						offset = 0,
						order = popularOrder,
						filter = null
					).take(5) // 5 par source pour avoir assez de variété
					suggestions.addAll(list)
					if (suggestions.size >= 10) break // Limite pour l'accueil
				} catch (e: Exception) {
					// Ignorer les erreurs de sources individuelles
					continue
				}
			}
			suggestions.shuffled().take(10)
		}.getOrDefault(emptyList())
	}

	private suspend fun mapCombinedList(
		historyList: List<MangaWithHistory>,
		localList: List<Manga>,
		suggestions: List<Manga>,
		grouped: Boolean,
		mode: ListMode,
		filters: Set<ListFilterOption>,
		isIncognito: Boolean,
	): List<ListModel> {
		val result = ArrayList<ListModel>()

		// Add filter info if needed
		quickFilter.filterItem(filters)?.let(result::add)

		// Add incognito mode info if enabled
		if (isIncognito) {
			result += InfoModel(
				key = AppSettings.KEY_INCOGNITO_MODE,
				title = R.string.incognito_mode,
				text = R.string.incognito_mode_hint,
				icon = R.drawable.ic_incognito,
			)
		}

		// Add welcome header
		result += WelcomeHeaderModel()

		// Add recent manga carousel if not empty (limit to 10)
		if (historyList.isNotEmpty()) {
			val recentManga = historyList.take(10).map { it.manga }
			result += CarouselSectionModel(
				key = "recent_manga",
				titleRes = R.string.recent_manga,
				manga = recentManga,
				mode = mode
			)
		}

		// Add random downloaded manga carousel if not empty (limit to 10)
		if (localList.isNotEmpty()) {
			// Use a stable seed based on the list content to ensure consistent ordering
			val seed = localList.map { it.id }.hashCode()
			val randomLocalManga = localList.shuffled(kotlin.random.Random(seed)).take(10)
			result += CarouselSectionModel(
				key = "downloaded_manga",
				titleRes = R.string.on_device,
				manga = randomLocalManga,
				mode = mode
			)
		}

		// Add suggestions carousel if not empty (limit to 10)
		if (suggestions.isNotEmpty()) {
			val filteredSuggestions = if (appSettings.isSuggestionsCarouselExcludeNsfw) {
				suggestions.filter { !it.isNsfw }
			} else {
				suggestions
			}
			val limitedSuggestions = filteredSuggestions.take(10)
			if (limitedSuggestions.isNotEmpty()) {
				result += CarouselSectionModel(
					key = "suggestions",
					titleRes = R.string.suggestions,
					manga = limitedSuggestions,
					mode = mode
				)
			}
		}



		// Handle empty state
		if (result.isEmpty() || (result.size == 1 && result[0] is InfoModel)) {
			return if (filters.isEmpty()) {
				listOf(getEmptyState(hasFilters = false))
			} else {
				listOfNotNull(quickFilter.filterItem(filters), getEmptyState(hasFilters = true))
			}
		}

		return result
	}

	private suspend fun mapList(
		list: List<MangaWithHistory>,
		grouped: Boolean,
		mode: ListMode,
		filters: Set<ListFilterOption>,
		isIncognito: Boolean,
	): List<ListModel> {
		if (list.isEmpty()) {
			return if (filters.isEmpty()) {
				listOf(getEmptyState(hasFilters = false))
			} else {
				listOfNotNull(quickFilter.filterItem(filters), getEmptyState(hasFilters = true))
			}
		}
		val result = ArrayList<ListModel>((if (grouped) (list.size * 1.4).toInt() else list.size) + 2)
		quickFilter.filterItem(filters)?.let(result::add)
		if (isIncognito) {
			result += InfoModel(
				key = AppSettings.KEY_INCOGNITO_MODE,
				title = R.string.incognito_mode,
				text = R.string.incognito_mode_hint,
				icon = R.drawable.ic_incognito,
			)
		}
		val order = sortOrder.value
		var prevHeader: ListHeader? = null
		var isEmpty = true
		for ((manga, history) in list) {
			isEmpty = false
			if (grouped) {
				val header = history.header(order)
				if (header != prevHeader) {
					if (header != null) {
						result += header
					}
					prevHeader = header
				}
			}
			result += mangaListMapper.toListModel(manga, mode)
		}
		if (filters.isNotEmpty() && isEmpty) {
			result += getEmptyState(hasFilters = true)
		}
		return result
	}

	private fun MangaHistory.header(order: ListSortOrder): ListHeader? = when (order) {
		ListSortOrder.LAST_READ,
		ListSortOrder.LONG_AGO_READ -> ListHeader(calculateTimeAgo(updatedAt))

		ListSortOrder.OLDEST,
		ListSortOrder.NEWEST -> ListHeader(calculateTimeAgo(createdAt))

		ListSortOrder.UNREAD,
		ListSortOrder.PROGRESS -> ListHeader(
			when {
				ReadingProgress.isCompleted(percent) -> R.string.status_completed
				percent in 0f..0.01f -> R.string.status_planned
				percent in 0f..1f -> R.string.status_reading
				else -> R.string.unknown
			},
		)

		ListSortOrder.ALPHABETIC,
		ListSortOrder.ALPHABETIC_REVERSE,
		ListSortOrder.RELEVANCE,
		ListSortOrder.NEW_CHAPTERS,
		ListSortOrder.UPDATED,
		ListSortOrder.RATING -> null
	}

	private fun getEmptyState(hasFilters: Boolean) = if (hasFilters) {
		EmptyState(
			icon = R.drawable.ic_empty_history,
			textPrimary = R.string.nothing_found,
			textSecondary = R.string.text_empty_holder_secondary_filtered,
			actionStringRes = R.string.reset_filter,
		)
	} else {
		EmptyState(
			icon = R.drawable.ic_empty_history,
			textPrimary = R.string.text_history_holder_primary,
			textSecondary = R.string.text_history_holder_secondary,
			actionStringRes = 0,
		)
	}
}
