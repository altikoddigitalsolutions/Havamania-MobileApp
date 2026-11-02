package com.havamania.ui.theme

import java.util.Locale

/** Native UI translations must exist before a language can be selected. */
object AppLanguageSupport {
    private val names = mapOf("TR" to "Türkçe")
    private fun code(value: String) = value.trim().uppercase(Locale.ROOT)
    fun isSupported(value: String) = names.containsKey(code(value))
    fun resolve(value: String): String = code(value).takeIf { it in names } ?: "TR"
    fun displayName(value: String): String = names.getValue(resolve(value))
}
