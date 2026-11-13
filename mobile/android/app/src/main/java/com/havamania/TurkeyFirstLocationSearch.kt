package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Fetch TR separately so the global result cap cannot exclude domestic matches. */
internal suspend fun turkeyFirstLocationSearch(
    query: String = "", search: suspend (String?) -> List<GeocodingResultDto>
): List<GeocodingResultDto> = coroutineScope {
    suspend fun attempt(country: String?): Result<List<GeocodingResultDto>> = try {
        Result.success(search(country))
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Result.failure(error) }
    val domestic = async { attempt("TR") }
    val global = async { attempt(null) }
    val first = domestic.await()
    val second = global.await()
    val prefix = AiIntentParser.normalizeTurkish(query.substringBefore(","))
    val merged = (first.getOrNull().orEmpty() + second.getOrNull().orEmpty())
        .distinctBy(::locationSuggestionKey)
        .sortedWith(compareBy<GeocodingResultDto> { if (it.countryCode?.trim().equals("TR", ignoreCase = true)) 0 else 1 }
            .thenBy { if (AiIntentParser.normalizeTurkish(it.name).startsWith(prefix)) 0 else 1 })
    if (merged.isEmpty()) (first.exceptionOrNull() ?: second.exceptionOrNull())?.let { throw it }
    merged
}