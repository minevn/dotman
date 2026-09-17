package net.minevn.dotman.extras

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

internal const val MINUTES_PER_DAY = 24 * 60

private val DAY_NAMES = mapOf(
    DayOfWeek.MONDAY to "T2",
    DayOfWeek.TUESDAY to "T3",
    DayOfWeek.WEDNESDAY to "T4",
    DayOfWeek.THURSDAY to "T5",
    DayOfWeek.FRIDAY to "T6",
    DayOfWeek.SATURDAY to "T7",
    DayOfWeek.SUNDAY to "CN",
)

/**
 * Khung thời gian của một khuyến mãi: đang diễn ra hoặc khung kế tiếp
 *
 * @param from Mốc bắt đầu
 * @param to Mốc kết thúc, null nếu không bao giờ kết thúc
 * @param active true nếu khung này đang diễn ra tại thời điểm tính
 */
class Window(val from: ZonedDateTime, val to: ZonedDateTime?, val active: Boolean)

/**
 * Lịch hoạt động của một khuyến mãi
 */
sealed interface Schedule {
    val isRepeating: Boolean

    fun isActive(now: ZonedDateTime): Boolean

    /**
     * Khung đang diễn ra, hoặc khung kế tiếp nếu chưa diễn ra
     *
     * @return null nếu không còn khung nào
     */
    fun window(now: ZonedDateTime): Window?
}

/**
 * from/to là mốc thời gian cố định (epoch millis), bao gồm cả hai đầu
 */
class FixedSchedule(val from: Long, val to: Long) : Schedule {
    override val isRepeating = false

    override fun isActive(now: ZonedDateTime): Boolean {
        val currentTime = now.toInstant().toEpochMilli()
        return from <= currentTime && to >= currentTime
    }

    override fun window(now: ZonedDateTime): Window? {
        val currentTime = now.toInstant().toEpochMilli()
        if (to < currentTime) {
            return null
        }
        return Window(from.toZoned(now), to.toZoned(now), from <= currentTime)
    }
}

/**
 * Lặp lại theo tuần, tùy chọn giới hạn trong khoảng from..to
 *
 * @param days thứ áp dụng
 * @param startMinute phút trong ngày bắt đầu (bao gồm), 0..1439
 * @param endMinute phút trong ngày kết thúc (loại trừ), 1..1440
 * @param from mốc bắt đầu áp dụng (epoch millis), null = không giới hạn
 * @param to mốc kết thúc áp dụng (epoch millis, bao gồm), null = không giới hạn
 */
class WeeklySchedule(
    val days: Set<DayOfWeek>, val startMinute: Int, val endMinute: Int,
    val from: Long?, val to: Long?
) : Schedule {
    override val isRepeating = true

    val wholeDay get() = startMinute == 0 && endMinute == MINUTES_PER_DAY

    override fun isActive(now: ZonedDateTime): Boolean {
        val currentTime = now.toInstant().toEpochMilli()
        val inRange = (from == null || from <= currentTime) && (to == null || currentTime <= to)
        val minute = now.hour * 60 + now.minute
        return inRange && now.dayOfWeek in days && startMinute <= minute && minute < endMinute
    }

    override fun window(now: ZonedDateTime): Window? {
        val currentTime = now.toInstant().toEpochMilli()
        if (to != null && currentTime > to) {
            return null
        }
        val ref = if (from != null && from > currentTime) from.toZoned(now) else now
        val weekly = weeklyWindow(ref)
        if (to != null && weekly.from.toInstant().toEpochMilli() > to) {
            return null
        }
        if (from == null && to == null) {
            return weekly
        }

        val fromZoned = from?.toZoned(now)
        val toZoned = to?.toZoned(now)
        val windowFrom = if (fromZoned != null && fromZoned > weekly.from) fromZoned else weekly.from
        val windowTo = when {
            weekly.to == null -> toZoned
            toZoned != null && toZoned < weekly.to -> toZoned
            else -> weekly.to
        }
        return Window(windowFrom, windowTo, isActive(now))
    }

    /**
     * Khung tuần tại thời điểm ref, chưa xét giới hạn from/to
     */
    private fun weeklyWindow(ref: ZonedDateTime): Window {
        val today = ref.toLocalDate()
        val minute = ref.hour * 60 + ref.minute
        val zone = ref.zone

        if (!wholeDay) {
            // Mỗi ngày một khung riêng
            var day = today
            if (day.dayOfWeek !in days || minute >= endMinute) {
                day = nextDay(today)
            }
            val active = day == today && minute >= startMinute
            return Window(day.atMinute(startMinute, zone), day.atMinute(endMinute, zone), active)
        }

        // Cả ngày: gộp các thứ liên tiếp thành một khung
        if (days.size == DayOfWeek.values().size) {
            return Window(today.atMinute(0, zone), null, true)
        }
        val day = if (today.dayOfWeek in days) today else nextDay(today)
        var first = day
        while (first.minusDays(1).dayOfWeek in days) {
            first = first.minusDays(1)
        }
        var last = day
        while (last.plusDays(1).dayOfWeek in days) {
            last = last.plusDays(1)
        }
        return Window(first.atMinute(0, zone), last.plusDays(1).atMinute(0, zone), day == today)
    }

    /**
     * Ngày gần nhất sau [date] có thứ nằm trong days (luôn tìm được trong 7 ngày)
     */
    private fun nextDay(date: LocalDate): LocalDate {
        var day = date.plusDays(1)
        while (day.dayOfWeek !in days) {
            day = day.plusDays(1)
        }
        return day
    }

    /**
     * Mô tả lịch để log lúc nạp file, ví dụ: "T7, CN, cả ngày; kế tiếp 19/09/2026 00:00 -> 21/09/2026 00:00"
     */
    fun describe(now: ZonedDateTime): String {
        val dayText = if (days.size == DayOfWeek.values().size) {
            "mọi ngày"
        } else {
            DayOfWeek.values().filter { it in days }.joinToString(", ") { DAY_NAMES.getValue(it) }
        }
        val hourText = if (wholeDay) {
            "cả ngày"
        } else {
            "${startMinute.toHourText()}-${endMinute.toHourText()}"
        }
        val rangeText = buildString {
            from?.let { append(", từ ${it.toZoned(now).format(ExtraFormat.DISPLAY_FORMAT)}") }
            to?.let { append(", đến ${it.toZoned(now).format(ExtraFormat.DISPLAY_FORMAT)}") }
        }
        val window = window(now)
        val windowText = if (window == null) {
            "đã kết thúc"
        } else {
            val state = if (window.active) "đang chạy" else "kế tiếp"
            val end = window.to?.format(ExtraFormat.DISPLAY_FORMAT) ?: "không kết thúc"
            "$state ${window.from.format(ExtraFormat.DISPLAY_FORMAT)} -> $end"
        }
        return "$dayText, $hourText$rangeText; $windowText"
    }
}

internal fun Long.toZoned(reference: ZonedDateTime): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(this), reference.zone)

private fun LocalDate.atMinute(minute: Int, zone: ZoneId): ZonedDateTime =
    atStartOfDay(zone).plusMinutes(minute.toLong())

private fun Int.toHourText() = "%02d:%02d".format(this / 60, this % 60)
