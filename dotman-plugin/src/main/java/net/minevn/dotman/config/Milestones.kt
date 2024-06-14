package net.minevn.dotman.config

import net.minevn.dotman.DotMan
import net.minevn.dotman.TOP_KEY_DONATE_TOTAL
import net.minevn.dotman.database.PlayerDataDAO
import net.minevn.dotman.utils.BukkitBossBar
import net.minevn.dotman.utils.Utils.Companion.format
import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.libs.bukkit.runSync
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

class Milestones : FileConfig("mocnap") {

    private var components: List<Component> = emptyList()

    init {
        loadComponents()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadComponents() {
        var premiumWarning = false
        components = (config.getList("mocnap") ?: emptyList()).map {
            try {
                it as Map<*, *>
                val type = it["type"]

                if (type !in listOf("all", "week", "month")) {
                    warning("Loại mốc nạp \"$type\" không hợp lệ. Chỉ chấp nhận all, week, month")
                    return@map null
                }
                if (type != "all") {
                    if (!premiumWarning) {
                        premiumWarning = true
                        warning("Tính năng mốc nạp theo tuần, tháng chỉ có ở phiên bản DotMan premium. " +
                                "Hãy mua plugin để ủng hộ author nhé!")
                    }
                    return@map null
                }

                Component(it["type"] as String, it["amount"] as Int, it["commands"] as List<String>)
            } catch (e: Exception) {
                e.warning("Có một mốc nạp không hợp lệ, hãy liên hệ developer để được hỗ trợ")
                null
            }
        }.filterNotNull()

        info("Đã nạp ${components.size} mốc nạp.")
    }

    override fun reload() {
        super.reload()
        loadComponents()
    }

    fun getAll() = components.toList()

    class Component(val type: String, val amount: Int, val commands: List<String>, val bossBar: String? = null,
                    val from: Int = 0, barColor: String = "GREEN", barStyle: String = "SEGMENTED_10") {

        var bar: BukkitBossBar? = null
        var barTask: BukkitTask? = null; private set

        init {
            if (bossBar != null) {
                bar = BukkitBossBar("§r", barColor, barStyle).apply {
                    isVisible = false
                    barTask = Bukkit.getScheduler().runTaskTimerAsynchronously(DotMan.instance, Runnable {
                        val current = PlayerDataDAO.getInstance().getSumData("${TOP_KEY_DONATE_TOTAL}_$type")
                        if (current in from until amount) {
                            if (!isVisible) {
                                isVisible = true
                                runSync { Bukkit.getOnlinePlayers().forEach {addPlayer(it)} }
                            }
                            val title = bossBar
                                .replace("%CURRENT%", current.format())
                                .replace("%TARGET%", amount.format())
                            progress = current.toDouble() / amount
                            setTitle(title)
                        } else {
                            if (isVisible) {
                                removeAll()
                                isVisible = false
                            }
                        }
                    }, 0, 20 * 5)
                }
            }
        }

        private val typeName = when (type) {
            "all" -> "toàn thời gian"
            "week" -> "tuần"
            "month" -> "tháng"
            else -> throw IllegalArgumentException()
        }

        /**
         * Kiểm tra xem người chơi đã đạt mốc nạp này chưa,
         * nếu đạt thì thực hiện các lệnh trong config
         *
         * @param player người chơi
         * @param current số tiền sau khi nạp
         * @param amount số tiền nạp
         */
        fun check(player: Player, current: Int, amount: Int) {
            if (current >= this.amount && current - amount < this.amount) {
                info("${player.name} đã đạt mốc nạp $amount ($typeName)")
                runSync {
                    commands.forEach {
                        val command = it.replace("%player%", player.name)
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                    }
                }
            }
        }

        /**
         * Kiểm tra xem toàn server đã đạt mốc nạp này chưa,
         * nếu đạt thì thực hiện các lệnh trong config
         *
         * @param current số tiền sau khi nạp
         * @param amount số tiền nạp
         */
        fun sumCheck(current: Int, amount: Int) {
            if (current >= this.amount && current - amount < this.amount) {
                runSync {
                    commands.forEach {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it)
                    }
                    bar?.removeAll()
                    bar?.isVisible = false
                    bar = null
                }
            }
        }
    }
}
