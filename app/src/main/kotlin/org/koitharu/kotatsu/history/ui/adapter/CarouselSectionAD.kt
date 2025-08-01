package org.koitharu.kotatsu.history.ui.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.databinding.ItemCarouselSectionBinding
import org.koitharu.kotatsu.history.ui.model.CarouselSectionModel
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.model.ListModel

fun carouselSectionAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: MangaListListener,
) = adapterDelegateViewBinding<CarouselSectionModel, ListModel, ItemCarouselSectionBinding>(
	{ inflater, parent -> ItemCarouselSectionBinding.inflate(inflater, parent, false) },
) {
	val carouselAdapter = MangaCarouselAdapter(coil, lifecycleOwner, listener)
	
	binding.recyclerViewCarousel.apply {
		layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
		adapter = carouselAdapter
		setHasFixedSize(true)
	}

	bind {
		binding.textViewTitle.setText(item.titleRes)
		carouselAdapter.submitList(item.manga)
	}
}
