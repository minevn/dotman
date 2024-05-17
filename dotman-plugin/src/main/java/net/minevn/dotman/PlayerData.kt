package net.minevn.dotman

import net.minevn.dotman.database.PlayerDataDAO
import net.minevn.dotman.utils.Utils
import net.minevn.libs.bukkit.runSync
import org.bukkit.Bukkit
import org.bukkit.entity.Player

const val PLAYER_DATA_UPDATE_INTERVAL = 1 * 60 * 1000L // 1 phút

class PlayerData(
    val data: Map<String, Int>,
    private val initialTime: Long = System.currentTimeMillis()
) {
    fun isExpired() = System.currentTimeMillis() - initialTime > PLAYER_DATA_UPDATE_INTERVAL

    companion object {
        private val dataCache = mutableMapOf<Player, PlayerData>()

        private fun getFromDB(player: Player) = PlayerData(
            PlayerDataDAO.getInstance().getAllData(player.uniqueId.toString())
        )

        operator fun get(player: Player) = dataCache[player].run {
            if (this?.isExpired() != false) {
                if (Bukkit.isPrimaryThread()) {
                    Utils.runAsync { getFromDB(player).also { runSync { dataCache[player] = it } } }
                    this ?: PlayerData(emptyMap(), 0L)
                } else {
                    getFromDB(player).also { runSync { dataCache[player] = it } }
                }
            } else this
        }
    }
}
