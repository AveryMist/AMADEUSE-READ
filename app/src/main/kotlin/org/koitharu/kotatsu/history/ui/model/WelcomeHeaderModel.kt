package org.koitharu.kotatsu.history.ui.model

import org.koitharu.kotatsu.list.ui.model.ListModel

data class WelcomeHeaderModel(
	private val id: String = "welcome_header"
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is WelcomeHeaderModel
	}

	override fun getChangePayload(previousState: ListModel): Any? = null
}
