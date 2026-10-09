package com.example.diettracker.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date helpers. The app stores days as ISO-8601 `yyyy-MM-dd` strings in the
 * device's default time zone, which sorts correctly and is trivial to index.
 */
object DateUtils {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val DISPLAY: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
    private val WEEKDAY: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)

    fun today(): String = LocalDate.now().format(ISO)

    fun toIso(date: LocalDate): String = date.format(ISO)

    fun parse(iso: String): LocalDate =
        runCatching { LocalDate.parse(iso, ISO) }.getOrDefault(LocalDate.now())

    fun displayDate(iso: String): String = parse(iso).format(DISPLAY)

    fun weekday(iso: String): String = parse(iso).format(WEEKDAY)

    /** True when [iso] is the device's current day. */
    fun isToday(iso: String): Boolean = iso == today()

    fun plusDays(iso: String, days: Long): String = toIso(parse(iso).plusDays(days))

    fun minusDays(iso: String, days: Long): String = plusDays(iso, -days)

    /** "今天" / "昨天" / "明天" / the plain date. */
    fun friendlyLabel(iso: String): String = when (iso) {
        today() -> "今天"
        minusDays(today(), 1) -> "昨天"
        plusDays(today(), 1) -> "明天"
        else -> displayDate(iso)
    }

    /**
     * UTC midnight of [iso], which is what Material 3's `DatePicker` expects for
     * `initialSelectedDateMillis` / `selectedDateMillis`.
     */
    fun toEpochMillis(iso: String): Long =
        parse(iso).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

    /** Inverse of [toEpochMillis]. */
    fun fromEpochMillis(millis: Long): String =
        java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneOffset.UTC)
            .toLocalDate()
            .format(ISO)
}
