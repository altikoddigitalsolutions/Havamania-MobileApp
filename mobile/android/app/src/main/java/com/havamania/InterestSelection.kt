package com.havamania

/** Apply an explicit intent to the transaction's current list, never a stale UI copy. */
internal fun updateInterestSelection(current: List<String>, id: String, selected: Boolean): List<String> {
    require(id.isNotBlank() && id.length <= 100 && ',' !in id)
    return if (selected) (current + id).distinct() else current.filterNot { it == id }
}
