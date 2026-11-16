package com.havamania

import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class CitySyncRecoveryActionTest {
    @Test fun transportFailureOffersRetry() {
        assertTrue(canManuallyRetryCitySync(IOException("offline")))
        assertTrue(canManuallyRetryCitySync(CitySyncTimeoutException()))
    }
    @Test fun identityErrorsNeverOfferBlindRetryEvenWhenWrapped() {
        for (cause in listOf(DefaultCityNameAmbiguousException(), CityIdentityConflictException())) {
            assertFalse(canManuallyRetryCitySync(cause))
            assertFalse(canManuallyRetryCitySync(IOException("wrapper", cause)))
        }
    }
    @Test fun blockedUnknownOrWaitingOperationsNeedRecoveryBeforeRetry() {
        assertFalse(canManuallyRetryCitySync(IllegalStateException()))
        assertFalse(canManuallyRetryCitySync(CitySyncWaitingException(null, Long.MAX_VALUE)))
        assertFalse(canManuallyRetryCitySync(CitySyncWaitingException("AUTHENTICATION", 0)))
        assertFalse(canManuallyRetryCitySync(CitySyncWaitingException("REJECTED", 0)))
    }
    @Test fun wrappedAmbiguityExplainsReselectionAndPreservation() {
        val text = citySyncErrorText(IOException("wrapper", DefaultCityNameAmbiguousException()))
        assertTrue(text.contains("korundu"))
        assertTrue(text.contains("yeniden varsayılan seçin"))
    }
}
