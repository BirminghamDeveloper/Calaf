package com.calaf.feature.onboarding

import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityError
import com.calaf.core.model.ProfileDraft
import com.calaf.core.testing.FakeCommunityRepository
import com.calaf.core.testing.MainDispatcherRule
import com.calaf.core.ui.Notice
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PublishViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = FakeCommunityRepository()
    private val draft = ProfileDraft("Amina", "1995-02-03", "London", "UK")

    @Test fun `publishing sends the draft and confirms`() = runTest(main.dispatcher) {
        val model = PublishViewModel(repository)
        model.publish(draft); runCurrent()
        assertEquals(listOf<CommunityCommand>(CommunityCommand.Publish(draft)), repository.commands)
        assertEquals(Notice.SAVED, model.state.value.action.notice)
    }

    @Test fun `a failed publish shows the error`() = runTest(main.dispatcher) {
        val model = PublishViewModel(repository)
        repository.failWith = CommunityError.INVALID
        model.publish(draft); runCurrent()
        assertEquals(CommunityError.INVALID, model.state.value.action.error)
        assertEquals(false, model.state.value.action.busy)
    }
}
