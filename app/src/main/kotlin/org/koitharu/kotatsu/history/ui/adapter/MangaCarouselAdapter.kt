package org.koitharu.kotatsu.history.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.request.allowRgb565
import coil3.request.transformations
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.image.TrimTransformation
import org.koitharu.kotatsu.core.util.ext.defaultPlaceholders
import org.koitharu.kotatsu.core.util.ext.enqueueWith
import org.koitharu.kotatsu.core.util.ext.mangaExtra
import org.koitharu.kotatsu.core.util.ext.newImageRequest
import org.koitharu.kotatsu.core.util.ext.textAndVisible
import org.koitharu.kotatsu.databinding.ItemMangaCarouselBinding
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.parsers.model.Manga

class MangaCarouselAdapter(
	private val coil: ImageLoader,
	private val lifecycleOwner: LifecycleOwner,
	private val listener: MangaListListener,
) : ListAdapter<Manga, MangaCarouselAdapter.ViewHolder>(DiffCallback()) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val binding = ItemMangaCarouselBinding.inflate(
			LayoutInflater.from(parent.context),
			parent,
			false
		)
		return ViewHolder(binding)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	inner class ViewHolder(
		private val binding: ItemMangaCarouselBinding,
	) : RecyclerView.ViewHolder(binding.root) {

		init {
			binding.root.setOnClickListener {
				val position = bindingAdapterPosition
				if (position != RecyclerView.NO_POSITION) {
					listener.onItemClick(getItem(position), itemView)
				}
			}
			binding.root.setOnLongClickListener {
				val position = bindingAdapterPosition
				if (position != RecyclerView.NO_POSITION) {
					listener.onItemLongClick(getItem(position), itemView)
				} else {
					false
				}
			}
		}

		fun bind(manga: Manga) {
			binding.textViewTitle.text = manga.title
			binding.textViewSubtitle.textAndVisible = manga.altTitle

			binding.imageViewCover.newImageRequest(lifecycleOwner, manga.coverUrl)?.run {
				defaultPlaceholders(binding.root.context)
				allowRgb565(true)
				transformations(TrimTransformation())
				mangaExtra(manga)
				enqueueWith(coil)
			}

			// Hide badge for now - could be used for unread chapters count later
			binding.badge.isVisible = false
		}
	}

	private class DiffCallback : DiffUtil.ItemCallback<Manga>() {
		override fun areItemsTheSame(oldItem: Manga, newItem: Manga): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: Manga, newItem: Manga): Boolean {
			return oldItem == newItem
		}
	}
}
