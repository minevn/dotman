package net.minevn.dotman.discord

import net.minevn.libs.post
import net.minevn.libs.toJson
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.dotman.utils.applyReplacements

class Webhook(private val url: String, private val payload: Map<*, *>) {
    fun send(replacements: Map<String, String>) {
        val processedPayload = applyReplacements(payload, replacements) as? Map<*, *> ?: return
        val json = processedPayload.withDiscordIdentity().toJson()
        try {
            post(url, "application/json", json)
        } catch (e: Exception) {
            e.warning("Gửi Discord webhook thất bại")
        }
    }

    private fun Map<*, *>.withDiscordIdentity(): Map<*, *> {
        val username = this["username"] as? String
        val avatarUrl = this["avatar_url"] as? String
        if (!username.isNullOrBlank() && !avatarUrl.isNullOrBlank()) {
            return this
        }

        return this.filterKeys { it != "username" && it != "avatar_url" }
    }
}
