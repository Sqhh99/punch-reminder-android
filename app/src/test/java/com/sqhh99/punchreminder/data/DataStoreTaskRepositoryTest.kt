package com.sqhh99.punchreminder.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.sqhh99.punchreminder.data.repository.DataStoreTaskRepository
import com.sqhh99.punchreminder.domain.model.PunchTask
import com.sqhh99.punchreminder.domain.model.TaskSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreTaskRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After
    fun tearDown() = scope.cancel()

    private fun newRepository(): DataStoreTaskRepository {
        val store = PreferenceDataStoreFactory.create(scope = scope) {
            File(tmp.root, "tasks_${System.nanoTime()}.preferences_pb")
        }
        return DataStoreTaskRepository(store)
    }

    private fun sampleTask(id: String = "t1", name: String = "上班打卡") =
        PunchTask(id = id, name = name, hour = 8, minute = 50, schedule = TaskSchedule.Weekdays)

    @Test
    fun upsert_thenRead_persists() = runBlocking {
        val repo = newRepository()
        repo.upsert(sampleTask())
        assertEquals(listOf(sampleTask()), repo.getAll())
    }

    @Test
    fun upsert_existingId_updatesInPlace() = runBlocking {
        val repo = newRepository()
        repo.upsert(sampleTask())
        repo.upsert(sampleTask(name = "改名"))
        assertEquals(1, repo.getAll().size)
        assertEquals("改名", repo.getById("t1")?.name)
    }

    @Test
    fun setEnabled_togglesPersisted() = runBlocking {
        val repo = newRepository()
        repo.upsert(sampleTask())
        repo.setEnabled("t1", false)
        assertFalse(repo.getById("t1")!!.enabled)
    }

    @Test
    fun delete_removesTask() = runBlocking {
        val repo = newRepository()
        repo.upsert(sampleTask())
        repo.delete("t1")
        assertTrue(repo.getAll().isEmpty())
        assertNull(repo.getById("t1"))
    }
}
