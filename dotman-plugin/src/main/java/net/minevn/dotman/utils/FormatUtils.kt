package net.minevn.dotman.utils

import net.minevn.dotman.config.Language
import net.minevn.libs.parseHexColorToInt
import java.text.DecimalFormat
import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat

val nonDecimalFormat = DecimalFormat("###,###")
fun formatNonDecimalDouble(value: Double) = nonDecimalFormat.format(value)

val locationFormat = DecimalFormat("###.#")
fun formatLocationDouble(value: Double) = locationFormat.format(value)

fun formatFloat(value: Float) = String.format("%.1f", value)

val dateAndTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
fun Long.formatDate() = dateAndTimeFormat.format(this)

private val CONFIG_DATE_TIME_PATTERNS = listOf("dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm")

/**
 * Parse thời gian trong file config, nhận "dd/MM/yyyy HH:mm:ss" hoặc "dd/MM/yyyy HH:mm".
 * Không lenient (31/09, 25:00 báo lỗi) và phải khớp hết chuỗi: ký tự thừa như "23:59abc" báo lỗi
 * thay vì bị SimpleDateFormat.parse(String) âm thầm bỏ qua.
 *
 * @return epoch millis
 * @throws ParseException nếu không khớp định dạng nào
 */
fun parseConfigDateTime(text: String): Long {
    val value = text.trim()
    for (pattern in CONFIG_DATE_TIME_PATTERNS) {
        val format = SimpleDateFormat(pattern).apply { isLenient = false }
        val position = ParsePosition(0)
        val date = format.parse(value, position)
        if (date != null && position.index == value.length) {
            return date.time
        }
    }
    throw ParseException("'$value' không đúng định dạng dd/MM/yyyy HH:mm hoặc dd/MM/yyyy HH:mm:ss", 0)
}

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS

/**
 * Định dạng khoảng thời gian: đơn vị lớn nhất khác 0 kèm đơn vị liền kề nhỏ hơn, không nhảy cóc đơn vị.
 * "1 ngày, 5 giờ", "5 giờ, 20 phút", "20 phút"; đơn vị liền kề bằng 0 thì bỏ ("98 ngày" chứ không "98 ngày, 15 phút").
 * Dưới 1 phút -> lang.durationNow. Âm -> coi như 0. Text lấy từ key duration-* trong messages.yml.
 *
 * @param millis khoảng thời gian tính bằng mili giây
 */
fun formatDuration(millis: Long, lang: Language): String {
    val total = millis.coerceAtLeast(0)
    val units = listOf(
        total / DAY_MILLIS to lang.durationDay,
        total % DAY_MILLIS / HOUR_MILLIS to lang.durationHour,
        total % HOUR_MILLIS / MINUTE_MILLIS to lang.durationMinute,
    )
    val largest = units.indexOfFirst { it.first > 0 }
    if (largest == -1) {
        return lang.durationNow
    }
    return units.drop(largest).take(2)
        .filter { it.first > 0 }
        .joinToString(lang.durationSeparator) { it.second.replace("%N%", it.first.toString()) }
}

fun getTimeString(time: Long): String {
    val totalsecond = time / 1000
    var timeString = ""
    val day = totalsecond / 86400
    val hour = totalsecond % 86400 / 3600
    val min = totalsecond % 86400 % 3600 / 60
    val second = totalsecond % 86400 % 3600 % 60
    if (day != 0L) timeString += "$day ngày "
    if (hour != 0L) timeString += "$hour giờ "
    if (min != 0L) timeString += "$min phút "
    val milis = time % 1000 / 100
    timeString += if (totalsecond <= 0) String.format("%s.%s giây", second, milis) else "$second giây"
    return timeString
}

/**
 * Đệ quy thay thế các placeholder trong dữ liệu (String, Map, List) bằng giá trị từ replacements.
 *
 * @param input Dữ liệu đầu vào, có thể là String, Map, List hoặc kiểu khác.
 * @param replacements Map chứa các cặp placeholder và giá trị thay thế.
 * @return Dữ liệu đã được thay thế placeholder, giữ nguyên kiểu gốc.
 */
fun applyReplacements(input: Any?, replacements: Map<String, String>): Any? {
    return when (input) {
        is String -> {
            replaceAllPlaceholders(input, replacements)
        }

        is Map<*, *> -> {
            applyReplacements(input, replacements)
        }

        is List<*> -> {
            applyReplacements(input, replacements)
        }

        else -> {
            input
        }
    }
}

/**
 * Đệ quy thay thế các placeholder trong dữ liệu (String, Map, List) bằng giá trị từ replacements.
 * Overload cho Map.
 *
 * @param inputMap Map dữ liệu đầu vào.
 * @param replacements Map chứa các cặp placeholder và giá trị thay thế.
 * @return Map đã được thay thế placeholder và chuyển đổi màu nếu cần.
 */
fun applyReplacements(inputMap: Map<*, *>, replacements: Map<String, String>): Map<*, *> {
    return inputMap.mapValues { (key, value) ->
        if (key == "color" && value is String) {
            parseHexColorToInt(value)
        } else {
            applyReplacements(value, replacements)
        }
    }
}

/**
 * Đệ quy thay thế các placeholder trong dữ liệu (String, Map, List) bằng giá trị từ replacements.
 * Overload cho List.
 *
 * @param inputList List dữ liệu đầu vào.
 * @param replacements Map chứa các cặp placeholder và giá trị thay thế.
 * @return Chuỗi đã thay thế nếu toàn bộ là String, hoặc List đã thay thế từng phần tử.
 */
fun applyReplacements(inputList: List<*>, replacements: Map<String, String>): Any {
    val isAllStrings = inputList.all { it is String }
    if (isAllStrings) {
        val joinedText = inputList.filterIsInstance<String>().joinToString("\n")
        return replaceAllPlaceholders(joinedText, replacements)
    } else {
        return inputList.map { element ->
            applyReplacements(element, replacements)
        }
    }
}

/**
 * Thay thế tất cả các placeholder trong chuỗi bằng giá trị tương ứng từ replacements.
 *
 * @param text Chuỗi chứa các placeholder cần thay thế.
 * @param replacements Map chứa các cặp placeholder và giá trị thay thế.
 * @return Chuỗi đã được thay thế toàn bộ placeholder.
 */
fun replaceAllPlaceholders(text: String, replacements: Map<String, String>): String {
    var result: String = text
    for ((placeholder, replaceValue) in replacements) {
        result = result.replace(placeholder, replaceValue, ignoreCase = false)
    }
    return result
}
