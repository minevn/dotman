package net.minevn.dotman.extras

import net.minevn.dotman.config.Language
import net.minevn.dotman.utils.DurationFormat
import net.minevn.dotman.utils.Utils.Companion.color
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Định dạng dùng chung cho thông báo, bossbar và /khuyenmai
 */
object ExtraFormat {
    /**
     * Định dạng mốc thời gian để hiển thị.
     * DateTimeFormatter là thread-safe nên dùng được từ các timer async.
     */
    val DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    /**
     * Thay 4 placeholder dùng chung: %NAME%, %RATE%, %FROM%, %TO%.
     * Nối §r sau tên để màu của tên không lan sang phần còn lại của dòng;
     * from/to null hiện lang.khuyenmaiTimeUnknown.
     */
    fun replacePlaceholders(
        line: String, name: String, ratePercent: Int, from: ZonedDateTime?, to: ZonedDateTime?, lang: Language
    ): String {
        return line
            .replace("%NAME%", name.color() + "§r")
            .replace("%RATE%", ratePercent.toString())
            .replace("%FROM%", from?.format(DISPLAY_FORMAT) ?: lang.khuyenmaiTimeUnknown)
            .replace("%TO%", to?.format(DISPLAY_FORMAT) ?: lang.khuyenmaiTimeUnknown)
    }

    /**
     * Thời gian còn lại tới [to] dạng DurationFormat; to null -> lang.khuyenmaiTimeUnknown
     */
    fun formatRemaining(to: ZonedDateTime?, now: ZonedDateTime, lang: Language): String {
        if (to == null) {
            return lang.khuyenmaiTimeUnknown
        }
        return DurationFormat.format(to.toInstant().toEpochMilli() - now.toInstant().toEpochMilli(), lang)
    }
}
