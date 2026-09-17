package net.minevn.dotman.utils

import net.minevn.dotman.config.Language

object DurationFormat {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /**
     * Ghép tối đa 2 đơn vị lớn nhất khác 0: "1 ngày 5 giờ", "5 giờ 20 phút", "20 phút".
     * Dưới 1 phút -> lang.durationNow. Âm -> coi như 0.
     *
     * @param millis khoảng thời gian tính bằng mili giây
     */
    fun format(millis: Long, lang: Language): String {
        val total = millis.coerceAtLeast(0)
        val days = total / DAY
        val hours = total % DAY / HOUR
        val minutes = total % HOUR / MINUTE

        val parts = listOf(
            days to lang.durationDay,
            hours to lang.durationHour,
            minutes to lang.durationMinute,
        )
            .filter { it.first > 0 }
            .take(2)
            .map { it.second.replace("%N%", it.first.toString()) }

        if (parts.isEmpty()) {
            return lang.durationNow
        }
        return parts.joinToString(" ")
    }
}
