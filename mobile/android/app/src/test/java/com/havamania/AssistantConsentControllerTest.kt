package com.havamania

import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AssistantConsentControllerTest {
    private fun document(accepted: Boolean = false) = AssistantConsentDocument(2, "Local test text", "a".repeat(64),
        AssistantHealthConsent(2, "Optional health test text", "b".repeat(64)), accepted)

    private open class Gateway(var doc: AssistantConsentDocument) : AssistantConsentGateway {
        var saves = 0
        var lastHealth: Boolean? = null
        var failSave = false
        var stale = false
        override suspend fun load(owner: String) = doc
        override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument {
            saves++
            lastHealth = health
            if (stale) throw AssistantConsentStaleException()
            if (failSave) error("offline")
            doc = doc.copy(accepted = true, health_accepted = health)
            return doc
        }
        override suspend fun revoke(owner: String) { doc = doc.copy(accepted = false, health_accepted = false) }
    }

    @Test fun noImplicitApprovalAndSeparateOptionalHealth() = runBlocking {
        val gateway = Gateway(document())
        val controller = AssistantConsentController(this, { "A" }, gateway)
        try {
            controller.load(); yield()
            assertFalse(controller.state.value.allowed)
            assertTrue(controller.state.value.visible)
            assertFalse(controller.state.value.document!!.health_accepted)
            controller.save(false); controller.save(true); yield()
            assertEquals(1, gateway.saves)
            assertEquals(false, gateway.lastHealth)
            assertTrue(controller.state.value.allowed)
            assertFalse(controller.state.value.visible)
            controller.open(); yield()
            controller.save(true); yield()
            assertTrue(controller.state.value.document!!.health_accepted)
            controller.revoke(); yield()
            assertFalse(controller.state.value.allowed)
            assertFalse(controller.state.value.document!!.health_accepted)
        } finally { controller.close() }
    }

    @Test fun oldAccountResponseCannotGrantConsentToNewAccountEvenIfItIgnoresCancellation() = runBlocking {
        var owner = "A"
        val late = CompletableDeferred<AssistantConsentDocument>()
        val gateway = object : Gateway(document()) {
            override suspend fun load(owner: String): AssistantConsentDocument =
                if (owner == "A") withContext(NonCancellable) { late.await() } else doc
        }
        val controller = AssistantConsentController(this, { owner }, gateway)
        try {
            controller.load(); yield()
            owner = "B"; controller.reset(); controller.load(); yield()
            assertFalse(controller.state.value.allowed)
            late.complete(document(accepted = true)); yield(); yield()
            assertFalse(controller.state.value.allowed)
            assertEquals("Local test text", controller.state.value.document!!.text)
            controller.reset()
            assertNull(controller.state.value.document)
        } finally { late.complete(document()); controller.close() }
    }

    @Test fun changedTextRequiresAnotherExplicitDecisionAndClearsHealthChoice() = runBlocking {
        val gateway = Gateway(document())
        val controller = AssistantConsentController(this, { "A" }, gateway)
        try {
            controller.load(); yield()
            gateway.doc = document().copy(version = 3, text_hash = "c".repeat(64))
            gateway.stale = true
            controller.save(true); yield()
            assertFalse(controller.state.value.allowed)
            assertTrue(controller.state.value.visible)
            assertNotNull(controller.state.value.error)
            assertEquals(3, controller.state.value.document!!.version)
            assertFalse(controller.state.value.document!!.health_accepted)
        } finally { controller.close() }
    }

    @Test fun failedSaveAndMalformedDocumentNeverLookApproved() = runBlocking {
        val gateway = Gateway(document())
        val controller = AssistantConsentController(this, { "A" }, gateway)
        try {
            controller.load(); yield()
            gateway.failSave = true
            controller.save(false); yield()
            assertFalse(controller.state.value.allowed)
            assertNotNull(controller.state.value.error)
            assertFalse(controller.state.value.busy)
            gateway.doc = document(true).copy(text_hash = "missing")
            controller.open(); yield()
            assertNull(controller.state.value.document)
            assertFalse(controller.state.value.allowed)
        } finally { controller.close() }
    }

    @Test fun protocolRetainsDisplayedTextHashesAndDefaultsToNoHealthApproval() {
        val doc = document()
        val update = AssistantConsentUpdate(true, doc.version, doc.text_hash)
        val restored = Json.decodeFromString<AssistantConsentUpdate>(Json.encodeToString(update))
        assertEquals(doc.text_hash, restored.consent_hash)
        assertFalse(restored.health_accepted)
        assertNull(restored.health_consent_hash)
        assertEquals(AssistantResult.ConsentRequired, assistantHttpFailure(428, "not-json"))
        assertTrue(isLocalAssistantPrompt("İYİ GÜNLER"))
    }
    @Test fun changedHealthTextIsShownAgainWithoutAssumingHealthPermission() = runBlocking {
        val gateway = Gateway(document(true).copy(health_review_required = true))
        val controller = AssistantConsentController(this, { "A" }, gateway)
        try {
            controller.load(); yield()
            assertTrue(controller.state.value.visible)
            assertFalse(controller.state.value.document!!.health_accepted)
            controller.dismiss()
            assertTrue(controller.state.value.allowed)
            assertFalse(controller.state.value.document!!.health_accepted)
        } finally { controller.close() }
    }
    @Test fun timeoutClearsBusyStateAndMakesDismissPossible() = runBlocking {
        val gateway = object : Gateway(document()) {
            override suspend fun load(owner: String): AssistantConsentDocument = awaitCancellation()
        }
        val controller = AssistantConsentController(this, { "A" }, gateway, timeoutMillis = 5)
        try {
            controller.open()
            withTimeout(1000) { while (controller.state.value.busy) delay(1) }
            assertNotNull(controller.state.value.error)
            assertFalse(controller.state.value.allowed)
            controller.dismiss()
            assertFalse(controller.state.value.visible)
        } finally { controller.close() }
    }
}
