package net.minevn.dotman.extras

import net.minevn.dotman.DotMan
import net.minevn.dotman.config.Language
import net.minevn.dotman.config.PlannedExtras
import net.minevn.dotman.utils.BukkitBossBar
import net.minevn.dotman.utils.DurationFormat
import net.minevn.dotman.utils.Utils.Companion.color
import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.runAsyncTimer
import net.minevn.dotman.utils.Utils.Companion.runSync
import net.minevn.dotman.utils.Utils.Companion.warning
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.time.ZonedDateTime

/**
 * Thông báo khuyến mãi định kỳ ra chat và bossbar theo section thong-bao của khuyenmai.yml.
 * Một instance sống cùng một PlannedExtras: DotMan tạo sau khi nạp PlannedExtras và gọi stop() khi reload/disable.
 */
class ExtraAnnouncer(private val extras: PlannedExtras) {
    private val main = DotMan.instance
    private var announceTask: BukkitTask? = null
    private var bossBar: BukkitBossBar? = null
    private var bossBarTask: BukkitTask? = null

    fun start() {
        startAnnouncer()
        startBossBar()
    }

    /**
     * Tạo timer gửi thông báo khuyến mãi định kỳ ra chat theo section thong-bao
     */
    private fun startAnnouncer() {
        val config = extras.config
        val enabled = config.getBoolean("thong-bao.enabled", true)
        val interval = config.getInt("thong-bao.interval", 300)
        if (!enabled || interval <= 0) {
            return
        }
        val message = extras.getList("thong-bao.message")
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
        val config = extras.config
        if (!config.getBoolean("thong-bao.bossbar.enabled", false)) {
            return
        }
        val titles = extras.getList("thong-bao.bossbar.titles")
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
        return resolveAnnouncement(extras.getAll(), cfg.extraRate, cfg.extraUntil, extras.legacyName, now)
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
            PlannedExtras.pickCurrent(components, now)?.let { extra ->
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
                ?: lang.khuyenmaiTimeUnknown
            return line
                .replace("%NAME%", announcement.name.color() + "§r")
                .replace("%RATE%", announcement.ratePercent.toString())
                .replace("%FROM%", announcement.from?.format(PlannedExtras.DISPLAY_FORMAT) ?: lang.khuyenmaiTimeUnknown)
                .replace("%TO%", announcement.to?.format(PlannedExtras.DISPLAY_FORMAT) ?: lang.khuyenmaiTimeUnknown)
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
    }
}
