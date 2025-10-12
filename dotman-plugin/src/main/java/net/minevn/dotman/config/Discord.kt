package net.minevn.dotman.config

import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.libs.post
import net.minevn.libs.toJson

class Discord : FileConfig("discord") {

    /**
     * Payload gửi lên Discord webhook.
     */
    data class Payload(
        val content: String? = null,
        val username: String? = null,
        val avatarUrl: String? = null,
        val embeds: List<Embed>? = null
    )

    /**
     * Định nghĩa một Embed chuẩn của Discord.
     */
    data class Embed(
        val title: String? = null,
        val description: String? = null,
        val timestamp: String? = null,
        val color: Int? = null,
        val footer: Footer? = null,
        val image: Image? = null,
        val thumbnail: Thumbnail? = null,
        val author: Author? = null,
        val fields: List<Field>? = null
    )

    /** Author của embed */
    data class Author(
        val name: String? = null,
        val url: String? = null,
        val iconUrl: String? = null
    )

    /** Footer của embed */
    data class Footer(
        val text: String? = null,
        val iconUrl: String? = null
    )

    /** Ảnh cỡ lớn */
    data class Image(val url: String? = null)
    /** Ảnh thumbnail nhỏ */
    data class Thumbnail(val url: String? = null)
    /** Field của embed */
    data class Field(val name: String? = null, val value: String? = null, val inline: Boolean? = null)

    /**
     * Đại diện một webhook hoàn chỉnh để gửi lên discord.
     */
    inner class WebHook(private val url: String, private val payloadTmpl: Payload) {
        fun send(replacements: Map<String, String>) {
            val applied = this@Discord.run { payloadTmpl.applyReplacements(replacements) }
            val json = applied.toDiscordMap().toJson()
            runCatching { post(url, "application/json", json) }
                .onFailure { it.warning("Discord webhook failed") }
        }
    }

    /** Danh sách webhook đã bật */
    val list: List<WebHook> = (config.getList("discord-hooks") ?: emptyList()).mapNotNull { raw ->
        val map = raw as? Map<*, *> ?: return@mapNotNull null
        val enabled = map["enabled"] as? Boolean ?: true
        if (!enabled) return@mapNotNull null
        val url = map["url"] as? String ?: return@mapNotNull null
        val payloadMap = map["payload"] as? Map<*, *> ?: return@mapNotNull null
        WebHook(url, mapToPayload(payloadMap))
    }

    // region mapping helpers

    /** Map YAML sang Payload. Chấp nhận content dạng String hoặc List<String>. */
    private fun mapToPayload(map: Map<*, *>): Payload = Payload(
        content = asStringOrJoin(map["content"]),
        username = map["username"] as? String,
        avatarUrl = map["avatarUrl"] as? String,
        embeds = (map["embeds"] as? List<*>)?.mapNotNull { e -> (e as? Map<*, *>)?.let { mapToEmbed(it) } }
    )

    /**
     * Map YAML -> Embed. Hỗ trợ description là String hoặc List<String>.
     */
    private fun mapToEmbed(map: Map<*, *>): Embed = Embed(
        title = map["title"] as? String,
        description = asStringOrJoin(map["description"]),
        timestamp = map["timestamp"] as? String,
        color = parseColor(map["color"]),
        footer = (map["footer"] as? Map<*, *>)?.let {
            Footer(text = it["text"] as? String, iconUrl = it["iconUrl"] as? String)
        },
        image = (map["image"] as? Map<*, *>)?.let { Image(url = it["url"] as? String) },
        thumbnail = (map["thumbnail"] as? Map<*, *>)?.let { Thumbnail(url = it["url"] as? String) },
        author = (map["author"] as? Map<*, *>)?.let {
            Author(name = it["name"] as? String, url = it["url"] as? String, iconUrl = it["iconUrl"] as? String)
        },
        fields = (map["fields"] as? List<*>)?.mapNotNull { f ->
            (f as? Map<*, *>)?.let {
                Field(
                    name = it["name"] as? String,
                    value = it["value"] as? String,
                    inline = when (val b = it["inline"]) {
                        is Boolean -> b
                        is String -> b.equals("true", true)
                        else -> null
                    }
                )
            }
        }
    )

