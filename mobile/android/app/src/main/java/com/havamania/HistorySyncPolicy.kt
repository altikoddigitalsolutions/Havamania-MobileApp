package com.havamania

/** A removal must not discard a newer local edit or rely on an offline/pending snapshot. */
internal fun shouldApplyHistoryRemoval(
    localUpdatedAt: Long,
    removedUpdatedAt: Long,
    fromCache: Boolean,
    hasPendingWrites: Boolean
): Boolean = !fromCache && !hasPendingWrites && localUpdatedAt <= removedUpdatedAt
