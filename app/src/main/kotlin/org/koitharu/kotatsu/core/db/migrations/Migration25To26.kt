package org.koitharu.kotatsu.core.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration25To26 : Migration(25, 26) {

	override fun migrate(db: SupportSQLiteDatabase) {
		// Create user_tags table
		db.execSQL("""
			CREATE TABLE IF NOT EXISTS user_tags (
				tag_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
				title TEXT NOT NULL,
				color INTEGER,
				created_at INTEGER NOT NULL,
				sort_key INTEGER NOT NULL,
				deleted_at INTEGER NOT NULL DEFAULT 0
			)
		""".trimIndent())

		// Create manga_user_tags table
		db.execSQL("""
			CREATE TABLE IF NOT EXISTS manga_user_tags (
				manga_id INTEGER NOT NULL,
				tag_id INTEGER NOT NULL,
				created_at INTEGER NOT NULL,
				deleted_at INTEGER NOT NULL DEFAULT 0,
				PRIMARY KEY (manga_id, tag_id),
				FOREIGN KEY (manga_id) REFERENCES manga(manga_id) ON DELETE CASCADE,
				FOREIGN KEY (tag_id) REFERENCES user_tags(tag_id) ON DELETE CASCADE
			)
		""".trimIndent())

		// Create indices for better performance
		db.execSQL("CREATE INDEX IF NOT EXISTS index_manga_user_tags_manga_id ON manga_user_tags (manga_id)")
		db.execSQL("CREATE INDEX IF NOT EXISTS index_manga_user_tags_tag_id ON manga_user_tags (tag_id)")
	}
}
