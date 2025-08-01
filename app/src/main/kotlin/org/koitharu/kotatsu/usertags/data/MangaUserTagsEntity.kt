package org.koitharu.kotatsu.usertags.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

import org.koitharu.kotatsu.core.db.entity.MangaEntity

@Entity(
	tableName = "manga_user_tags",
	primaryKeys = ["manga_id", "tag_id"],
	foreignKeys = [
		ForeignKey(
			entity = MangaEntity::class,
			parentColumns = ["manga_id"],
			childColumns = ["manga_id"],
			onDelete = ForeignKey.CASCADE
		),
		ForeignKey(
			entity = UserTagEntity::class,
			parentColumns = ["tag_id"],
			childColumns = ["tag_id"],
			onDelete = ForeignKey.CASCADE
		)
	]
)
data class MangaUserTagsEntity(
	@ColumnInfo(name = "manga_id", index = true) val mangaId: Long,
	@ColumnInfo(name = "tag_id", index = true) val tagId: Long,
	@ColumnInfo(name = "created_at") val createdAt: Long,
	@ColumnInfo(name = "deleted_at") val deletedAt: Long,
)
