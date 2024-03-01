package net.minevn.dotman

import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.libs.bukkit.parseJson
import org.bukkit.entity.Player
import net.minevn.libs.get

class UpdateChecker {
    companion object {
        private val plugin = DotMan.instance
        private val url = "https://api.github.com/repos/MineVN/DotMan/releases/latest"
        private var releaseVersion : String = ""
        private val currentVersion = plugin.description.version.trim()
        private var latestVersion = currentVersion
        private var latest = false

        fun init() {
            latest = checkUpdate()
            val language = plugin.language
            if (latest) {
                info(plugin.language.updateLatest)
            } else {
                language.updateAvailable
                    .replace("%NEW_VERSION%", "")
                    .replace("%CURRENT_VERSION%", currentVersion)
                    .let { info(it) }
                info(plugin.language.updateAvailableLink.replace("%URL%", releaseVersion))
            }
        }

        // Check for updates when a player logs in
        fun loginCheckForUpdates(player: Player) {
            if (plugin.config.checkUpdate && !latest && player.hasPermission("dotman.update")) {
                val language = plugin.language
                language.updateAvailable
                    .replace("%NEW_VERSION%", latestVersion)
                    .replace("%CURRENT_VERSION%", currentVersion)
                    .let { player.sendMessage(it) }
                player.sendMessage(language.updateAvailableLink.replace("%URL%", releaseVersion))
            }
        }

        private fun checkUpdate(): Boolean {
            try {
                get(url).parseJson().asJsonObject.let { json ->
                    latestVersion = json.get("tag_name").asString
                    releaseVersion = json.get("html_url").asString
                    return latestVersion != currentVersion
                }
            } catch (e: Exception) {
                e.warning("Could not check for updates")
                return true
            }
        }
    }
}
