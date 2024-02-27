package net.minevn.dotman

import net.minevn.libs.gson.JsonParser
import org.bukkit.entity.Player
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        val inputStream = connection.inputStream
        val reader = BufferedReader(InputStreamReader(inputStream))
        val data = reader.readText()
        val json = JsonParser().parse(data).asJsonObject
        val latestVersion = json.get("tag_name").asString
        releaseVersion = json.get("html_url").asString
        return latestVersion != currentVersion
    }
}