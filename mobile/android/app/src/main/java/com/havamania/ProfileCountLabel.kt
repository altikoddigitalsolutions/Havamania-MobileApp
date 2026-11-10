package com.havamania

// An empty local cache does not prove that the remote account has no records.
internal fun profileCountLabel(count: Int, confirmedEmpty: Boolean): String =
    if (count > 0 || confirmedEmpty) count.toString() else "—"
