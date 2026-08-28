package com.havamania

import java.time.LocalDate
import java.time.LocalDateTime

interface TimeProvider {
    fun now(): LocalDateTime
    fun today(): LocalDate
}

object DefaultTimeProvider : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()
    override fun today(): LocalDate = LocalDate.now()
}
