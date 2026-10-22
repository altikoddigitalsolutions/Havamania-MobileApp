package com.havamania

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.havamania.ui.theme.ThemeManager
import com.havamania.ui.theme.dataStore
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileSnapshotTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun profileFieldsArePublishedTogetherAndClearingDoesNotTouchAnotherOwner() = runBlocking {
        val uid = "profile-test-${UUID.randomUUID()}"
        val other = "$uid-other"
        try {
            ThemeManager.saveProfileSnapshot(context, UserProfile(uid = uid, name = "Old", bio = "Old bio"))
            ThemeManager.saveProfileSnapshot(context, UserProfile(uid = other, name = "Other"))
            val initialObserved = CompletableDeferred<Unit>()
            val observed = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) {
                    context.dataStore.data.map { prefs ->
                        listOf("user_name", "user_bio", "user_interests").map {
                            prefs[stringPreferencesKey("havamania:$uid:$it")].orEmpty()
                        }
                    }.distinctUntilChanged().onEach { initialObserved.complete(Unit) }.take(2).toList()
                }
            }
            // Wait for the collector's initial snapshot before changing the profile.
            withTimeout(10_000) { initialObserved.await() }
            ThemeManager.saveProfileSnapshot(context, UserProfile(uid = uid, name = "New", bio = "New bio",
                photoURL = "local-test-photo", aboutMe = "Camping",
                personalizationProfile = FirestorePersonalizationProfile(selectedInterests = listOf("kamp"))))
            val snapshots = observed.await()
            assertEquals(listOf("Old", "Old bio", ""), snapshots.first())
            assertEquals(listOf("New", "New bio", "kamp"), snapshots.last())
            ThemeManager.saveProfileSnapshot(context, UserProfile(uid = uid))
            assertEquals("", ThemeManager.getUserName(context, uid).first())
            assertEquals("", ThemeManager.getUserBio(context, uid).first())
            assertEquals("", ThemeManager.getUserAboutMe(context, uid).first())
            assertTrue(ThemeManager.getUserInterests(context, uid).first().isEmpty())
            assertNull(ThemeManager.getUserImageUriByUid(context, uid).first())
            assertEquals("Other", ThemeManager.getUserName(context, other).first())
        } finally {
            context.dataStore.edit { prefs ->
                prefs.asMap().keys.filter { it.name.startsWith("havamania:$uid:") ||
                    it.name.startsWith("havamania:$other:") }.forEach { prefs.remove(it) }
            }
        }
    }
}
