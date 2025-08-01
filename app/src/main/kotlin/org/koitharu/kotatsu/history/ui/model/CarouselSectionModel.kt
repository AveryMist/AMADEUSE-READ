package org.koitharu.kotatsu.history.ui.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

data class CarouselSectionModel(
	val key: String,
	@StringRes val titleRes: Int,
	val manga: List<Manga>,
	val mode: ListMode,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is CarouselSectionModel && other.key == key
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is CarouselSectionModel && previousState.manga != manga) {
			"manga_changed"
		} else {
			null
		}
	}
}
