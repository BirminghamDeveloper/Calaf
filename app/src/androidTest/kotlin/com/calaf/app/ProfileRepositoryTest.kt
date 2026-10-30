package com.calaf.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.calaf.app.data.LocalProfileRepository
import com.calaf.core.model.ProfileDraft
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ProfileRepositoryTest {
    @Test fun draftsPersistAcrossRepositoryRestartAndRemainAccountScoped() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "profile-test-${UUID.randomUUID()}.preferences_pb")
        var job = SupervisorJob()
        try {
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
            val repository = LocalProfileRepository(store)
            val draft = ProfileDraft("Amina", "1995-02-03", "London", "UK", interests = "Reading,Walking", values = "Family",
                gender = com.calaf.core.model.Gender.WOMAN, wantsChildren = com.calaf.core.model.Answer.YES)
            repository.save("account-a", draft)
            assertEquals(ProfileDraft(), repository.load("account-b"))
            job.cancelAndJoin()
            job = SupervisorJob()
            val restored = LocalProfileRepository(PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file })
            assertEquals(draft, restored.load("account-a"))
            assertEquals(ProfileDraft(), restored.load("account-b"))
        } finally {
            job.cancelAndJoin()
            file.delete()
        }
    }
}
