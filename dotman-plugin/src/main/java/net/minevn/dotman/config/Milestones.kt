package net.minevn.dotman.config

import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning

class Milestones : FileConfig("mocnap") {

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

    class Component(val type: String, val amount: Int, val commands: List<String>)

}