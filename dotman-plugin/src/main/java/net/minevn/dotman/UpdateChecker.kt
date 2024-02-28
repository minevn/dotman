package net.minevn.dotman

import net.minevn.libs.gson.JsonParser
import org.bukkit.entity.Player
import net.minevn.libs.get

class UpdateChecker {
    private val plugin = DotMan.instance
    private val url = "https://api.github.com/repos/MineVN/DotMan/releases/latest"
    private var releaseVersion : String = ""
    private val currentVersion = plugin.description.version.trim()
    private var latestVersion = currentVersion
    private var latest = false

    fun init() {
        latest = checkUpdate()
        if (latest) {
            plugin.logger.info(plugin.language.updateLatest)
        } else {
            plugin.logger.info(plugin.language.updateAvailable
                .replace("%NEW_VERSION%", "")
                .replace("%CURRENT_VERSION%", currentVersion)
            )
            plugin.logger.info(plugin.language.updateAvailableLink.replace("%URL%", releaseVersion))
        }
    }

    // Check for updates when a player logs in
    fun loginCheckForUpdates(player: Player) {
        if (plugin.config.checkUpdate && !latest && player.hasPermission("dotman.update")) {
            player.sendMessage("There is a new version of DotMan available.")
            player.sendMessage("You can download it at $releaseVersion")
        }
    }

    private fun checkUpdate(): Boolean {
        val data = get(url)
        val json = JsonParser().parse(data).asJsonObject
        latestVersion = json.get("tag_name").asString
        releaseVersion = json.get("html_url").asString
        return latestVersion != currentVersion
    }
}