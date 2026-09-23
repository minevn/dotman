package net.minevn.dotman.extras

import net.minevn.dotman.DotMan
import net.minevn.dotman.config.Language
import net.minevn.dotman.config.PlannedExtrasConfig
import net.minevn.dotman.utils.BukkitBossBar
import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.runAsyncTimer
import net.minevn.dotman.utils.Utils.Companion.runSync
import net.minevn.dotman.utils.Utils.Companion.warning
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.time.ZonedDateTime

/**
 * Thông báo khuyến mãi ra chat và bossbar theo section thong-bao của khuyenmai.yml.
 * Một vòng lặp tick mỗi giây theo dõi khuyến mãi đang áp dụng (cùng logic ưu tiên với CardProvider:
 * planned trước, legacy config.yml sau). Khi khuyến mãi bắt đầu/kết thúc: gửi message.active/message.ended
 * ngay lập tức (không chờ chu kỳ) và render lại bossbar ngay; chu kỳ lặp lại message.active được tính lại
 * từ lúc gửi gần nhất, dù gửi do bắt đầu hay do đến chu kỳ.
 * Một instance sống cùng một PlannedExtrasConfig: DotMan tạo sau khi nạp PlannedExtrasConfig và gọi stop() khi reload/disable.
 */
class ExtraAnnouncer(private val extras: PlannedExtrasConfig) {
    private val main = DotMan.instance
    private var tickTask: BukkitTask? = null
    private var bossBar: BukkitBossBar? = null

    fun start() {
        val config = extras.config
        val chatEnabled = config.getBoolean("thong-bao.chat.enabled", true)
        val activeMessage = extras.getList("thong-bao.chat.message.active")
        val endedMessage = extras.getList("thong-bao.chat.message.ended")
        val interval = config.getInt("thong-bao.chat.interval", 300).coerceAtLeast(0)
        if (chatEnabled && activeMessage.isEmpty()) {
            warning("thong-bao.chat.message.active trống, không gửi thông báo khuyến mãi")
        }
        val chatReady = chatEnabled && activeMessage.isNotEmpty()

        val bossBarEnabled = config.getBoolean("thong-bao.bossbar.enabled", false)
        val titles = extras.getList("thong-bao.bossbar.titles")
        val rotate = config.getInt("thong-bao.bossbar.rotate", 5).coerceAtLeast(1)
        if (bossBarEnabled && titles.isEmpty()) {
            warning("thong-bao.bossbar.titles trống, không hiển thị bossbar")
        }
        val bar = if (bossBarEnabled && titles.isNotEmpty()) newBossBar(config) else null
        bossBar = bar

        if (!chatReady && bar == null) {
            return
        }
        if (chatReady) {
            val repeatNote = if (interval > 0) ", lặp lại mỗi $interval giây khi đang áp dụng" else ""
            info("Thông báo khuyến mãi khi bắt đầu/kết thúc$repeatNote")
        }
        if (bar != null) {
            info("Bossbar khuyến mãi: ${titles.size} tiêu đề, đổi mỗi $rotate giây")
        }

        var lastAnnouncement: Announcement? = null
        var lastPlanned: PlannedExtra? = null
        var secondsSinceAnnounce = 0
        var secondsSinceRotate = 0
        var titleIndex = 0

        // Tick mỗi giây: phát hiện khuyến mãi bắt đầu/kết thúc để gửi ngay & render lại bossbar ngay,
        // đồng thời tự đếm chu kỳ lặp lại message.active và chu kỳ đổi tiêu đề bossbar.
        tickTask = runAsyncTimer(0, 20L) {
            val now = ZonedDateTime.now()
            val currentPlanned = PlannedExtrasConfig.pickCurrent(extras.getAll(), now)
            val current = currentAnnouncement(now)

            if (current?.name != lastAnnouncement?.name) {
                // Khuyến mãi trước vẫn còn hiệu lực (chỉ bị ghi đè bởi khuyến mãi tỉ lệ cao hơn) thì
                // không phải là "kết thúc", chỉ đổi khuyến mãi đang được áp dụng
                val previousPlanned = lastPlanned
                val previousStillActive = when {
                    previousPlanned != null -> previousPlanned.isActive(now)
                    lastAnnouncement != null ->
                        main.config.extraRate > 0 && main.config.extraUntil > now.toInstant().toEpochMilli()
                    else -> false
                }
                if (chatReady) {
                    if (!previousStillActive) {
                        lastAnnouncement?.let { broadcast(endedMessage, it, now) }
                    }
                    current?.let { broadcast(activeMessage, it, now) }
                }
                lastAnnouncement = current
                lastPlanned = currentPlanned
                secondsSinceAnnounce = 0
                secondsSinceRotate = 0
                titleIndex = 0
            } else if (chatReady && current != null && interval > 0) {
                secondsSinceAnnounce++
                if (secondsSinceAnnounce >= interval) {
                    broadcast(activeMessage, current, now)
                    secondsSinceAnnounce = 0
                }
            }

            if (bar == null) {
                return@runAsyncTimer
            }
            if (current == null) {
                if (bar.isVisible) {
                    bar.removeAll()
                    bar.isVisible = false
                }
                return@runAsyncTimer
            }
            secondsSinceRotate++
            if (secondsSinceRotate >= rotate) {
                secondsSinceRotate = 0
                titleIndex++
            }
            bar.setTitle(formatLine(titles[titleIndex % titles.size], current, now, main.language))
            bar.progress = bossBarProgress(current, now)
            bar.color = bossBarColor(bar.progress)
            if (!bar.isVisible) {
                bar.isVisible = true
                runSync { Bukkit.getOnlinePlayers().forEach { bar.addPlayer(it) } }
            }
        }
    }

