package net.minevn.dotman.config

import net.minevn.dotman.extras.FixedSchedule
import net.minevn.dotman.extras.MINUTES_PER_DAY
import net.minevn.dotman.extras.PlannedExtra
import net.minevn.dotman.extras.Schedule
import net.minevn.dotman.extras.WeeklySchedule
import net.minevn.dotman.utils.Utils.Companion.color
import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.dotman.utils.Utils.Companion.warning
import org.bukkit.Bukkit
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.ZonedDateTime

class PlannedExtras : FileConfig("khuyenmai") {

    private var components: List<PlannedExtra> = emptyList()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

    /**
     * Tên hiển thị cho khuyến mãi legacy trong config.yml (extra-rate), vì loại này không có tên
     */
    val legacyName: String get() = get("legacy-name")

    init {
        loadComponents()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadComponents() {
        components = (config.getList("khuyen-mai") ?: emptyList()).mapNotNull {
            try {
                it as Map<*, *>
                val name = it["name"] as String
                val rate = (it["rate"] as Number).toDouble()
                PlannedExtra(name, rate, parseSchedule(name, it))
            } catch (e: Exception) {
                val name = try { (it as Map<*, *>)["name"] as String } catch (_: Exception) { "<unknown>" }
                e.warning("Khuyến mãi $name không hợp lệ: ${e.message}")
                null
            }
        }

        info("Đã nạp ${components.size} khuyến mãi")
        val now = ZonedDateTime.now()
        components.forEach { component ->
            val schedule = component.schedule
            if (schedule is WeeklySchedule) {
                info("Khuyến mãi '${component.name}' (lặp lại): ${schedule.describe(now)}")
            }
        }

        val console = Bukkit.getServer().consoleSender
        val activeComponents = components.filter { it.isActive(now) }.sortedByDescending { it.rate }
        if (activeComponents.isNotEmpty()) {
            val size = activeComponents.size
            console.send("Có $size chương trình khuyến mãi đang hoạt động:")
            console.send(activeComponents.joinToString("§r, ") { "${it.name.color()} §r(§b${it.getPercentage()}%§r)" })
            if (size > 1) {
                console.send("Khuyến mãi có tỉ lệ cao nhất (đầu danh sách) sẽ được ưu tiên.")
            }
        } else {
            console.send("Không có chương trình khuyến mãi nào đang hoạt động.")
        }
    }

    /**
     * Đọc lịch của một khuyến mãi từ map trong khuyenmai.yml.
     * - Chỉ có from/to: ngày cố định
     * - Có days hoặc hours: lặp lại theo tuần; from/to (nếu có) là giới hạn khoảng áp dụng
     */
    private fun parseSchedule(name: String, map: Map<*, *>): Schedule {
        val hasWeekly = map.containsKey("days") || map.containsKey("hours")
        val from = map["from"]?.let { dateFormat.parse(it.toString().trim()).time }
        val to = map["to"]?.let { dateFormat.parse(it.toString().trim()).time }

        if (from != null && to != null && from >= to) {
            warning("Khuyến mãi '$name' có thời gian bắt đầu không hợp lệ: ${map["from"]} >= ${map["to"]}")
        }

        if (!hasWeekly) {
            if (from == null || to == null) {
                throw IllegalArgumentException("thiếu from/to hoặc days/hours")
            }
            return FixedSchedule(from, to)
        }

        val days = map["days"]?.let { parseDays(it) } ?: DayOfWeek.values().toSet()
        val hours = map["hours"]?.let { parseHours(it.toString().trim()) } ?: (0 to MINUTES_PER_DAY)
        return WeeklySchedule(days, hours.first, hours.second, from, to)
    }

    override fun reload() {
        super.reload()
        loadComponents()
    }

    /**
     * Toàn bộ khuyến mãi đã nạp, theo thứ tự trong file
     */
    fun getAll(): List<PlannedExtra> = components

    /**
     * Lấy khuyến mãi đang hoạt động tại thời điểm chỉ định
     * Nếu có nhiều khuyến mãi cùng lúc, sẽ lấy khuyến mãi có tỉ lệ cao nhất
     *
     * @param now Thời điểm cần kiểm tra, mặc định là hiện tại
     * @return Khuyến mãi đang hoạt động, hoặc null nếu không có
     */
    fun getCurrentExtra(now: ZonedDateTime = ZonedDateTime.now()): PlannedExtra? = pickCurrent(components, now)

    companion object {
        private val HOURS_REGEX = Regex("^(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})$")

        /**
         * Logic thuần của getCurrentExtra, không phụ thuộc trạng thái PlannedExtras
         */
        internal fun pickCurrent(components: List<PlannedExtra>, now: ZonedDateTime): PlannedExtra? =
            components.filter { it.isActive(now) }.maxByOrNull { it.rate }

        /**
         * Parse thứ trong tuần: [7, 8] / "7, 8" / 7 -> {SATURDAY, SUNDAY}.
         * 2..7 = thứ 2..thứ 7, 8 = chủ nhật.
         *
         * @throws IllegalArgumentException nếu rỗng hoặc có giá trị không hợp lệ
         */
        internal fun parseDays(value: Any): Set<DayOfWeek> {
            val items = if (value is List<*>) {
                value.map { it.toString() }
            } else {
                value.toString().split(",")
            }
            val trimmed = items.map { it.trim() }.filter { it.isNotEmpty() }
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("days rỗng")
            }
            return trimmed.map { item ->
                val number = item.toIntOrNull()?.takeIf { it in 2..8 }
                    ?: throw IllegalArgumentException("thứ '$item' không hợp lệ, chỉ nhận 2..8 (8 = chủ nhật)")
                DayOfWeek.of(number - 1)
            }.toSet()
        }

        /**
         * Parse khung giờ "HH:mm-HH:mm" -> (phút bắt đầu, phút kết thúc), ví dụ "18:00-22:00" -> (1080, 1320).
         * Kết thúc tối đa 24:00; không hỗ trợ qua nửa đêm.
         *
         * @throws IllegalArgumentException nếu sai định dạng hoặc giá trị
         */
        internal fun parseHours(value: String): Pair<Int, Int> {
            val match = HOURS_REGEX.find(value)
                ?: throw IllegalArgumentException("hours '$value' không đúng định dạng HH:mm-HH:mm")
            val (startHour, startMin, endHour, endMin) = match.destructured.toList().map { it.toInt() }
            val start = startHour * 60 + startMin
            val end = endHour * 60 + endMin
            if (startMin >= 60 || endMin >= 60 || start !in 0 until MINUTES_PER_DAY || end !in 1..MINUTES_PER_DAY) {
                throw IllegalArgumentException("hours '$value' có giờ/phút không hợp lệ")
            }
            if (start >= end) {
                throw IllegalArgumentException("hours '$value' phải có giờ bắt đầu trước giờ kết thúc, không hỗ trợ qua nửa đêm")
            }
            return start to end
        }
    }
}
