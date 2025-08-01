package org.koitharu.kotatsu.favourites.ui.carousel

import android.os.Bundle
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.databinding.FragmentListBinding
import org.koitharu.kotatsu.history.ui.HistoryListAdapter
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.list.ui.size.DynamicItemSizeResolver

@AndroidEntryPoint
class FavouritesCarouselFragment : MangaListFragment() {

	override val viewModel by viewModels<FavouritesCarouselViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		// MangaListFragment gère automatiquement l'adapter et la configuration
	}

	override fun onScrolledToEnd() {
		// Pas de pagination pour les favoris
	}

	override fun onEmptyActionClick() {
		router.openFavoriteCategories()
	}

	override fun onCreateAdapter() = HistoryListAdapter(
		coil,
		viewLifecycleOwner,
		this,
		DynamicItemSizeResolver(resources, settings, adjustWidth = false),
	)
}
