package net.minevn.dotman.config

import net.minevn.dotman.extras.FixedSchedule
import net.minevn.dotman.extras.MINUTES_PER_DAY
import net.minevn.dotman.extras.PlannedExtraEntry
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

/**
 * Nạp và giữ danh sách khuyến mãi theo lịch từ khuyenmai.yml (mỗi mục là một PlannedExtraEntry)
 */
class PlannedExtras : FileConfig("khuyenmai") {

    private var components: List<PlannedExtraEntry> = emptyList()
    // isLenient = false: ngày sai (31/09, 25:00) báo lỗi thay vì tự cuộn sang ngày/tháng kế
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm").apply { isLenient = false }

    /**
     * Tên hiển thị cho khuyến mãi legacy trong config.yml (extra-rate), vì loại này không có tên
     */
    val legacyName: String get() = get("legacy-name")

    init {
        loadComponents()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadComponents() {
        val console = Bukkit.getServer().consoleSender
        components = (config.getList("khuyen-mai") ?: emptyList()).mapNotNull {
            try {
                it as Map<*, *>
                val name = it["name"] as String
                val rate = (it["rate"] as Number).toDouble()
                PlannedExtraEntry(name, rate, parseSchedule(it))
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
                console.send("Khuyến mãi '${component.name.color()}' §r(lặp lại): ${schedule.describe(now)}")
            }
        }

        val activeComponents = components.filter { it.isActive(now) }.sortedByDescending { it.rate }
        val applied = activeComponents.firstOrNull()
        if (applied == null) {
            console.send("Không có chương trình khuyến mãi nào đang hoạt động.")
        } else {
            console.send("Áp dụng khuyến mãi: ${applied.name.color()} §r(§b${applied.getPercentage()}%§r)")
            val ignored = activeComponents.drop(1)
            if (ignored.isNotEmpty()) {
                console.send("Đang có ${activeComponents.size} khuyến mãi cùng hoạt động, khuyến mãi có tỉ lệ cao nhất đã được áp dụng, các khuyến mãi còn lại bị bỏ qua:")
                ignored.forEach { console.send(" - ${it.name.color()} §r(§b${it.getPercentage()}%§r)") }
            }
        }
    }

    /**
     * Đọc lịch của một khuyến mãi từ map trong khuyenmai.yml.
     * - Chỉ có from/to: ngày cố định
     * - Có days hoặc hours: lặp lại theo tuần; from/to (nếu có) là giới hạn khoảng áp dụng
     */
    private fun parseSchedule(map: Map<*, *>): Schedule {
        val hasWeekly = map.containsKey("days") || map.containsKey("hours")
        // Bỏ hẳn key = không giới hạn; có key mà để trống là lỗi, giống days/hours
        val from = parseTime(map, "from")
        val to = parseTime(map, "to")

        if (from != null && to != null && from >= to) {
            throw IllegalArgumentException("thời gian bắt đầu phải trước thời gian kết thúc: ${map["from"]} >= ${map["to"]}")
        }

        if (!hasWeekly) {
            if (from == null || to == null) {
                throw IllegalArgumentException("thiếu from/to hoặc days/hours")
            }
            return FixedSchedule(from, to)
        }

        // Bỏ hẳn key = mọi ngày / cả ngày; có key mà để trống là lỗi để tránh vô tình bật khuyến mãi mọi lúc
        val days = if (map.containsKey("days")) {
            parseDays(requireValue(map, "days"))
        } else {
            DayOfWeek.values().toSet()
        }
        val hours = if (map.containsKey("hours")) {
            parseHours(requireValue(map, "hours").toString().trim())
        } else {
            0 to MINUTES_PER_DAY
        }
        return WeeklySchedule(days, hours.first, hours.second, from, to)
    }

    private fun requireValue(map: Map<*, *>, key: String): Any =
        map[key] ?: throw IllegalArgumentException("$key rỗng")

    /**
     * Đọc mốc thời gian from/to (epoch millis)
     *
     * @return null nếu không có key
     * @throws IllegalArgumentException nếu có key mà để trống
     */
    private fun parseTime(map: Map<*, *>, key: String): Long? {
        if (!map.containsKey(key)) {
            return null
        }
        return dateFormat.parse(requireValue(map, key).toString().trim()).time
    }

    override fun reload() {
        super.reload()
        loadComponents()
    }

    /**
     * Toàn bộ khuyến mãi đã nạp, theo thứ tự trong file
     */
    fun getAll(): List<PlannedExtraEntry> = components

    /**
     * Lấy khuyến mãi đang hoạt động tại thời điểm chỉ định
     * Nếu có nhiều khuyến mãi cùng lúc, sẽ lấy khuyến mãi có tỉ lệ cao nhất
     *
     * @param now Thời điểm cần kiểm tra, mặc định là hiện tại
     * @return Khuyến mãi đang hoạt động, hoặc null nếu không có
     */
    fun getCurrentExtra(now: ZonedDateTime = ZonedDateTime.now()): PlannedExtraEntry? = pickCurrent(components, now)

    companion object {
        private val HOURS_REGEX = Regex("^(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})$")

        /**
         * Logic thuần của getCurrentExtra, không phụ thuộc trạng thái PlannedExtras
         */
        internal fun pickCurrent(components: List<PlannedExtraEntry>, now: ZonedDateTime): PlannedExtraEntry? =
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
