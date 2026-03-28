package com.example.kotlinfrontend.data.local

import android.content.Context
import com.example.kotlinfrontend.data.AppJson
import com.example.kotlinfrontend.data.model.DictionaryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

class SeedDictionaryDataSource(private val context: Context) {
    private val mutex = Mutex()
    private var cachedEntries: List<DictionaryEntry>? = null

    suspend fun load(): List<DictionaryEntry> = mutex.withLock {
        cachedEntries ?: loadFromAssets().also { cachedEntries = it }
    }

    private suspend fun loadFromAssets(): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        val rawJson = context.assets.open("dictionary_seed.json")
            .bufferedReader()
            .use { reader -> reader.readText() }
        AppJson.instance.decodeFromString(ListSerializer(DictionaryEntry.serializer()), rawJson)
            .filter { entry -> entry.isActive }
            .sortedBy { entry -> entry.sortOrder }
    }
}
