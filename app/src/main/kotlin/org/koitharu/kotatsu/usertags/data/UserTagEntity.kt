package org.koitharu.kotatsu.usertags.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "user_tags")
data class UserTagEntity(
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "tag_id") val tagId: Long,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "color") val color: Int?, // ARGB color value, nullable for default color
	@ColumnInfo(name = "created_at") val createdAt: Long,
	@ColumnInfo(name = "sort_key") val sortKey: Int,
	@ColumnInfo(name = "deleted_at") val deletedAt: Long,
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as UserTagEntity

		if (tagId != other.tagId) return false
		if (title != other.title) return false
		if (color != other.color) return false
		if (createdAt != other.createdAt) return false
		if (sortKey != other.sortKey) return false
		return deletedAt == other.deletedAt
	}

	override fun hashCode(): Int {
		var result = tagId.hashCode()
		result = 31 * result + title.hashCode()
		result = 31 * result + (color ?: 0)
		result = 31 * result + createdAt.hashCode()
		result = 31 * result + sortKey
		result = 31 * result + deletedAt.hashCode()
		return result
	}
}
