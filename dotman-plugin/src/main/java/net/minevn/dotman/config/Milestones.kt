package net.minevn.dotman.config

import net.minevn.dotman.DotMan
import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.libs.bukkit.FileConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class Milestones : FileConfig(DotMan.instance, "mocnap") {

    private var components: List<Component> = emptyList()

    init {
        loadComponents()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadComponents() {
        var premiumWarning = false
        components = config.getList("mocnap").map {
            try {
                it as Map<*, *>
                val component = Component(it["type"] as String, it["amount"] as Int, it["commands"] as List<String>)

                if (component.type !in listOf("all", "week", "month")) {
                    warning("Loại mốc nạp \"${component.type}\" không hợp lệ. Chỉ chấp nhận all, week, month")
                    return@map null
                }
                if (component.type != "all") {
                    if (!premiumWarning) {
                        premiumWarning = true
                        warning("Tính năng mốc nạp theo tuần, tháng chỉ có ở phiên bản DotMan premium. " +
                                "Hãy mua plugin để ủng hộ author nhé!")
                    }
                    return@map null
                }

                component
            } catch (e: Exception) {
                e.warning("Có một mốc nạp không hợp lệ, hãy liên hệ developer để được hỗ trợ")
                null
            }
        }.filterNotNull()

        info("Đã nạp ${components.size} mốc nạp")
    }

    override fun reload() {
        super.reload()
        loadComponents()
    }

    fun getAll() = components.toList()

    class Component(val type: String, val amount: Int, val commands: List<String>) {

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
         * @param current số tiền sau khi nạp
         * @param amount số tiền nạp
         */
        fun check(player: Player, current: Int, amount: Int) {
            if (current - amount < this.amount) {
                info("${player.name} đã đạt mốc nạp $amount ($typeName)")
                commands.forEach {
                    val command = it.replace("%player%", player.name)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.format(player.name))
                }
            }
        }
    }
}
