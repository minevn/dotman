package net.minevn.dotman

import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.libs.get
import net.minevn.libs.parseJson
import org.bukkit.command.CommandSender

class UpdateChecker {

    companion object {
        private const val URL = "https://api.github.com/repos/MineVN/DotMan/releases/latest"
        private val plugin = DotMan.instance
        private val language = plugin.language
        private var releaseVersion : String = ""
        private val currentVersion = plugin.description.version.trim()
        private var latestVersion = currentVersion
        private var latest = false

        fun init() {
            runNotSync {
                latest = checkUpdate()
                sendUpdateMessage(plugin.server.consoleSender, true)
            }
        }

        fun sendUpdateMessage(receiver: CommandSender, notifyLatestVersion: Boolean = false) {
            if (!plugin.config.checkUpdate || !receiver.hasPermission("dotman.update")) return
            if (!latest) {
                language.updateAvailable
                    .replace("%NEW_VERSION%", latestVersion)
                    .replace("%CURRENT_VERSION%", currentVersion)
                    .let { receiver.sendMessage(it) }
                receiver.sendMessage(language.updateAvailableLink.replace("%URL%", releaseVersion))
                return
            }
            if (notifyLatestVersion) {
                receiver.sendMessage(language.updateLatest)
            }
        }

        private fun checkUpdate(): Boolean {
            try {
                get(URL).parseJson().asJsonObject.let {
                    latestVersion = it["tag_name"].asString
                    releaseVersion = it["html_url"].asString
                    return latestVersion == currentVersion
                }
            } catch (e: Exception) {
                e.warning("Không thể kiểm tra cập nhật")
                return true
            }
        }
    }
}
