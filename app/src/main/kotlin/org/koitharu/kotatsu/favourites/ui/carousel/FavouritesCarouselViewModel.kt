package org.koitharu.kotatsu.favourites.ui.carousel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.download.ui.worker.DownloadWorker
import org.koitharu.kotatsu.list.ui.MangaListViewModel
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.history.domain.model.MangaWithHistory
import org.koitharu.kotatsu.history.ui.model.CarouselSectionModel
import org.koitharu.kotatsu.history.ui.model.WelcomeHeaderModel
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.list.domain.ListSortOrder
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.tracker.domain.model.TrackingLogItem
import javax.inject.Inject

@HiltViewModel
class FavouritesCarouselViewModel @Inject constructor(
	private val favouritesRepository: FavouritesRepository,
	private val historyRepository: HistoryRepository,
	private val trackingRepository: TrackingRepository,
	settings: AppSettings,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler) {

	override val content: StateFlow<List<ListModel>> = combine(
		observeRecentlyAdded(),
		observeInProgress(),
		observeNewChapters(),
		observeListModeWithTriggers(),
	) { recentlyAdded, inProgress, newChapters, mode ->
		buildCarouselList(recentlyAdded, inProgress, newChapters, mode)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	private fun observeRecentlyAdded(): Flow<List<Manga>> {
		return favouritesRepository.observeAll(ListSortOrder.NEWEST, emptySet<ListFilterOption>(), 10)
	}

	private fun observeInProgress(): Flow<List<Manga>> {
		return favouritesRepository.observeAll(ListSortOrder.NEWEST, emptySet<ListFilterOption>(), 50)
			.combine(historyRepository.observeAllWithHistory(ListSortOrder.NEWEST, emptySet<ListFilterOption>(), 50)) { favorites, historyList ->
				try {
					val inProgressFavorites = mutableListOf<Manga>()
					val historyMap = historyList.associateBy { it.manga.id }

					for (manga in favorites) {
						val history = historyMap[manga.id]
						if (history != null && history.history.percent > 0 && history.history.percent < 1.0f) {
							inProgressFavorites.add(manga)
						}
						if (inProgressFavorites.size >= 10) break
					}

					inProgressFavorites
				} catch (e: Exception) {
					emptyList<Manga>()
				}
			}
	}

	private fun observeNewChapters(): Flow<List<TrackingLogItem>> {
		return trackingRepository.observeTrackingLog(10, emptySet<ListFilterOption>())
	}

	private fun buildCarouselList(
		recentlyAdded: List<Manga>,
		inProgress: List<Manga>,
		newChapters: List<TrackingLogItem>,
		mode: ListMode,
	): List<ListModel> {
		val result = mutableListOf<ListModel>()

		// Add welcome header
		result += WelcomeHeaderModel()

		// Add new chapters carousel if not empty
		if (newChapters.isNotEmpty()) {
			result += CarouselSectionModel(
				key = "new_chapters",
				titleRes = R.string.new_chapters,
				manga = newChapters.take(10).map { it.manga },
				mode = mode
			)
		}

		// Add recently added favorites carousel if not empty
		if (recentlyAdded.isNotEmpty()) {
			result += CarouselSectionModel(
				key = "recently_added_favorites",
				titleRes = R.string.recently_added,
				manga = recentlyAdded.take(10),
				mode = mode
			)
		}

		// Add in progress favorites carousel if not empty
		if (inProgress.isNotEmpty()) {
			result += CarouselSectionModel(
				key = "in_progress_favorites",
				titleRes = R.string.in_progress,
				manga = inProgress.take(10),
				mode = mode
			)
		}

		// Handle empty state
		if (result.size <= 1) { // Only welcome header
			return listOf(
				EmptyState(
					icon = R.drawable.ic_empty_favourites,
					textPrimary = R.string.text_empty_holder_primary,
					textSecondary = R.string.empty_favourite_categories,
					actionStringRes = 0,
				)
			)
		}

		return result
	}
}
