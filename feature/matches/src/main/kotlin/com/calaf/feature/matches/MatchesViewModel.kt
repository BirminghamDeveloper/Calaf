package com.calaf.feature.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.LikedYou
import com.calaf.core.model.MatchMoment
import com.calaf.core.model.MemberPhotoCache
import com.calaf.core.model.PhotoRepositoryFactory
import com.calaf.core.model.PublicPhoto
import com.calaf.core.model.Match
import com.calaf.core.model.ReportCategory
import com.calaf.core.ui.ActionRunner
import com.calaf.core.ui.ActionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MatchesUiState(
    val matches: List<Match> = emptyList(),
    /** Null until first loaded. */
    val likedYou: LikedYou? = null,
    val loadingLikedYou: Boolean = false,
    val matchMoment: MatchMoment? = null,
    val action: ActionState = ActionState(),
)

@HiltViewModel(assistedFactory = MatchesViewModel.Factory::class)
class MatchesViewModel @AssistedInject constructor(
    @Assisted private val uid: String,
    private val repository: CommunityRepository,
    photos: PhotoRepositoryFactory,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(uid: String): MatchesViewModel
    }

    private val mutable = MutableStateFlow(MatchesUiState())
    val state = mutable.asStateFlow()
    private var observer: Job? = null
    private val memberPhotos = MemberPhotoCache(photos, uid)
    private val actions = ActionRunner(repository, viewModelScope, { mutable.value.action }) { change ->
        mutable.update { it.copy(action = change(it.action)) }
    }

    fun observe() {
        observer?.cancel()
        actions.clearError()
        refreshLikedYou()
        observer = viewModelScope.launch {
            repository.matches(uid).catch { actions.fail(it) }.collect { data ->
                mutable.update { it.copy(matches = data) }
            }
        }
    }

    fun stop() {
        observer?.cancel()
    }

    /** Loads who liked you. Locked in the live app until premium exists, and a simulated list in the demo. */
    fun refreshLikedYou() {
        if (state.value.loadingLikedYou) return
        mutable.update { it.copy(loadingLikedYou = true) }
        viewModelScope.launch {
            try {
                val data = repository.likedYou()
                mutable.update { it.copy(likedYou = data) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                actions.fail(error)
            } finally {
                mutable.update { it.copy(loadingLikedYou = false) }
            }
        }
    }

    /** Likes someone who already liked you. That is a match, so the match moment follows. */
    fun likeBack(profile: CommunityProfile, note: String = "") = respond(profile, like = true, note = note)

    fun pass(profile: CommunityProfile) = respond(profile, like = false, note = "")

    private fun respond(profile: CommunityProfile, like: Boolean, note: String) {
        if (state.value.action.busy) return
        actions.run(
            CommunityCommand.Interest(profile.id, like, if (like) note else ""),
            onResult = { result ->
                val matchId = result.matchId
                if (result.matched && matchId != null) {
                    mutable.update { it.copy(matchMoment = MatchMoment(profile, matchId)) }
                }
            },
            onSuccess = ::refreshLikedYou,
        )
    }

    suspend fun photo(ownerId: String, photo: PublicPhoto): ByteArray? = memberPhotos.photo(ownerId, photo)

    suspend fun matchPhotos(profile: CommunityProfile): Pair<ByteArray?, ByteArray?> =
        memberPhotos.matchPhotos(profile)

    fun dismissMatchMoment() {
        mutable.update { it.copy(matchMoment = null) }
    }

    fun unmatch(matchId: String) = actions.run(CommunityCommand.Unmatch(matchId))

    fun block(userId: String) = actions.run(CommunityCommand.Block(userId))

    fun report(userId: String, category: ReportCategory, details: String, onSuccess: () -> Unit) =
        actions.run(CommunityCommand.Report(userId, category, details), onSuccess = onSuccess)

    fun dismissNotice() = actions.dismissNotice()
}
