package net.minevn.dotman.utils

import net.minevn.libs.parseHexColorToInt
import java.text.DecimalFormat
import java.text.SimpleDateFormat

val nonDecimalFormat = DecimalFormat("###,###")
fun formatNonDecimalDouble(value: Double) = nonDecimalFormat.format(value)

val locationFormat = DecimalFormat("###.#")
fun formatLocationDouble(value: Double) = locationFormat.format(value)

fun formatFloat(value: Float) = String.format("%.1f", value)

val dateAndTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
fun Long.formatDate() = dateAndTimeFormat.format(this)

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
            processMap(input, replacements)
        }

        is List<*> -> {
            processList(input, replacements)
        }

        else -> {
            input
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

/**
 * Xử lý Map: đệ quy thay thế các placeholder cho từng value trong Map.
 * Đặc biệt, nếu key là "color" và value là String, sẽ convert từ mã màu hex sang int.
 *
 * @param map Map dữ liệu đầu vào.
 * @param replacements Map chứa các cặp placeholder và giá trị thay thế.
 * @return Map đã được thay thế placeholder và chuyển đổi màu nếu cần.
 */
fun processMap(map: Map<*, *>, replacements: Map<String, String>): Map<*, *> {
    return map.mapValues { (key, value) ->
        if (key == "color" && value is String) {
            parseHexColorToInt(value)
        } else {
            applyReplacements(value, replacements)
        }
    }
}

/**
 * Xử lý List: nếu toàn bộ phần tử là String thì join thành 1 chuỗi và thay thế placeholder,
 * ngược lại đệ quy thay thế từng phần tử trong List.
 *
 * @param list List dữ liệu đầu vào.
 * @param replacements Map chứa các cặp placeholder và giá trị thay thế.
 * @return Chuỗi đã thay thế nếu toàn bộ là String, hoặc List đã thay thế từng phần tử.
 */
fun processList(list: List<*>, replacements: Map<String, String>): Any {
    val isAllStrings = list.all { it is String }
    if (isAllStrings) {
        val joinedText = list.filterIsInstance<String>().joinToString("\n")
        return replaceAllPlaceholders(joinedText, replacements)
    } else {
        return list.map { element ->
            applyReplacements(element, replacements)
        }
    }
}
