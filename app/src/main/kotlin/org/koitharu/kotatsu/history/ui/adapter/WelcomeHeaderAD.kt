package org.koitharu.kotatsu.history.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koitharu.kotatsu.databinding.ItemWelcomeHeaderBinding
import org.koitharu.kotatsu.history.ui.model.WelcomeHeaderModel
import org.koitharu.kotatsu.list.ui.model.ListModel

fun welcomeHeaderAD() = adapterDelegateViewBinding<WelcomeHeaderModel, ListModel, ItemWelcomeHeaderBinding>(
	{ inflater, parent -> ItemWelcomeHeaderBinding.inflate(inflater, parent, false) },
) {
	bind {
		// The layout already contains the static welcome text
		// No dynamic binding needed for now
	}
}
