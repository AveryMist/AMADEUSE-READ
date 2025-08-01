package org.koitharu.kotatsu.explore.ui.model

import org.koitharu.kotatsu.core.model.MangaSourceInfo
import org.koitharu.kotatsu.core.model.isNsfw
import org.koitharu.kotatsu.parsers.model.MangaParserSource

/**
 * Helper object to determine source badges and status indicators
 */
object SourceBadgeHelper {

    /**
     * Determines if a source should show the NSFW badge
     */
    fun shouldShowNsfwBadge(source: MangaSourceInfo): Boolean {
        return source.isNsfw()
    }

    /**
     * Determines if a source should show the Popular badge
     * Currently uses pinned status as a proxy for popularity
     * You can customize this logic based on your requirements
     */
    fun shouldShowPopularBadge(source: MangaSourceInfo): Boolean {
        return source.isPinned
    }

    /**
     * Determines if a source should show the New badge
     * This is a placeholder implementation - you can customize based on:
     * - Source addition date
     * - App version when source was added
     * - User preferences
     */
    fun shouldShowNewBadge(source: MangaSourceInfo): Boolean {
        // Example logic: show "New" for specific sources
        // You can implement your own logic here
        return when (source.name.lowercase()) {
            // Add source names that should show "New" badge
            "franime.fr" -> true
            else -> false
        }
    }

    /**
     * Determines if a source should show the status indicator
     */
    fun shouldShowStatusIndicator(source: MangaSourceInfo): Boolean {
        return !source.isEnabled
    }

    /**
     * Gets the appropriate status icon resource ID
     */
    fun getStatusIconResId(source: MangaSourceInfo): Int? {
        return when {
            !source.isEnabled -> org.koitharu.kotatsu.R.drawable.ic_disable
            source.mangaSource is MangaParserSource && source.mangaSource.isBroken -> org.koitharu.kotatsu.R.drawable.ic_error_large
            else -> null
        }
    }

    /**
     * Determines the priority order for displaying badges
     * Higher priority badges are shown first
     */
    fun getBadgePriority(badgeType: BadgeType): Int {
        return when (badgeType) {
            BadgeType.NSFW -> 3
            BadgeType.NEW -> 2
            BadgeType.POPULAR -> 1
        }
    }

    enum class BadgeType {
        NSFW,
        POPULAR,
        NEW
    }
}
