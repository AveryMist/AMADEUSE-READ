package org.koitharu.kotatsu.explore.ui.adapter

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil3.ImageLoader
import coil3.request.allowRgb565
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import coil3.request.transformations
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSourceInfo
import org.koitharu.kotatsu.core.model.getSummary
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.core.parser.favicon.faviconUri
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.image.AnimatedFaviconDrawable
import org.koitharu.kotatsu.core.ui.image.FaviconDrawable
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.drawableStart
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.mangaSourceExtra
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.recyclerView
import org.koitharu.kotatsu.core.util.ext.setProgressIcon
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemExploreButtonsBinding
import org.koitharu.kotatsu.databinding.ItemExploreSourceGridBinding
import org.koitharu.kotatsu.databinding.ItemExploreSourceListBinding
import org.koitharu.kotatsu.databinding.ItemRecommendationBinding
import org.koitharu.kotatsu.databinding.ItemRecommendationMangaBinding
import org.koitharu.kotatsu.explore.ui.model.ExploreButtons
import org.koitharu.kotatsu.explore.ui.model.MangaSourceItem
import org.koitharu.kotatsu.explore.ui.model.RecommendationsItem
import org.koitharu.kotatsu.explore.ui.model.SourceBadgeHelper
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.MangaCompactListModel
import org.koitharu.kotatsu.parsers.model.Manga

fun exploreButtonsAD(
	clickListener: View.OnClickListener,
) = adapterDelegateViewBinding<ExploreButtons, ListModel, ItemExploreButtonsBinding>(
	{ layoutInflater, parent -> ItemExploreButtonsBinding.inflate(layoutInflater, parent, false) },
) {

	// Set up click listeners for the new card-based design
	binding.cardBookmarks.setOnClickListener {
		val view = it.apply { id = R.id.button_bookmarks }
		clickListener.onClick(view)
	}
	binding.cardDownloads.setOnClickListener {
		val view = it.apply { id = R.id.button_downloads }
		clickListener.onClick(view)
	}
	binding.cardLocal.setOnClickListener {
		val view = it.apply { id = R.id.button_local }
		clickListener.onClick(view)
	}
	binding.cardRandom.setOnClickListener {
		val view = it.apply { id = R.id.button_random }
		clickListener.onClick(view)
	}

	bind {
		// Handle random loading state with the new ImageView
		if (item.isRandomLoading) {
			binding.imageViewRandom.setImageResource(R.drawable.ic_timelapse) // Use a loading icon
			binding.cardRandom.isClickable = false
		} else {
			binding.imageViewRandom.setImageResource(R.drawable.ic_dice)
			binding.cardRandom.isClickable = true
		}
	}
}

