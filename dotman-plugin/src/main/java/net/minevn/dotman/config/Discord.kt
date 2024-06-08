package net.minevn.dotman.config

import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.libs.post
import net.minevn.libs.toJson

class Discord : FileConfig("discord") {
    class WebHook(val url: String, val contentTmpl: String) {
        fun send(content: String) {
            val dto = DTO(content).toJson()
            runCatching { post(url, "application/json", dto) }
                .onFailure { it.warning("Discord webhook failed") }
        }
    }

    data class DTO(val content: String)

    val list: List<WebHook> = (config.getList("discord-hooks") ?: emptyList()).map {
        it as Map<*, *>
        if (it["enabled"] != true) return@map null
        WebHook(it["url"] as String, it["content"] as String)
    }.filterNotNull()

    init {
        info("Đã nạp ${list.size} webhook discord.")
    }
}
