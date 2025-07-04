package com.ml.tomatoscan.data.database.converters

import androidx.room.TypeConverter
import org.json.JSONArray

class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val jsonArray = JSONArray()
        for (item in value) {
            jsonArray.put(item)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            // Return empty list if parsing fails
        }
        return list
    }
}