    private fun newBossBar(config: YamlConfiguration): BukkitBossBar? {
        val style = config.getString("thong-bao.bossbar.style", "SEGMENTED_10")!!
        return try {
            BukkitBossBar("§r", "GREEN", style).apply { isVisible = false }
        } catch (e: IllegalArgumentException) {
            e.warning("thong-bao.bossbar.style không hợp lệ, không hiển thị bossbar")
            null
        }
    }

    private fun broadcast(message: List<String>, announcement: Announcement, now: ZonedDateTime) {
        val lines = message.map { formatLine(it, announcement, now, main.language) }
        runSync {
            Bukkit.getOnlinePlayers().forEach { player -> lines.forEach { player.sendMessage(it) } }
        }
    }

    /**
     * Đọc config hiện tại rồi giao cho resolveAnnouncement
     */
    private fun currentAnnouncement(now: ZonedDateTime): Announcement? {
        val cfg = main.config
        return resolveAnnouncement(extras.getAll(), cfg.extraRate, cfg.extraUntil, extras.legacyName, now)
    }

    /**
     * Add người chơi mới vào bossbar nếu đang hiện; gọi từ DotManListener.onJoin
     */
    fun onJoin(player: Player) {
        bossBar?.takeIf { it.isVisible }?.addPlayer(player)
    }

    /**
     * Hủy timer và bossbar; gọi trước khi tạo PlannedExtrasConfig mới hoặc khi disable plugin
     */
    fun stop() {
        tickTask?.cancel()
        tickTask = null
        bossBar?.removeAll()
        bossBar?.isVisible = false
        bossBar = null
    }

    /**
     * Dữ liệu cần cho thông báo, gộp planned và legacy về một kiểu
     *
     * @param from Mốc bắt đầu, null với legacy
     * @param to Mốc kết thúc, null nếu không xác định
     */
    class Announcement(val name: String, val ratePercent: Int, val from: ZonedDateTime?, val to: ZonedDateTime?)

    companion object {
        /**
         * Khuyến mãi đang được áp dụng để thông báo, cùng thứ tự ưu tiên với CardProvider:
         * planned trước, legacy (config.yml) sau. Không đụng Bukkit/DotMan.
         *
         * @return null nếu không có khuyến mãi nào đang áp dụng
         */
        internal fun resolveAnnouncement(
            components: List<PlannedExtra>, legacyRate: Double, legacyUntil: Long, legacyName: String,
            now: ZonedDateTime
        ): Announcement? {
            PlannedExtrasConfig.pickCurrent(components, now)?.let { extra ->
                val window = extra.schedule.window(now) ?: return null
                return Announcement(extra.name, extra.getPercentage(), window.from, window.to)
            }
            if (legacyRate > 0 && legacyUntil > now.toInstant().toEpochMilli()) {
                return Announcement(legacyName, (legacyRate * 100).toInt(), null, legacyUntil.toZoned(now))
            }
            return null
        }

        /**
         * Thay placeholder %NAME%, %RATE%, %FROM%, %TO%, %REMAINING% cho một dòng thông báo
         */
        internal fun formatLine(line: String, announcement: Announcement, now: ZonedDateTime, lang: Language): String {
            return ExtraFormat
                .replacePlaceholders(line, announcement.name, announcement.ratePercent, announcement.from, announcement.to, lang)
                .replace("%REMAINING%", ExtraFormat.formatRemaining(announcement.to, now, lang))
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
    }
}
