package com.taskify.app.data.local.converter

import androidx.room.TypeConverter

/**
 * Room TypeConverters for types that SQLite can't store natively.
 * Currently a thin layer — dates are stored as Long epoch-millis directly
 * in entities, so conversion happens at the domain mapper layer instead.
 * This keeps Room entities as simple data containers.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
}
