package net.minevn.dotman.config

import net.minevn.dotman.utils.Utils.Companion.warning

class Milestone : FileConfig("milestone") {

    private var components: List<Component> = emptyList()

    init {
        loadComponents()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadComponents() {
        var premiumWarning = false
        components = config.getList("").map {
            it as Map<*, *>
            val component = Component(it["type"] as String, it["amount"] as Int, it["commands"] as List<String>)
            if (component.type != "all" && !premiumWarning) {
                premiumWarning = true
                warning("Tính năng mốc nạp chỉ có ở phiên bản DotMan premium. Hãy mua plugin để ủng hộ author nhé!")
            }
            component
        }
    }

    override fun reload() {
        super.reload()
        loadComponents()
    }

    fun getAll() = components.toList()

    class Component(val type: String, val amount: Int, val commands: List<String>)

}