fun exploreRecommendationItemAD(
	coil: ImageLoader,
	itemClickListener: OnListItemClickListener<Manga>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<RecommendationsItem, ListModel, ItemRecommendationBinding>(
	{ layoutInflater, parent -> ItemRecommendationBinding.inflate(layoutInflater, parent, false) },
) {

	val adapter = BaseListAdapter<MangaCompactListModel>()
		.addDelegate(ListItemType.MANGA_LIST, recommendationMangaItemAD(coil, itemClickListener, lifecycleOwner))
	binding.pager.adapter = adapter
	binding.pager.recyclerView?.isNestedScrollingEnabled = false
	binding.dots.bindToViewPager(binding.pager)

	// Set up the "More" button click listener
	binding.buttonMore.setOnClickListener {
		// Simple click action - can be customized later
		// For now, just a placeholder
	}

	bind {
		adapter.items = item.manga
	}
}

fun recommendationMangaItemAD(
	coil: ImageLoader,
	itemClickListener: OnListItemClickListener<Manga>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<MangaCompactListModel, MangaCompactListModel, ItemRecommendationMangaBinding>(
	{ layoutInflater, parent -> ItemRecommendationMangaBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v ->
		itemClickListener.onItemClick(item.manga, v)
	}
	bind {
		binding.textViewTitle.text = item.manga.title
		binding.textViewSubtitle.textAndVisible = item.subtitle
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.manga.coverUrl)?.run {
			defaultPlaceholders(context)
			allowRgb565(true)
			transformations(TrimTransformation())
			mangaSourceExtra(item.manga.source)
			enqueueWith(coil)
		}
	}
}


fun exploreSourceListItemAD(
	coil: ImageLoader,
	listener: OnListItemClickListener<MangaSourceItem>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<MangaSourceItem, ListModel, ItemExploreSourceListBinding>(
	{ layoutInflater, parent ->
		ItemExploreSourceListBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
	on = { item, _, _ -> item is MangaSourceItem && !item.isGrid },
) {

	AdapterDelegateClickListenerAdapter(this, listener).attach(itemView)
	val iconPinned = ContextCompat.getDrawable(context, R.drawable.ic_pin_small)

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		binding.textViewTitle.drawableStart = if (item.source.isPinned) iconPinned else null
		binding.textViewSubtitle.text = item.source.getSummary(context)

		// Setup source icon
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
		binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			fallback(fallbackIcon)
			placeholder(AnimatedFaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name))
			error(fallbackIcon)
			mangaSourceExtra(item.source)
			enqueueWith(coil)
		}

		// Setup status indicator
		binding.imageViewStatus.apply {
			val iconResId = SourceBadgeHelper.getStatusIconResId(item.source)
			if (iconResId != null) {
				setImageResource(iconResId)
				visibility = View.VISIBLE
			} else {
				visibility = View.GONE
			}
		}

		// Setup badges
		setupSourceBadges(binding, item.source)
	}
}

fun exploreSourceGridItemAD(
	coil: ImageLoader,
	listener: OnListItemClickListener<MangaSourceItem>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<MangaSourceItem, ListModel, ItemExploreSourceGridBinding>(
	{ layoutInflater, parent ->
		ItemExploreSourceGridBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
	on = { item, _, _ -> item is MangaSourceItem && item.isGrid },
) {

	AdapterDelegateClickListenerAdapter(this, listener).attach(itemView)
	val iconPinned = ContextCompat.getDrawable(context, R.drawable.ic_pin_small)

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		binding.textViewTitle.drawableStart = if (item.source.isPinned) iconPinned else null

		// Setup source icon
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Large, item.source.name)
		binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			fallback(fallbackIcon)
			placeholder(AnimatedFaviconDrawable(context, R.style.FaviconDrawable_Large, item.source.name))
			error(fallbackIcon)
			mangaSourceExtra(item.source)
			enqueueWith(coil)
		}

		// Setup status indicator
		binding.imageViewStatus.apply {
			val iconResId = SourceBadgeHelper.getStatusIconResId(item.source)
			if (iconResId != null) {
				setImageResource(iconResId)
				visibility = View.VISIBLE
			} else {
				visibility = View.GONE
			}
		}

		// Setup badges
		setupSourceBadges(binding, item.source)
	}
}

private fun setupSourceBadges(binding: ItemExploreSourceListBinding, source: MangaSourceInfo) {
	// NSFW Badge
	binding.textViewNsfw.isVisible = SourceBadgeHelper.shouldShowNsfwBadge(source)

	// Popular Badge
	binding.textViewPopular.isVisible = SourceBadgeHelper.shouldShowPopularBadge(source)

	// New Badge
	binding.textViewNew.isVisible = SourceBadgeHelper.shouldShowNewBadge(source)
}

private fun setupSourceBadges(binding: ItemExploreSourceGridBinding, source: MangaSourceInfo) {
	// NSFW Badge
	binding.textViewNsfw.isVisible = SourceBadgeHelper.shouldShowNsfwBadge(source)

	// Popular Badge
	binding.textViewPopular.isVisible = SourceBadgeHelper.shouldShowPopularBadge(source)

	// New Badge
	binding.textViewNew.isVisible = SourceBadgeHelper.shouldShowNewBadge(source)
}
