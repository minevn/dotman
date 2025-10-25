package net.minevn.dotman.config

import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.libs.parseHexColorToInt
import net.minevn.libs.post
import net.minevn.libs.toJson

class Discord : FileConfig("discord") {

    /**
     * Đại diện một webhook hoàn chỉnh để gửi lên discord.
     */
    class WebHook(private val url: String, private val payload: Map<*, *>) {
        fun send(replacements: Map<String, String>) {
            val processedPayload = applyReplacements(payload, replacements) as? Map<*, *> ?: return
            val json = processedPayload.toJson()

            try {
                post(url, "application/json", json)
            } catch (e: Exception) {
                e.warning("Gửi Discord webhook thất bại")
            }
        }

        /**
         * Áp dụng replacements cho tất cả các giá trị String trong Map/List đệ quy:
         * 
         * - String: thay thế tất cả placeholder
         * - Map: xử lý từng value, color field được convert từ hex sang int
         * - List: nếu toàn String thì join thành 1 chuỗi, không thì xử lý từng phần tử
         */
        private fun applyReplacements(data: Any?, replacements: Map<String, String>): Any? {
            when (data) {
                is String -> {
                    return replaceAllPlaceholders(data, replacements)
                }
                
                is Map<*, *> -> {
                    return processMap(data, replacements)
                }
                
                is List<*> -> {
                    return processList(data, replacements)
                }
                
                else -> {
                    return data
                }
            }
        }

        /**
         * Thay thế tất cả placeholder trong một chuỗi.
         */
        private fun replaceAllPlaceholders(text: String, replacements: Map<String, String>): String {
            var result: String = text
            for ((placeholder, replaceValue) in replacements) {
                result = result.replace(placeholder, replaceValue, ignoreCase = false)
            }
            return result
        }

        /**
         * Xử lý Map: apply replacements cho từng value.
         * Đặc biệt: field "color" sẽ được convert từ hex string sang int.
         */
        private fun processMap(map: Map<*, *>, replacements: Map<String, String>): Map<*, *> {
            return map.mapValues { (key, value) ->
                if (key == "color" && value is String) {
                    parseHexColorToInt(value)
                } else {
                    applyReplacements(value, replacements)
                }
            }
        }

        /**
         * Xử lý List:
         * - Nếu list chứa toàn String -> join thành 1 chuỗi với separator \n
         * - Ngược lại -> apply replacements cho từng phần tử
         */
        private fun processList(list: List<*>, replacements: Map<String, String>): Any {
            val isAllStrings = list.all { it is String }
            if (isAllStrings) {
                val joinedText = list.filterIsInstance<String>().joinToString("\n")
                return replaceAllPlaceholders(joinedText, replacements)
            } else {
                return list.map { element ->
                    applyReplacements(element, replacements)
                }
            }
        }
    }

    /** Danh sách webhook đã bật */
    val list: List<WebHook> = (config.getList("discord-hooks") ?: emptyList()).mapNotNull { raw ->
        try {
            val map = raw as? Map<*, *> ?: throw TypeCastException("discord-hooks không hợp lệ")
            if (map["enabled"] as? Boolean != true) {
                return@mapNotNull null
            }
            val url = map["url"] as? String ?: throw TypeCastException("url không hợp lệ")
            val payload = map["payload"] as? Map<*, *> ?: throw TypeCastException("payload không hợp lệ")

            WebHook(url, payload)
        } catch (e: TypeCastException) {
            warning("Có lỗi xảy ra khi nạp webhook: ${e.message}")
            null
        }
    }

    init {
        info("Đã nạp ${list.size} webhook discord.")
    }
}