    /** Trả về chuỗi từ String hoặc List<String> (nối bằng \n). */
    private fun asStringOrJoin(value: Any?): String? = when (value) {
        is String -> value
        is List<*> -> value.filterIsInstance<String>().joinToString("\n").takeIf { it.isNotEmpty() }
        else -> null
    }

    /** Replace placeholder */
    private fun String.applyReplacements(repl: Map<String, String>): String {
        var s = this
        for ((k, v) in repl) s = s.replace(k, v)
        return s
    }

    /** Replace placeholder cho payload và các embed con */
    private fun Payload.applyReplacements(repl: Map<String, String>): Payload = this.copy(
        content = content?.applyReplacements(repl),
        username = username?.applyReplacements(repl),
        avatarUrl = avatarUrl?.applyReplacements(repl),
        embeds = embeds?.map { it.applyReplacements(repl) }
    )

    /** Replace placeholder cho embed và các field */
    private fun Embed.applyReplacements(repl: Map<String, String>): Embed = this.copy(
        title = title?.applyReplacements(repl),
        description = description?.applyReplacements(repl),
        timestamp = timestamp?.applyReplacements(repl),
        footer = footer?.let { Footer(it.text?.applyReplacements(repl),
            it.iconUrl?.applyReplacements(repl)) },
        image = image?.let { Image(it.url?.applyReplacements(repl)) },
        thumbnail = thumbnail?.let { Thumbnail(it.url?.applyReplacements(repl)) },
        author = author?.let { Author(it.name?.applyReplacements(repl),
            it.url?.applyReplacements(repl),
            it.iconUrl?.applyReplacements(repl)) },
        fields = fields?.map { Field(it.name?.applyReplacements(repl),
            it.value?.applyReplacements(repl),
            it.inline) }
    )

    /**
     * Convert màu từ mã hex trong config:
     * - Chấp nhận dạng "#RRGGBB" hoặc "RRGGBB" (không phân biệt hoa thường)
     * - Nếu sai định dạng -> trả về 0xFFFFFF (trắng)
     * - Nếu không có trường color -> trả về null (không set màu)
     */
    private fun parseColor(value: Any?): Int? {
        if (value == null) return null
        val raw = (value as? String)?.trim() ?: run {
            warning("discord.yml: 'color' phải là chuỗi hex dạng #RRGGBB, plugin sẽ dùng mặc định #FFFFFF")
            return 0xFFFFFF
        }
        val match = Regex("^#?([0-9a-fA-F]{6})$").matchEntire(raw)
        val hex = match?.groupValues?.get(1)
        return if (hex != null) {
            try {
                hex.toInt(16)
            } catch (_: Exception) {
                warning("discord.yml: 'color' không hợp lệ, plugin sẽ dùng mặc định #FFFFFF")
                0xFFFFFF
            }
        } else {
            warning("discord.yml: 'color' phải là chuỗi hex dạng #RRGGBB, plugin sẽ dùng mặc định #FFFFFF")
            0xFFFFFF
        }
    }

    /**
     * Chuyển Payload sang Map có key theo chuẩn Discord (snake_case).
     */
    private fun Payload.toDiscordMap(): Map<String, Any?> = buildMap {
        put("content", content)
        put("username", username)
        put("avatar_url", avatarUrl)
        put("embeds", embeds?.map { it.toDiscordMap() })
    }

    /** Chuyển Embed sang Map theo chuẩn Discord (snake_case). */
    private fun Embed.toDiscordMap(): Map<String, Any?> = buildMap {
        put("title", title)
        put("description", description)
        put("timestamp", timestamp)
        put("color", color)
        footer?.let { put("footer", buildMap {
            put("text", it.text)
            put("icon_url", it.iconUrl)
        }) }
        image?.let { put("image", buildMap { put("url", it.url) }) }
        thumbnail?.let { put("thumbnail", buildMap { put("url", it.url) }) }
        author?.let { put("author", buildMap {
            put("name", it.name)
            put("url", it.url)
            put("icon_url", it.iconUrl)
        }) }
        fields?.let { list ->
            put("fields", list.map { f -> buildMap {
                put("name", f.name)
                put("value", f.value)
                if (f.inline != null) put("inline", f.inline)
            } })
        }
    }
    // endregion

    init {
        info("Đã nạp ${list.size} webhook discord.")
    }
}
