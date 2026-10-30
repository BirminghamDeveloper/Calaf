package com.calaf.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val draft: ProfileDraft = ProfileDraft(),
    val issue: ProfileIssue? = null,
    val storageError: Boolean = false,
    val reviewing: Boolean = false,
    val saved: Boolean = false,
)

class ProfileViewModel(private val userId: String, private val repository: ProfileRepository) : ViewModel() {
    private val mutable = MutableStateFlow(ProfileUiState())
    val state = mutable.asStateFlow()
    private var autosave: Job? = null
    init { load() }
    fun load() {
        mutable.update { it.copy(loading = true, storageError = false) }
        viewModelScope.launch {
            try {
                val draft = repository.load(userId)
                mutable.value = ProfileUiState(loading = false, draft = draft,
                    reviewing = AccountValidation.profile(draft) == null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.update { it.copy(loading = false, storageError = true) } }
        }
    }
    fun edit(draft: ProfileDraft) {
        if (state.value.loading || state.value.saving) return
        mutable.update { it.copy(draft = draft, issue = null, saved = false) }
        autosave?.cancel()
        autosave = viewModelScope.launch {
            delay(350)
            persist(draft, review = false)
        }
    }
    fun editAgain() { mutable.update { it.copy(reviewing = false, saved = false) } }
    fun save() {
        if (state.value.saving || state.value.loading) return
        val draft = state.value.draft
        val issue = AccountValidation.profile(draft)
        mutable.update { it.copy(issue = issue) }
        if (issue != null) return
        autosave?.cancel()
        mutable.update { it.copy(saving = true) }
        viewModelScope.launch { persist(draft, review = true) }
    }
    private suspend fun persist(draft: ProfileDraft, review: Boolean) {
        try {
            repository.save(userId, draft)
            mutable.update { it.copy(storageError = false, saved = true, reviewing = review, saving = false) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutable.update { it.copy(storageError = true, saving = false, saved = false) } }
    }
}
