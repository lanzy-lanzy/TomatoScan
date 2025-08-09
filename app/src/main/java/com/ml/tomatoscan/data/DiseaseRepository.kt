package com.ml.tomatoscan.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ml.tomatoscan.models.Disease
import java.io.IOException

class DiseaseRepository(private val context: Context) {

    fun getDiseases(): List<Disease> {
        val jsonString: String
        try {
            jsonString = context.assets.open("diseases.json").bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyList()
        }
        val listType = object : TypeToken<List<Disease>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }

    fun getDiseaseByName(name: String): Disease? {
        return getDiseases().find { it.name == name }
    }
}
