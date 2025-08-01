package org.koitharu.kotatsu.core.prefs

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import org.koitharu.kotatsu.R

@Keep
enum class NavItem(
	@IdRes val id: Int,
	@StringRes val title: Int,
	@DrawableRes val icon: Int,
) {

	HISTORY(R.id.nav_history, R.string.history, R.drawable.ic_nav_home_selector),
	FAVORITES(R.id.nav_favorites, R.string.favourites, R.drawable.ic_nav_favorites_selector),
	LOCAL(R.id.nav_local, R.string.on_device, R.drawable.ic_storage_selector),
	EXPLORE(R.id.nav_explore, R.string.explore, R.drawable.ic_nav_explore_selector),
	SUGGESTIONS(R.id.nav_suggestions, R.string.suggestions, R.drawable.ic_suggestion_selector),
	PROFILE(R.id.nav_feed, R.string.profile, R.drawable.ic_nav_profile_selector),
	UPDATED(R.id.nav_updated, R.string.updated, R.drawable.ic_updated_selector),
	BOOKMARKS(R.id.nav_bookmarks, R.string.bookmarks, R.drawable.ic_bookmark_selector),
	;

	fun isAvailable(settings: AppSettings): Boolean = when (this) {
		SUGGESTIONS -> settings.isSuggestionsEnabled
		UPDATED -> settings.isTrackerEnabled
		PROFILE -> true // Profile is always available
		else -> true
	}
}
