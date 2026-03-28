package com.example.kotlinfrontend.app

import android.content.Context
import com.example.kotlinfrontend.data.local.AppLocalStore
import com.example.kotlinfrontend.data.local.SeedDictionaryDataSource
import com.example.kotlinfrontend.data.remote.SupabaseService
import com.example.kotlinfrontend.data.repository.AuthRepository
import com.example.kotlinfrontend.data.repository.BookmarkRepository
import com.example.kotlinfrontend.data.repository.ComplaintRepository
import com.example.kotlinfrontend.data.repository.DefaultAuthRepository
import com.example.kotlinfrontend.data.repository.DefaultBookmarkRepository
import com.example.kotlinfrontend.data.repository.DefaultComplaintRepository
import com.example.kotlinfrontend.data.repository.DefaultDictionaryRepository
import com.example.kotlinfrontend.data.repository.DefaultHistoryRepository
import com.example.kotlinfrontend.data.repository.DictionaryRepository
import com.example.kotlinfrontend.data.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabaseService = SupabaseService()
    val localStore = AppLocalStore(context)
    private val seedDictionaryDataSource = SeedDictionaryDataSource(context)

    val authRepository: AuthRepository = DefaultAuthRepository(
        appScope = appScope,
        supabaseService = supabaseService
    )

    val bookmarkRepository: BookmarkRepository = DefaultBookmarkRepository(
        authRepository = authRepository,
        supabaseService = supabaseService
    )

    val dictionaryRepository: DictionaryRepository = DefaultDictionaryRepository(
        seedDataSource = seedDictionaryDataSource,
        supabaseService = supabaseService,
        bookmarkRepository = bookmarkRepository
    )

    val historyRepository: HistoryRepository = DefaultHistoryRepository(
        authRepository = authRepository,
        localStore = localStore,
        supabaseService = supabaseService
    )

    val complaintRepository: ComplaintRepository = DefaultComplaintRepository(
        authRepository = authRepository,
        localStore = localStore,
        dictionaryRepository = dictionaryRepository,
        supabaseService = supabaseService
    )
}
