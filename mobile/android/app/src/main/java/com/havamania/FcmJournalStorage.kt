package com.havamania

import android.app.Application
import android.util.AtomicFile
import java.io.File

/** Storage only; constructing this does not obtain/register an FCM token or contact the backend. */
internal class AtomicFcmJournalStorage(app: Application) : FcmJournalStorage {
    private val file = AtomicFile(File(app.noBackupFilesDir, "fcm-token-queue.json"))
    override fun read(): String? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        return file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
    override fun write(value: String) {
        val stream = file.startWrite()
        try {
            stream.write(value.toByteArray(Charsets.UTF_8))
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
