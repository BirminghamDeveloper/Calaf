package com.calaf.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.MatchMoment
import com.calaf.core.model.MemberPhotoCache
import com.calaf.core.model.PartnerPreference
import com.calaf.core.model.PhotoRepositoryFactory
import com.calaf.core.model.PublicPhoto
import com.calaf.core.model.ReportCategory
import com.calaf.core.ui.ActionRunner
import com.calaf.core.ui.ActionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    /** The member's own published profile. Null until they publish, which discovery needs. */
    val profile: CommunityProfile? = null,
    /** The member's saved filters, which the filter sheet edits. */
    val preferences: PartnerPreference = PartnerPreference(),
    val candidates: List<CommunityProfile> = emptyList(),
    val cursor: String? = null,
    val discovering: Boolean = false,
    val loaded: Boolean = false,
    /** Set when a like becomes a match. Shown full screen until dismissMatchMoment() is called. */
    val matchMoment: MatchMoment? = null,
    val action: ActionState = ActionState(),
)

@HiltViewModel(assistedFactory = DiscoveryViewModel.Factory::class)
class DiscoveryViewModel @AssistedInject constructor(
    @Assisted private val uid: String,
    private val repository: CommunityRepository,
    private val photos: PhotoRepositoryFactory,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(uid: String): DiscoveryViewModel
    }

    private val mutable = MutableStateFlow(DiscoveryUiState())
    val state = mutable.asStateFlow()
    private var observers = listOf<Job>()
    private val memberPhotos = MemberPhotoCache(photos, uid)
    private val actions = ActionRunner(repository, viewModelScope, { mutable.value.action }) { change ->
        mutable.update { it.copy(action = change(it.action)) }
    }

    fun observe() {
        observers.forEach(Job::cancel)
        actions.clearError()
        observers = listOf(
            viewModelScope.launch {
                repository.profile(uid).catch { actions.fail(it) }.collect { data ->
                    mutable.update { it.copy(profile = data) }
                }
            },
            viewModelScope.launch {
                repository.preferences(uid).catch { actions.fail(it) }.collect { data ->
                    mutable.update { it.copy(preferences = data) }
                }
            },
        )
    }

    fun stop() {
        observers.forEach(Job::cancel)
    }

    /** Saves the filters, then restarts discovery so the stack reflects them. [onSaved] runs once they are saved. */
    fun applyFilters(value: PartnerPreference, onSaved: () -> Unit = {}) = actions.run(
        CommunityCommand.Preferences(value),
        onSuccess = {
            onSaved()
            reload()
        },
    )

    private fun reload() {
        mutable.update { it.copy(candidates = emptyList(), cursor = null, loaded = false) }
        discover()
    }

    /** Loads the first page, or the next one when [more] is true. */
    fun discover(more: Boolean = false) {
        if (state.value.discovering) return
        mutable.update { it.copy(discovering = true, action = it.action.copy(error = null)) }
        viewModelScope.launch {
            try {
                val page = repository.discover(if (more) state.value.cursor else null)
                mutable.update {
                    it.copy(
                        candidates = if (more) {
                            (it.candidates + page.profiles).distinctBy(CommunityProfile::id)
                        } else {
                            page.profiles
                        },
                        cursor = page.cursor,
                        loaded = true,
                    )
                }
            } catch (error: Exception) {
                actions.fail(error)
            } finally {
                mutable.update { it.copy(discovering = false) }
            }
        }
    }

    /** Loads the first page the first time the screen opens. Does nothing once a page has loaded. */
    fun discoverIfNeeded() {
        val current = state.value
        if (!current.loaded && !current.discovering) discover()
    }

    /**
     * Likes or passes. The card leaves the stack at once so the next one shows straight away, and comes back to the
     * front if the request fails. When only a few cards are left the next page is fetched.
     */
    fun interest(userId: String, like: Boolean, note: String = "") {
        if (state.value.action.busy) return
        val index = state.value.candidates.indexOfFirst { it.id == userId }
        val removed = state.value.candidates.getOrNull(index)
        removeCandidate(userId)
        actions.run(
            CommunityCommand.Interest(userId, like, if (like) note else ""),
            onResult = { result ->
                val matchId = result.matchId
                if (result.matched && matchId != null && removed != null) {
                    mutable.update { it.copy(matchMoment = MatchMoment(removed, matchId)) }
                }
            },
            onSuccess = ::prefetchIfNeeded,
            onFailure = { if (removed != null) restoreCandidate(removed, index) },
        )
    }

    /** Fetches the next page while a few cards are still left, so the stack never runs dry mid swipe. */
    private fun prefetchIfNeeded() {
        val current = state.value
        if (DiscoveryPaging.shouldPrefetch(current.candidates.size, current.cursor, current.discovering)) {
            discover(more = true)
        }
    }

    /** The bytes of another member's photo, or null if it cannot be read. Blurring is applied by the screen. */
    suspend fun photo(ownerId: String, photo: PublicPhoto): ByteArray? = memberPhotos.photo(ownerId, photo)

    /** Both main photos for the match moment, shown clear because matched members see each other. */
    suspend fun matchPhotos(profile: CommunityProfile): Pair<ByteArray?, ByteArray?> =
        memberPhotos.matchPhotos(profile)

    fun dismissMatchMoment() {
        mutable.update { it.copy(matchMoment = null) }
    }

    fun block(userId: String) = actions.run(CommunityCommand.Block(userId), onResult = { removeCandidate(userId) })

    fun report(userId: String, category: ReportCategory, details: String, onSuccess: () -> Unit) =
        actions.run(CommunityCommand.Report(userId, category, details), onSuccess = onSuccess)

    fun retry() {
        observe()
        discover()
    }

    fun dismissNotice() = actions.dismissNotice()

    private fun restoreCandidate(profile: CommunityProfile, index: Int) {
        mutable.update { state ->
            if (state.candidates.any { it.id == profile.id }) {
                state
            } else {
                val restored = state.candidates.toMutableList()
                restored.add(index.coerceIn(0, restored.size), profile)
                state.copy(candidates = restored)
            }
        }
    }

    private fun removeCandidate(userId: String) {
        mutable.update { state -> state.copy(candidates = state.candidates.filterNot { it.id == userId }) }
    }
}
