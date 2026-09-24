package net.minevn.dotman.utils

import net.minevn.dotman.config.Language

object DurationFormat {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /**
     * Đơn vị lớn nhất khác 0 kèm đơn vị liền kề nhỏ hơn, không nhảy cóc đơn vị:
     * "1 ngày, 5 giờ", "5 giờ, 20 phút", "20 phút"; đơn vị liền kề bằng 0 thì bỏ ("98 ngày" chứ không "98 ngày, 15 phút").
     * Dưới 1 phút -> lang.durationNow. Âm -> coi như 0.
     *
     * @param millis khoảng thời gian tính bằng mili giây
     */
    fun format(millis: Long, lang: Language): String {
        val total = millis.coerceAtLeast(0)
        val days = total / DAY
        val hours = total % DAY / HOUR
        val minutes = total % HOUR / MINUTE

        val units = listOf(
            days to lang.durationDay,
            hours to lang.durationHour,
            minutes to lang.durationMinute,
        )
        val largest = units.indexOfFirst { it.first > 0 }
        if (largest == -1) {
            return lang.durationNow
        }
        return units.drop(largest).take(2)
            .filter { it.first > 0 }
            .joinToString(lang.durationSeparator) { it.second.replace("%N%", it.first.toString()) }
    }
}
