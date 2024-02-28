package net.minevn.dotman

import net.minevn.libs.gson.JsonParser
import org.bukkit.entity.Player
import net.minevn.libs.get

class UpdateChecker {
    private val plugin = DotMan.instance
    private val url = "https://api.github.com/repos/MineVN/DotMan/releases/latest"
    private var releaseVersion : String = ""
    private val currentVersion = plugin.description.version.trim()
    private var latest = false

    fun init() {
        latest = checkUpdate()
        if (latest) {
            plugin.logger.info("You are using the latest version of DotMan.")
        } else {
            plugin.logger.info("There is a new version of DotMan available.")
            plugin.logger.info("You can download it at $releaseVersion")
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
        val latestVersion = json.get("tag_name").asString
        releaseVersion = json.get("html_url").asString
        return latestVersion != currentVersion
    }
}