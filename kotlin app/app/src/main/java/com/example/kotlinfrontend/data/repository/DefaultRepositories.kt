package com.example.kotlinfrontend.data.repository

import com.example.kotlinfrontend.data.model.BookmarkRow
import com.example.kotlinfrontend.data.model.ComplaintRecord
import com.example.kotlinfrontend.data.model.DictionaryEntry
import com.example.kotlinfrontend.data.model.ProfileRecord
import com.example.kotlinfrontend.data.model.SessionState
import com.example.kotlinfrontend.data.model.SessionUser
import com.example.kotlinfrontend.data.model.TranslationHistoryItem
import com.example.kotlinfrontend.data.local.AppLocalStore
import com.example.kotlinfrontend.data.local.SeedDictionaryDataSource
import com.example.kotlinfrontend.data.remote.SupabaseService
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun nowIso(): String = Instant.now().toString()

class DefaultAuthRepository(
    private val appScope: CoroutineScope,
    private val supabaseService: SupabaseService
) : AuthRepository {
    private val _sessionState = MutableStateFlow(SessionState.initializing(supabaseService.isConfigured))
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        if (!supabaseService.isConfigured) {
            _sessionState.value = SessionState.guest(
                isConfigured = false,
                errorMessage = "Supabase is not configured. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY."
            )
        } else {
            observeSession()
        }
    }

    override suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> = runCatching {
        val client = requireClient()
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName)
            }
        }
        if (client.auth.currentUserOrNull() == null) {
            runCatching {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.onFailure { error ->
                val normalized = error.message.orEmpty().lowercase()
                if (!normalized.contains("email") || !normalized.contains("confirm")) {
                    throw error
                }
            }
        }
        Unit
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        val client = requireClient()
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        requireClient().auth.resetPasswordForEmail(email = email)
        Unit
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        requireClient().auth.signOut()
        Unit
    }

    private fun observeSession() {
        val client = requireClient()
        appScope.launch(Dispatchers.IO) {
            client.auth.sessionStatus.collect { status ->
                when (status) {
                    SessionStatus.Initializing -> {
                        _sessionState.value = SessionState.initializing(isConfigured = true)
                    }

                    is SessionStatus.Authenticated -> {
                        val user = client.auth.currentUserOrNull()
                        val fullNameFromMetadata = user?.userMetadata
                            ?.get("full_name")
                            ?.toString()
                            ?.trim('"')
                        val profile = try {
                            client.from("profiles")
                                .select()
                                .decodeList<ProfileRecord>()
                                .firstOrNull()
                        } catch (_: Exception) {
                            null
                        }

                        _sessionState.value = SessionState(
                            isInitialized = true,
                            isConfigured = true,
                            isAuthenticated = user != null,
                            user = user?.let {
                                SessionUser(
                                    id = it.id,
                                    email = it.email,
                                    fullName = profile?.fullName ?: fullNameFromMetadata,
                                    role = profile?.role ?: "user"
                                )
                            }
                        )
                    }

                    is SessionStatus.NotAuthenticated -> {
                        _sessionState.value = SessionState.guest(
                            isConfigured = true,
                            errorMessage = null
                        )
                    }

                    is SessionStatus.RefreshFailure -> {
                        _sessionState.value = SessionState.guest(
                            isConfigured = true,
                            errorMessage = "Session refresh failed."
                        )
                    }
                }
            }
        }
    }

    private fun requireClient() = supabaseService.clientOrNull
        ?: error("Supabase is not configured.")
}

