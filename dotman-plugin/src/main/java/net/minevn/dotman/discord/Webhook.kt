package net.minevn.dotman.discord

import net.minevn.libs.post
import net.minevn.libs.toJson
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.dotman.utils.applyReplacements

class Webhook(private val url: String, private val payload: Map<*, *>) {
    fun send(replacements: Map<String, String>) {
        val processedPayload = applyReplacements(payload, replacements) as? Map<*, *> ?: return
        val json = processedPayload.toJson()
        try {
            post(url, "application/json", json)
        } catch (e: Exception) {
            e.warning("Gửi Discord webhook thất bại")
        }
    }
}
