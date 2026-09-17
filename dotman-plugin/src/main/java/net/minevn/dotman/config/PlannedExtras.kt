package net.minevn.dotman.config

import net.minevn.dotman.utils.BukkitBossBar
import net.minevn.dotman.utils.DurationFormat
import net.minevn.dotman.utils.Utils.Companion.color
import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.runAsyncTimer
import net.minevn.dotman.utils.Utils.Companion.runSync
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.dotman.utils.Utils.Companion.warning
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PlannedExtras : FileConfig("khuyenmai") {

    private var components: List<Component> = emptyList()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
    private var announceTask: BukkitTask? = null
    private var bossBar: BukkitBossBar? = null
    private var bossBarTask: BukkitTask? = null

    /**
     * Tên hiển thị cho khuyến mãi legacy trong config.yml (extra-rate), vì loại này không có tên
     */
    val legacyName: String get() = get("legacy-name")

    init {
        loadComponents()
        startAnnouncer()
        startBossBar()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadComponents() {
        components = (config.getList("khuyen-mai") ?: emptyList()).mapNotNull {
            try {
                it as Map<*, *>
                val name = it["name"] as String
                val rate = (it["rate"] as Number).toDouble()
                Component(name, rate, parseSchedule(name, it))
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

    /**
     * Tạo timer gửi thông báo khuyến mãi định kỳ ra chat theo section thong-bao
     */
    private fun startAnnouncer() {
        val enabled = config.getBoolean("thong-bao.enabled", true)
        val interval = config.getInt("thong-bao.interval", 300)
        if (!enabled || interval <= 0) {
            return
        }
        val message = getList("thong-bao.message")
        if (message.isEmpty()) {
            warning("thong-bao.message trống, không gửi thông báo khuyến mãi")
            return
        }
        // delay 0: gửi ngay lần đầu khi start/reload
        announceTask = runAsyncTimer(0, interval * 20L) { announce(message) }
        info("Thông báo khuyến mãi mỗi $interval giây")
    }

    private fun announce(message: List<String>) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return
        }
        val now = ZonedDateTime.now()
        val announcement = currentAnnouncement(now) ?: return
        message.forEach { Bukkit.broadcastMessage(formatLine(it, announcement, now, main.language)) }
    }

    /**
     * Tạo bossbar hiện trong suốt thời gian có khuyến mãi, tiêu đề luân phiên theo thong-bao.bossbar.titles
     */
    private fun startBossBar() {
        if (!config.getBoolean("thong-bao.bossbar.enabled", true)) {
            return
        }
        val titles = getList("thong-bao.bossbar.titles")
        if (titles.isEmpty()) {
            warning("thong-bao.bossbar.titles trống, không hiển thị bossbar")
            return
        }
        val style = config.getString("thong-bao.bossbar.style", "SEGMENTED_10")!!
        val rotate = config.getInt("thong-bao.bossbar.rotate", 5).coerceAtLeast(1)
        val bar = try {
            BukkitBossBar("§r", "GREEN", style)
        } catch (e: IllegalArgumentException) {
            e.warning("thong-bao.bossbar.style không hợp lệ, không hiển thị bossbar")
            return
        }
        bar.isVisible = false
        bossBar = bar
        var index = 0
        bossBarTask = runAsyncTimer(0, rotate * 20L) {
            val now = ZonedDateTime.now()
            val announcement = currentAnnouncement(now)
            if (announcement == null) {
                if (bar.isVisible) {
                    bar.removeAll()
                    bar.isVisible = false
                }
                return@runAsyncTimer
            }
            val progress = bossBarProgress(announcement, now)
            bar.setTitle(formatLine(titles[index % titles.size], announcement, now, main.language))
            bar.progress = progress
            bar.color = bossBarColor(progress)
            index++
            if (!bar.isVisible) {
                bar.isVisible = true
                runSync { Bukkit.getOnlinePlayers().forEach { bar.addPlayer(it) } }
            }
        }
        info("Bossbar khuyến mãi: ${titles.size} tiêu đề, đổi mỗi $rotate giây")
    }

    /**
     * Đọc config hiện tại rồi giao cho resolveAnnouncement
     */
    private fun currentAnnouncement(now: ZonedDateTime): Announcement? {
        val cfg = main.config
        return resolveAnnouncement(components, cfg.extraRate, cfg.extraUntil, legacyName, now)
    }

    /**
     * Add người chơi mới vào bossbar nếu đang hiện; gọi từ DotManListener.onJoin
     */
    fun onJoin(player: Player) {
        bossBar?.takeIf { it.isVisible }?.addPlayer(player)
    }

    /**
     * Hủy timer và bossbar; gọi trước khi tạo PlannedExtras mới hoặc khi disable plugin
     */
    fun stop() {
        announceTask?.cancel()
        announceTask = null
        bossBarTask?.cancel()
        bossBarTask = null
        bossBar?.removeAll()
        bossBar?.isVisible = false
        bossBar = null
    }

    override fun reload() {
        super.reload()
        stop()
        loadComponents()
        startAnnouncer()
        startBossBar()
    }

    /**
     * Toàn bộ khuyến mãi đã nạp, theo thứ tự trong file
     */
    fun getAll(): List<Component> = components

    /**
     * Lấy khuyến mãi đang hoạt động tại thời điểm chỉ định
     * Nếu có nhiều khuyến mãi cùng lúc, sẽ lấy khuyến mãi có tỉ lệ cao nhất
     *
     * @param now Thời điểm cần kiểm tra, mặc định là hiện tại
     * @return Khuyến mãi đang hoạt động, hoặc null nếu không có
     */
    fun getCurrentExtra(now: ZonedDateTime = ZonedDateTime.now()): Component? = pickCurrent(components, now)

    /**
     * Dữ liệu cần cho thông báo, gộp planned và legacy về một kiểu
     *
     * @param from Mốc bắt đầu, null với legacy
     * @param to Mốc kết thúc, null nếu không xác định
     */
    class Announcement(val name: String, val ratePercent: Int, val from: ZonedDateTime?, val to: ZonedDateTime?)

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
                from?.let { append(", từ ${it.toZoned(now).format(DISPLAY_FORMAT)}") }
                to?.let { append(", đến ${it.toZoned(now).format(DISPLAY_FORMAT)}") }
            }
            val window = window(now)
            val windowText = if (window == null) {
                "đã kết thúc"
            } else {
                val state = if (window.active) "đang chạy" else "kế tiếp"
                val end = window.to?.format(DISPLAY_FORMAT) ?: "không kết thúc"
                "$state ${window.from.format(DISPLAY_FORMAT)} -> $end"
            }
            return "$dayText, $hourText$rangeText; $windowText"
        }
    }

    class Component(val name: String, val rate: Double, val schedule: Schedule) {
        /**
         * Kiểm tra xem khuyến mãi có đang hoạt động tại thời điểm chỉ định không
         *
         * @param now Thời điểm cần kiểm tra, mặc định là hiện tại
         * @return true nếu khuyến mãi đang hoạt động
         */
        fun isActive(now: ZonedDateTime = ZonedDateTime.now()) = schedule.isActive(now)

        /**
         * Tính toán số tiền khuyến mãi dựa trên số tiền gốc
         *
         * @param baseAmount Số tiền gốc
         * @return Số tiền sau khi áp dụng khuyến mãi
         */
        fun calculateAmount(baseAmount: Int): Int {
            return baseAmount + (baseAmount * rate).toInt()
        }

        /**
         * Lấy phần trăm khuyến mãi
         *
         * @return Phần trăm khuyến mãi
         */
        fun getPercentage(): Int {
            return (rate * 100).toInt()
        }
    }

    companion object {
        private const val MINUTES_PER_DAY = 24 * 60
        private const val TIME_UNKNOWN = "không xác định"

        private val HOURS_REGEX = Regex("^(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})$")

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
         * Định dạng mốc thời gian để hiển thị (log, /khuyenmai, thông báo).
         * DateTimeFormatter là thread-safe nên dùng được từ các timer async.
         */
        val DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

        /**
         * Logic thuần của getCurrentExtra, không phụ thuộc trạng thái PlannedExtras
         */
        internal fun pickCurrent(components: List<Component>, now: ZonedDateTime): Component? =
            components.filter { it.isActive(now) }.maxByOrNull { it.rate }

        /**
         * Khuyến mãi đang được áp dụng để thông báo, cùng thứ tự ưu tiên với CardProvider:
         * planned trước, legacy (config.yml) sau. Không đụng Bukkit/DotMan.
         *
         * @return null nếu không có khuyến mãi nào đang áp dụng
         */
        internal fun resolveAnnouncement(
            components: List<Component>, legacyRate: Double, legacyUntil: Long, legacyName: String,
            now: ZonedDateTime
        ): Announcement? {
            pickCurrent(components, now)?.let { extra ->
                val window = extra.schedule.window(now) ?: return null
                return Announcement(extra.name, extra.getPercentage(), window.from, window.to)
            }
            if (legacyRate > 0 && legacyUntil > now.toInstant().toEpochMilli()) {
                return Announcement(legacyName, (legacyRate * 100).toInt(), null, legacyUntil.toZoned(now))
            }
            return null
        }

        /**
         * Thay placeholder %NAME%, %RATE%, %FROM%, %TO%, %REMAINING% cho một dòng thông báo.
         * Nối §r sau tên để màu của tên không lan sang phần còn lại của dòng.
         */
        internal fun formatLine(line: String, announcement: Announcement, now: ZonedDateTime, lang: Language): String {
            val remaining = announcement.to
                ?.let { DurationFormat.format(it.toInstant().toEpochMilli() - now.toInstant().toEpochMilli(), lang) }
                ?: TIME_UNKNOWN
            return line
                .replace("%NAME%", announcement.name.color() + "§r")
                .replace("%RATE%", announcement.ratePercent.toString())
                .replace("%FROM%", announcement.from?.format(DISPLAY_FORMAT) ?: TIME_UNKNOWN)
                .replace("%TO%", announcement.to?.format(DISPLAY_FORMAT) ?: TIME_UNKNOWN)
                .replace("%REMAINING%", remaining)
        }

        /**
         * Phần thời gian còn lại của khuyến mãi trong [0, 1]; thiếu from hoặc to -> 1.0
         */
        internal fun bossBarProgress(announcement: Announcement, now: ZonedDateTime): Double {
            val from = announcement.from ?: return 1.0
            val to = announcement.to ?: return 1.0
            val total = to.toInstant().toEpochMilli() - from.toInstant().toEpochMilli()
            if (total <= 0) {
                return 1.0
            }
            val left = to.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
            return (left.toDouble() / total).coerceIn(0.0, 1.0)
        }

        /**
         * Màu bossbar theo thời gian còn lại: > 0.5 GREEN, > 0.2 YELLOW, còn lại RED
         */
        internal fun bossBarColor(progress: Double): BarColor = when {
            progress > 0.5 -> BarColor.GREEN
            progress > 0.2 -> BarColor.YELLOW
            else -> BarColor.RED
        }

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

        private fun Long.toZoned(reference: ZonedDateTime): ZonedDateTime =
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(this), reference.zone)

        private fun LocalDate.atMinute(minute: Int, zone: ZoneId): ZonedDateTime =
            atStartOfDay(zone).plusMinutes(minute.toLong())

        private fun Int.toHourText() = "%02d:%02d".format(this / 60, this % 60)
    }
}
