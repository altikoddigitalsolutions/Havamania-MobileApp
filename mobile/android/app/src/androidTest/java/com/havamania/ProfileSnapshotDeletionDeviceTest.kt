package com.havamania

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class ProfileSnapshotDeletionDeviceTest {
    @Test fun lateProfileSnapshotCannotRepopulateCleanedUid() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "profile-delete-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var allowed = true
        try {
            val data = PreferenceDataStoreFactory.create(scope = scope) { file }
            val name = stringPreferencesKey("havamania:a:name")
            val bio = stringPreferencesKey("havamania:a:bio")
            val other = stringPreferencesKey("havamania:b:name")
            saveProfilePreferences(data, { allowed }) { it[name] = "A"; it[bio] = "Bio"; it[other] = "B" }
            allowed = false
            data.edit { prefs -> prefs.asMap().keys.filter { it.name.startsWith("havamania:a:") }.forEach { prefs.remove(it) } }
            var transformed = false
            saveProfilePreferences(data, { allowed }) { transformed = true; it[name] = "late"; it[bio] = "late" }
            assertFalse(transformed)
            val prefs = data.data.first()
            assertNull(prefs[name]); assertNull(prefs[bio]); assertEquals("B", prefs[other])
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin(); file.delete() }
    }
}
