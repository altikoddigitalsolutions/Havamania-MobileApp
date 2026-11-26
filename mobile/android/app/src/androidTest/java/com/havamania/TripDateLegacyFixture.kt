package com.havamania

/** Rebuild only the isolated test table: Android 12 SQLite does not support DROP COLUMN. */
internal fun stripTripDateIsoColumnsForLegacyFixture(db: android.database.sqlite.SQLiteDatabase) {
    val names = mutableListOf<String>()
    val definitions = mutableListOf<String>()
    db.rawQuery("PRAGMA table_info(travel_plans)", null).use { rows ->
        while (rows.moveToNext()) {
            val name = rows.getString(1)
            if (name in setOf("startDateIso", "endDateIso")) continue
            names += "`$name`"
            definitions += buildString {
                append("`$name` ${rows.getString(2)}")
                if (rows.getInt(3) != 0) append(" NOT NULL")
                if (!rows.isNull(4)) append(" DEFAULT ${rows.getString(4)}")
                if (rows.getInt(5) != 0) append(" PRIMARY KEY")
            }
        }
    }
    val indexes = mutableListOf<String>()
    db.rawQuery("SELECT sql FROM sqlite_master WHERE type='index' AND tbl_name='travel_plans' AND sql IS NOT NULL", null).use { rows ->
        while (rows.moveToNext()) indexes += rows.getString(0)
    }
    db.execSQL("CREATE TABLE travel_plans_legacy (${definitions.joinToString(",")})")
    val columns = names.joinToString(",")
    db.execSQL("INSERT INTO travel_plans_legacy ($columns) SELECT $columns FROM travel_plans")
    db.execSQL("DROP TABLE travel_plans")
    db.execSQL("ALTER TABLE travel_plans_legacy RENAME TO travel_plans")
    indexes.forEach { db.execSQL(it) }
}
