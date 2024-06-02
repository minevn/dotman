package net.minevn.dotman.config

import net.minevn.libs.toJson

class Discord : FileConfig("discord") {
    class WebHook(val url: String, val content: String) {
        fun send() {
            val dto = DTO(content).toJson()
        }
    }

    data class DTO(val content: String)

    val list: List<WebHook> = (config.getList("webhooks") ?: emptyList()).map {
        it as Map<*, *>
        WebHook(it["url"] as String, it["content"] as String)
    }
}
