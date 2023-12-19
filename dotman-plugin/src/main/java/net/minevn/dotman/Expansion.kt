package net.minevn.dotman

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

class Expansion : PlaceholderExpansion() {
    override fun getIdentifier() = "DotMan"

    override fun getAuthor() = "MineVN"

    override fun getVersion() = "1.0"

    override fun onPlaceholderRequest(player: Player, params: String): String {
        val args = params.lowercase().split("_").dropLastWhile { it.isEmpty() }
        return ""
    }
}
