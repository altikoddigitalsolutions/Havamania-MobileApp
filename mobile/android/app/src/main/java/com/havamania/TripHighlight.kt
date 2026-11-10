package com.havamania

internal fun isTripHighlighted(id: String, city: String, focusId: String?, query: String?): Boolean =
    id == focusId || (!query.isNullOrBlank() && city.contains(query.trim(), ignoreCase = true))
