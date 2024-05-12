package net.minevn.dotman.config

import net.minevn.dotman.config.Milestones.Component
import net.minevn.dotman.utils.Utils.Companion.getBarColor
import net.minevn.dotman.utils.Utils.Companion.getBarStyle
import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning

class MilestonesMaster : FileConfig("mocnaptong") {
    private var components: List<Component> = emptyList()

    init {
        loadComponents()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadComponents() {
        var premiumWarning = false
        components = (config.getList("mocnaptong") ?: emptyList()).map {
            try {
                it as Map<*, *>
                val bossBar = it.getOrDefault("bossbar", null) as String?
                val from = it.getOrDefault("from", 0) as Int
                val barColor = getBarColor(it.getOrDefault("bossbar-color", null) as String?)
                val barStyle = getBarStyle(it.getOrDefault("bossbar-style", null) as String?)
                val component = Component(it["type"] as String, it["amount"] as Int, it["commands"] as List<String>,
                    bossBar, from, barColor, barStyle)

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

        info("Đã nạp ${components.size} mốc nạp tổng.")
    }

    override fun reload() {
        super.reload()
        loadComponents()
    }

    fun getAll() = components.toList()
}