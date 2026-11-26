package com.havamania

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AssistantPhotoHttpTest {
    @Test fun structuredCodesControlConsentAndAvailability() {
        assertTrue(assistantPhotoHttpFailure(428, null, null) is AssistantConsentRequiredException)
        assertTrue(assistantPhotoHttpFailure(409, """{"detail":{"error":"photos_disabled"}}""", null) is PhotosDisabledException)
        assertTrue(assistantPhotoHttpFailure(409, """{"message":"photos_disabled"}""", null) is TerminalAssistantException)
        val busy = assistantPhotoHttpFailure(503, """{"error":"busy"}""", "12") as RateLimitedAssistantException
        assertEquals(12L, busy.seconds)
        assertFalse(assistantPhotoHttpFailure(503, """{"message":"busy"}""", null) is RateLimitedAssistantException)
        assertEquals(null, (assistantPhotoHttpFailure(429, null, "999999999") as RateLimitedAssistantException).seconds)
    }

    @Test fun upstreamDeploymentErrorsDoNotRequestFirebaseLoginOrBlameDeviceConnection() {
        for ((code, diagnosis) in listOf("bridge_auth_failed" to "bağlantı ayarları",
            "upload_endpoint_unavailable" to "sunucuda kullanılamıyor", "not_configured" to "henüz hazır değil")) {
            val failure = assistantPhotoHttpFailure(503, """{"error":"$code","message":"PRIVATE_UPSTREAM"}""", null)
            assertTrue(failure is PhotoUploadFailure)
            assertTrue("Missing specific diagnosis for $code", failure.message!!.contains(diagnosis))
            assertFalse(failure.message!!.contains("PRIVATE_UPSTREAM"))
            assertFalse(failure.message!!.contains("giriş"))
            assertFalse(failure.message!!.contains("Bağlantınızı kontrol"))
        }
    }

    @Test fun serverFailuresNeverEchoBodiesOrBlameDeviceInternet() {
        for (status in listOf(404, 405, 500, 501, 502, 503, 504)) {
            val message = assistantPhotoHttpFailure(status, "<html>private-server-secret</html>", null).message.orEmpty()
            assertFalse(message.contains("private-server-secret"))
            assertFalse(message.contains("Bağlantınızı kontrol"))
            assertTrue(message.isNotBlank())
        }
        assertTrue(assistantPhotoHttpFailure(503, """{"error":"not_configured"}""", null).message!!.contains("henüz hazır değil"))
        assertTrue(assistantPhotoHttpFailure(503, """{"error":"consent_sync_required"}""", null).message!!.contains("izin metni"))
        for (status in listOf(400, 401, 403, 409, 413, 415, 422))
            assertTrue(assistantPhotoHttpFailure(status, null, null) is TerminalAssistantException)
    }

    @Test fun receiptsRequireCompleteUuidAndAllowedMetadata() {
        val id = "A1234567-1234-1234-1234-123456789ABC"
        assertEquals(id.lowercase(), assistantPhotoReceiptId(AssistantAttachmentReceipt(id, "image/jpeg", 20)))
        for (receipt in listOf(null, AssistantAttachmentReceipt("1-1-1-1-1", "image/jpeg", 20),
            AssistantAttachmentReceipt(id, "text/html", 20), AssistantAttachmentReceipt(id, "image/jpeg", 0),
            AssistantAttachmentReceipt(id, "image/jpeg", 8 * 1024 * 1024 + 1))) {
            try { assistantPhotoReceiptId(receipt); fail("Invalid receipt accepted") }
            catch (e: PhotoUploadFailure) { assertEquals("Fotoğraf yüklemesi doğrulanamadı.", e.message) }
        }
    }

    private fun serve(body: String, block: (BackendChatbotService) -> Unit): String {
        val server = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newSingleThreadExecutor()
        val request = executor.submit<String> {
            server.accept().use { socket ->
                socket.soTimeout = 5000
                val input = socket.getInputStream()
                val header = StringBuilder()
                while (!header.endsWith("\r\n\r\n")) {
                    val next = input.read(); check(next >= 0); header.append(next.toChar())
                }
                val length = Regex("Content-Length: (\\d+)", RegexOption.IGNORE_CASE).find(header)!!.groupValues[1].toInt()
                val bytes = ByteArray(length)
                var read = 0
                while (read < length) { val n = input.read(bytes, read, length - read); check(n > 0); read += n }
                val response = body.toByteArray()
                socket.getOutputStream().apply {
                    write("HTTP/1.1 201 Created\r\nContent-Type: application/json\r\nContent-Length: ${response.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    write(response); flush()
                }
                header.toString() + bytes.toString(Charsets.ISO_8859_1)
            }
        }
        return try {
            block(BackendChatbotFactory.createAt("http://127.0.0.1:${server.localPort}/"))
            request.get(6, TimeUnit.SECONDS)
        } finally { server.close(); executor.shutdownNow() }
    }

    @Test fun actualRetrofitUploadsMultipartWithAuthenticationAndVersion() {
        val id = "a1234567-1234-1234-1234-123456789abc"
        val request = serve("""{"attachment_id":"$id","media_type":"image/jpeg","size":3}""") { api ->
            runBlocking { assertEquals(id, assistantPhotoReceiptId(requestAssistantPhoto(api, "Bearer local-test", byteArrayOf(1, 2, 3)).body())) }
        }
        assertTrue(request.startsWith("POST /v1/chatbot/attachments HTTP/1.1"))
        assertTrue(request.contains("Authorization: Bearer local-test"))
        assertTrue(request.contains("X-App-Version: ${BuildConfig.VERSION_NAME}"))
        assertTrue(request.contains("name=\"file\"; filename=\"photo.jpg\""))
        assertTrue(request.contains("Content-Type: image/jpeg"))
        assertFalse(request.contains("name=\"user_id\""))
    }

    @Test fun malformedSuccessBodyUsesSafeFailure() {
        serve("""{"private-server-secret":"bad response"}""") { api ->
            runBlocking {
                try { requestAssistantPhoto(api, "Bearer local-test", byteArrayOf(1)); fail("Invalid body accepted") }
                catch (e: PhotoUploadFailure) {
                    assertEquals("Fotoğraf yükleme yanıtı doğrulanamadı.", e.message)
                    assertFalse(e.message!!.contains("private-server-secret"))
                }
            }
        }
    }
}
