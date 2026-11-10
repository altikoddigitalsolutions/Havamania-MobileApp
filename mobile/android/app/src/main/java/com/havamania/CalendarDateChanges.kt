package com.havamania

import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

// Collect only while the screen is resumed. Re-read the system date so manual
// clock/time-zone changes are picked up too, without reloading trips or network data.
internal fun calendarDateChanges(
    today: () -> LocalDate = { LocalDate.now() },
    awaitNextCheck: suspend () -> Unit = { delay(60_000) }
) = flow {
    while (true) {
        emit(today())
        awaitNextCheck()
    }
}.distinctUntilChanged()
