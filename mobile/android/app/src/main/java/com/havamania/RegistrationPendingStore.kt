package com.havamania

import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal interface RegistrationPendingStore {
    fun read(): String?
    fun write(value: String?)
}
internal class MemoryRegistrationPendingStore : RegistrationPendingStore {
    private var value: String? = null
    override fun read() = value
    override fun write(value: String?) { this.value = value }
}

/** Small synchronous, durable reservation before SDK creation. Never stores passwords or tokens. */
internal class FileRegistrationPendingStore(private val file: File) : RegistrationPendingStore {
    private val atomic = AtomicFile(file)
    override fun read(): String? = try {
        atomic.openRead().use {
            if (it.channel.size() > 16_384) throw IOException("Registration journal too large")
            it.readBytes().toString(Charsets.UTF_8).takeUnless { text -> text == "null" }
        }
    } catch (missing: FileNotFoundException) {
        if (file.exists() || File(file.path + ".bak").exists() || File(file.path + ".new").exists()) throw missing
        null
    }
    override fun write(value: String?) {
        val bytes = (value ?: "null").toByteArray(Charsets.UTF_8)
        require(bytes.size <= 16_384)
        val output = atomic.startWrite()
        try {
            output.write(bytes)
            output.fd.sync()
            atomic.finishWrite(output)
        } catch (failure: Exception) {
            atomic.failWrite(output)
            throw failure
        }
    }
}

@Serializable
internal data class PendingRegistration(val version: Int = 1, val name: String, val email: String, val uid: String? = null) {
    fun validate(): PendingRegistration {
        require(version == 1 && name.length <= 2048 && email.isNotBlank() && email.length <= 512)
        require(uid == null || (uid.isNotBlank() && uid.length <= 256))
        return this
    }
    fun encode() = Json.encodeToString(validate())
    companion object {
        fun decode(text: String): PendingRegistration {
            require(text.length <= 16_384)
            return Json.decodeFromString<PendingRegistration>(text).validate()
        }
    }
}