package org.koitharu.kotatsu.usertags.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserTagsDao {

	/** USER TAGS QUERIES **/

	@Query("SELECT * FROM user_tags WHERE deleted_at = 0 ORDER BY sort_key")
	abstract suspend fun findAllTags(): List<UserTagEntity>

	@Query("SELECT * FROM user_tags WHERE deleted_at = 0 ORDER BY sort_key")
	abstract fun observeAllTags(): Flow<List<UserTagEntity>>

	@Query("SELECT * FROM user_tags WHERE tag_id = :tagId AND deleted_at = 0")
	abstract suspend fun findTag(tagId: Long): UserTagEntity?

	@Query("SELECT * FROM user_tags WHERE title = :title AND deleted_at = 0")
	abstract suspend fun findTagByTitle(title: String): UserTagEntity?

	@Insert(onConflict = OnConflictStrategy.ABORT)
	abstract suspend fun insertTag(tag: UserTagEntity): Long

	@Query("UPDATE user_tags SET title = :title, color = :color WHERE tag_id = :tagId")
	abstract suspend fun updateTag(tagId: Long, title: String, color: Int?)

	@Query("UPDATE user_tags SET deleted_at = :deletedAt WHERE tag_id = :tagId")
	protected abstract suspend fun setTagDeletedAt(tagId: Long, deletedAt: Long)

	suspend fun deleteTag(tagId: Long) = setTagDeletedAt(tagId, System.currentTimeMillis())

	@Query("SELECT MAX(sort_key) FROM user_tags WHERE deleted_at = 0")
	protected abstract suspend fun getMaxSortKey(): Int?

	suspend fun getNextSortKey(): Int {
		return (getMaxSortKey() ?: 0) + 1
	}

	/** MANGA-TAG RELATIONSHIP QUERIES **/

	@Query("SELECT user_tags.* FROM user_tags INNER JOIN manga_user_tags ON user_tags.tag_id = manga_user_tags.tag_id WHERE manga_user_tags.manga_id = :mangaId AND user_tags.deleted_at = 0 AND manga_user_tags.deleted_at = 0 ORDER BY user_tags.sort_key")
	abstract suspend fun findTagsForManga(mangaId: Long): List<UserTagEntity>

	@Query("SELECT user_tags.* FROM user_tags INNER JOIN manga_user_tags ON user_tags.tag_id = manga_user_tags.tag_id WHERE manga_user_tags.manga_id = :mangaId AND user_tags.deleted_at = 0 AND manga_user_tags.deleted_at = 0 ORDER BY user_tags.sort_key")
	abstract fun observeTagsForManga(mangaId: Long): Flow<List<UserTagEntity>>

	@Query("SELECT COUNT(*) FROM manga_user_tags WHERE tag_id = :tagId AND deleted_at = 0")
	abstract suspend fun countMangaWithTag(tagId: Long): Int

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract suspend fun insertMangaTag(mangaTag: MangaUserTagsEntity)

	@Query("UPDATE manga_user_tags SET deleted_at = :deletedAt WHERE manga_id = :mangaId AND tag_id = :tagId")
	protected abstract suspend fun setMangaTagDeletedAt(mangaId: Long, tagId: Long, deletedAt: Long)

	suspend fun deleteMangaTag(mangaId: Long, tagId: Long) = setMangaTagDeletedAt(
		mangaId = mangaId,
		tagId = tagId,
		deletedAt = System.currentTimeMillis()
	)

	@Query("SELECT EXISTS(SELECT 1 FROM manga_user_tags WHERE manga_id = :mangaId AND tag_id = :tagId AND deleted_at = 0)")
	abstract suspend fun isMangaTagged(mangaId: Long, tagId: Long): Boolean
}
