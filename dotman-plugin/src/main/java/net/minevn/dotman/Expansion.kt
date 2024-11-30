package net.minevn.dotman

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.minevn.dotman.utils.Utils.Companion.format
import org.bukkit.entity.Player

class Expansion : PlaceholderExpansion() {
    init {
        register()
    }

    override fun getIdentifier() = "DotMan"

    override fun getAuthor() = "MineVN"

    override fun getVersion() = "1.0"

    @Suppress("NAME_SHADOWING")
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        val params = params.lowercase()
        val args = params.lowercase().split("_").dropLastWhile { it.isEmpty() }

        if (params.startsWith("top_") && args.size >= 4) run top@{
            val last2 = args.takeLast(2)
            val key = args.drop(1).dropLast(2).joinToString("_").uppercase()
            val rank = last2[0].toIntOrNull() ?: return@top
            val type = last2[1].takeIf { it in listOf("player", "value") } ?: return@top
            val isPlayer = type == "player"
            val top = LeaderBoard["${key}_ALL"]
            val target = top[rank] ?: return if (isPlayer) "Chưa xếp hạng" else "0"
            return if (isPlayer) target.first.format() else target.second.format()
        }
        if (params.startsWith("data_") && args.size >= 2 && player != null) run data@{
            val key = args.drop(1).joinToString("_").uppercase()
            val playerData = PlayerData[player]
            return playerData.data["${key}_ALL"]?.format() ?: "0"
        }

        return null
    }
}
