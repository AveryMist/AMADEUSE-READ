package org.koitharu.kotatsu.explore.ui.model

import androidx.annotation.StringRes
import org.koitharu.kotatsu.list.ui.model.ListModel

/**
 * Header item for grouping sources by category
 */
data class SourceCategoryHeader(
    @StringRes val titleResId: Int,
    val titleText: String? = null,
    val category: SourceCategory
) : ListModel {

    override fun areItemsTheSame(other: ListModel): Boolean {
        return other is SourceCategoryHeader && other.category == category
    }

    enum class SourceCategory {
        PINNED,      // Pinned/Popular sources
        ENABLED,     // Regular enabled sources  
        DISABLED,    // Disabled sources
        NSFW         // NSFW sources (if separated)
    }
}
