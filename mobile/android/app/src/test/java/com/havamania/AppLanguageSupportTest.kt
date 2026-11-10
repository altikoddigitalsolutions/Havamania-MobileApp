package com.havamania

import com.havamania.ui.theme.AppLanguageSupport
import org.junit.Assert.*
import org.junit.Test

class AppLanguageSupportTest {
    @Test fun legacyUnsupportedPreferencesCannotAdvertiseUnavailableLanguage() {
        for (saved in listOf("EN", "en", "unknown", "")) {
            assertEquals("TR", AppLanguageSupport.resolve(saved))
            assertEquals("Türkçe", AppLanguageSupport.displayName(saved))
            assertFalse(AppLanguageSupport.isSupported(saved))
        }
    }

    @Test fun supportedPreferenceIsNormalizedBeforeStorageAndDisplay() {
        for (saved in listOf("TR", "tr", " TR ")) {
            assertTrue(AppLanguageSupport.isSupported(saved))
            assertEquals("TR", AppLanguageSupport.resolve(saved))
            assertEquals("Türkçe", AppLanguageSupport.displayName(saved))
        }
    }
}
