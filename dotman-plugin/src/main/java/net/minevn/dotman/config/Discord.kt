package net.minevn.dotman.config

import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.dotman.discord.Webhook

class Discord : FileConfig("discord") {
    val webhooks: List<Webhook> = (config.getList("discord-hooks") ?: emptyList()).mapNotNull { raw ->
        try {
            val map = raw as? Map<*, *> ?: throw TypeCastException("discord-hooks không hợp lệ")
            if (map["enabled"] as? Boolean != true) {
                return@mapNotNull null
            }
            val url = map["url"] as? String ?: throw TypeCastException("url không hợp lệ")
            val payload = map["payload"] as? Map<*, *> ?: throw TypeCastException("payload không hợp lệ")
            Webhook(url, payload)
        } catch (e: TypeCastException) {
            warning("Có lỗi xảy ra khi nạp webhook: ${e.message}")
            null
        }
    }

    init {
        info("Đã nạp ${webhooks.size} webhook discord.")
    }
}
