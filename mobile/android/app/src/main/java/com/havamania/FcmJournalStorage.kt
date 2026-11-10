package com.havamania

import android.app.Application
import android.util.AtomicFile
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Storage only; constructing this does not obtain/register an FCM token or contact the backend. */
internal class AtomicFcmJournalStorage private constructor(private val file: AtomicFile) : FcmJournalStorage {
    constructor(app: Application) : this(AtomicFile(File(app.noBackupFilesDir, "fcm-token-queue.json")))
    constructor(path: File) : this(AtomicFile(path))

    private val maxBytes = 4096
    override fun read(): String? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        return file.openRead().use { stream ->
            val bytes = ByteArray(maxBytes + 1)
            var count = 0
            while (count < bytes.size) {
                val read = stream.read(bytes, count, bytes.size - count)
                if (read < 0) break
                count += read
            }
            check(count <= maxBytes) { "FCM journal exceeds allowed size" }
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, 0, count)).toString()
        }
    }
    override fun write(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        check(bytes.size <= maxBytes) { "FCM journal exceeds allowed size" }
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
        // finishWrite has already closed/committed the stream. A verification
        // failure must not call failWrite on that committed transaction.
        check(read() == value) { "FCM journal write was not persisted" }
    }
}
