package com.havamania

private val temperatureTextPattern = Regex("""^\s*([+-]?\d+(?:[.,]\d+)?)\s*(?:°\s*[cC]?|[cC])?\s*$""")

internal fun measuredTemperature(text: String, range: ClosedFloatingPointRange<Double> = -100.0..65.0): Double? {
    val normalized = text.trim().replace('−', '-').replace('\u00a0', ' ').replace('\u202f', ' ')
    val value = temperatureTextPattern.matchEntire(normalized)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
    return value?.takeIf { it.isFinite() && it in range }
}

internal fun temperatureNumber(value: Double): String =
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')