class DefaultDictionaryRepository(
    private val seedDataSource: SeedDictionaryDataSource,
    private val supabaseService: SupabaseService,
    private val bookmarkRepository: BookmarkRepository
) : DictionaryRepository {
    @Volatile
    private var cachedEntries: List<DictionaryEntry>? = null

    override suspend fun search(
        query: String,
        category: String?,
        limit: Int,
        offset: Int
    ): List<DictionaryEntry> {
        val normalizedQuery = query.trim()
        val normalizedCategory = category?.takeUnless { value -> value.equals("All", ignoreCase = true) }
        return currentEntries()
            .filter { entry ->
                (normalizedCategory == null || entry.category == normalizedCategory) &&
                    (
                        normalizedQuery.isBlank() ||
                            entry.englishWord.contains(normalizedQuery, ignoreCase = true) ||
                            entry.urduWord.contains(normalizedQuery, ignoreCase = true) ||
                            entry.slug.replace('_', ' ').contains(normalizedQuery, ignoreCase = true)
                        )
            }
            .drop(offset)
            .take(limit)
    }

    override suspend fun getEntry(slug: String): DictionaryEntry? {
        return currentEntries().firstOrNull { entry -> entry.slug == slug }
    }

    override suspend fun getEntryById(id: String): DictionaryEntry? {
        return currentEntries().firstOrNull { entry -> entry.id == id }
    }

    override suspend fun getCategories(): List<String> {
        return buildList {
            add("All")
            addAll(
                currentEntries()
                    .map { entry -> entry.category }
                    .distinct()
                    .sorted()
            )
        }
    }

    override suspend fun getBookmarkedEntries(userId: String): List<DictionaryEntry> {
        val bookmarkedIds = bookmarkRepository.getBookmarkedIds(userId)
        if (bookmarkedIds.isEmpty()) {
            return emptyList()
        }
        return currentEntries().filter { entry -> entry.id in bookmarkedIds }
    }

    private suspend fun currentEntries(): List<DictionaryEntry> {
        cachedEntries?.let { return it }

        val seedEntries = seedDataSource.load()
        cachedEntries = seedEntries

        if (!supabaseService.isConfigured) {
            return seedEntries
        }

        val remoteEntries = try {
            supabaseService.clientOrNull
                ?.from("dictionary_entries")
                ?.select()
                ?.decodeList<DictionaryEntry>()
                ?.filter { entry -> entry.isActive }
                ?.sortedBy { entry -> entry.sortOrder }
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        if (remoteEntries.isNotEmpty()) {
            cachedEntries = remoteEntries
        }

        return cachedEntries.orEmpty()
    }
}

class DefaultBookmarkRepository(
    private val authRepository: AuthRepository,
    private val supabaseService: SupabaseService
) : BookmarkRepository {
    override suspend fun getBookmarkedIds(userId: String): Set<String> {
        if (!supabaseService.isConfigured || !isSameAuthenticatedUser(userId)) {
            return emptySet()
        }

        return try {
            supabaseService.clientOrNull
                ?.from("user_bookmarks")
                ?.select()
                ?.decodeList<BookmarkRow>()
                ?.map { row -> row.dictionaryEntryId }
                ?.toSet()
                .orEmpty()
        } catch (_: Exception) {
            emptySet()
        }
    }

    override suspend fun toggleBookmark(userId: String, entryId: String): Result<Boolean> = runCatching {
        require(isSameAuthenticatedUser(userId)) { "Please sign in to bookmark words." }
        val client = supabaseService.clientOrNull ?: error("Supabase is not configured.")
        val existingIds = getBookmarkedIds(userId)
        if (entryId in existingIds) {
            client.from("user_bookmarks").delete {
                filter {
                    eq("dictionary_entry_id", entryId)
                }
            }
            false
        } else {
            client.from("user_bookmarks").insert(
                BookmarkRow(
                    userId = userId,
                    dictionaryEntryId = entryId,
                    createdAt = nowIso()
                )
            )
            true
        }
    }

    private fun isSameAuthenticatedUser(userId: String): Boolean {
        return authRepository.sessionState.value.user?.id == userId
    }
}

class DefaultHistoryRepository(
    private val authRepository: AuthRepository,
    private val localStore: AppLocalStore,
    private val supabaseService: SupabaseService
) : HistoryRepository {
    override suspend fun savePrediction(
        predictedWord: String,
        confidence: Double,
        modelVersion: String,
        createdAt: String
    ): TranslationHistoryItem {
        val currentUser = authRepository.sessionState.value.user
        val historyItem = TranslationHistoryItem(
            id = UUID.randomUUID().toString(),
            userId = currentUser?.id,
            predictedWordSlug = predictedWord,
            confidence = confidence,
            modelVersion = modelVersion,
            createdAt = createdAt
        )

        localStore.appendGuestHistory(historyItem)

        if (currentUser != null && supabaseService.isConfigured) {
            runCatching {
                supabaseService.clientOrNull
                    ?.from("translation_history")
                    ?.insert(historyItem)
            }
        }

        return historyItem
    }

    override suspend fun listRecent(limit: Int, offset: Int): List<TranslationHistoryItem> {
        val currentUser = authRepository.sessionState.value.user
        if (currentUser != null && supabaseService.isConfigured) {
            val remoteHistory = try {
                supabaseService.clientOrNull
                    ?.from("translation_history")
                    ?.select()
                    ?.decodeList<TranslationHistoryItem>()
                    ?.sortedByDescending { item -> item.createdAt }
                    ?.drop(offset)
                    ?.take(limit)
                    .orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            if (remoteHistory.isNotEmpty()) {
                return remoteHistory
            }
        }

        return localStore.readGuestHistory()
            .sortedByDescending { item -> item.createdAt }
            .drop(offset)
            .take(limit)
    }
}

class DefaultComplaintRepository(
    private val authRepository: AuthRepository,
    private val localStore: AppLocalStore,
    private val dictionaryRepository: DictionaryRepository,
    private val supabaseService: SupabaseService
) : ComplaintRepository {
    override suspend fun submitFromPrediction(
        historyId: String,
        expectedWord: String,
        note: String
    ): Result<Unit> = runCatching {
        val user = requireUser()
        val historyItem = localStore.findHistoryById(historyId)
        val now = nowIso()
        val complaint = ComplaintRecord(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            sourceType = "prediction",
            translationHistoryId = historyId,
            reportedWordSlug = historyItem?.predictedWordSlug,
            expectedWord = expectedWord.ifBlank { null },
            note = note.ifBlank { null },
            status = "open",
            createdAt = now,
            updatedAt = now
        )

        requireConfiguredClient().from("complaints").insert(complaint)
        Unit
    }

    override suspend fun submitFromDictionary(entryId: String, note: String): Result<Unit> = runCatching {
        val user = requireUser()
        val entry = dictionaryRepository.getEntryById(entryId)
        val now = nowIso()
        val complaint = ComplaintRecord(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            sourceType = "dictionary",
            dictionaryEntryId = entryId,
            reportedWordSlug = entry?.slug,
            note = note.ifBlank { null },
            status = "open",
            createdAt = now,
            updatedAt = now
        )

        requireConfiguredClient().from("complaints").insert(complaint)
        Unit
    }

    override suspend fun listMine(limit: Int, offset: Int): List<ComplaintRecord> {
        if (authRepository.sessionState.value.user == null || !supabaseService.isConfigured) {
            return emptyList()
        }

        return try {
            requireConfiguredClient()
                .from("complaints")
                .select()
                .decodeList<ComplaintRecord>()
                .sortedByDescending { complaint -> complaint.createdAt }
                .drop(offset)
                .take(limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun requireUser(): SessionUser {
        return authRepository.sessionState.value.user
            ?: error("Please sign in to submit a report.")
    }

    private fun requireConfiguredClient() = supabaseService.clientOrNull
        ?: error("Supabase is not configured.")
}
