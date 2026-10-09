package com.example.diettracker.util

/**
 * Parses and validates user-typed numbers coming from `TextField`s.
 *
 * The UI keeps raw strings in state, so all numeric conversion funnels through
 * here and returns `null` instead of throwing.
 */
object NumberParsing {

    /** Parses a non-negative decimal. Accepts "," as a decimal separator. */
    fun parseNonNegative(raw: String): Double? {
        val cleaned = raw.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value.isNaN() || value.isInfinite() || value < 0.0) return null
        // Guard against absurd values that would only come from a typo.
        if (value > MAX_VALUE) return null
        return value
    }

    /** Parses a value that must be strictly greater than zero. */
    fun parsePositive(raw: String): Double? {
        val value = parseNonNegative(raw) ?: return null
        return if (value > 0.0) value else null
    }

    /** Formats a Double for pre-filling a text field, without trailing ".0". */
    fun formatForInput(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == Math.floor(rounded) && !rounded.isInfinite()) {
            rounded.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    /** Keeps only characters that can be part of a decimal number. */
    fun sanitizeDecimalInput(raw: String): String {
        val builder = StringBuilder()
        var dotSeen = false
        for (ch in raw) {
            when {
                ch.isDigit() -> builder.append(ch)
                (ch == '.' || ch == ',') && !dotSeen -> {
                    builder.append('.')
                    dotSeen = true
                }
            }
        }
        return builder.toString()
    }

    private const val MAX_VALUE = 100_000.0
}
