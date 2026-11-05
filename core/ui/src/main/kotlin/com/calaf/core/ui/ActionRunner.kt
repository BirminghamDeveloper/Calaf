package com.calaf.core.ui

import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityException
import com.calaf.core.model.CommunityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Whether an action is running, and the last error or confirmation. Part of every screen's UI state. */
data class ActionState(
    val busy: Boolean = false,
    val error: CommunityError? = null,
    val notice: Notice? = null,
)

/**
 * Runs one [CommunityCommand] at a time for a ViewModel and keeps its [ActionState] up to date.
 * The ViewModel passes how to [read] and [write] its own state, so the runner never holds state itself.
 */
class ActionRunner(
    private val repository: CommunityRepository,
    private val scope: CoroutineScope,
    private val read: () -> ActionState,
    private val write: ((ActionState) -> ActionState) -> Unit,
) {
    /**
     * Ignored while another action is running. [onResult] and [onSuccess] run only if the command succeeded and
     * [onFailure] only if it failed, which lets a screen undo an optimistic change.
     */
    fun run(
        command: CommunityCommand,
        onResult: (CommandResult) -> Unit = {},
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {},
    ) {
        if (read().busy) return
        write { it.copy(busy = true, error = null, notice = null) }
        scope.launch {
            try {
                val result = repository.execute(command)
                write { it.copy(notice = noticeFor(command, result)) }
                onResult(result)
                onSuccess()
            } catch (error: Exception) {
                fail(error)
                onFailure()
            } finally {
                write { it.copy(busy = false) }
            }
        }
    }

    fun fail(error: Throwable) {
        if (error is CancellationException) throw error
        write { it.copy(error = (error as? CommunityException)?.reason ?: CommunityError.UNKNOWN) }
    }

    fun clearError() = write { it.copy(error = null) }

    fun dismissNotice() = write { it.copy(notice = null) }
}
