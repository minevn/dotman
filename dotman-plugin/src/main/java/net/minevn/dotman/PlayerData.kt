package net.minevn.dotman

import net.minevn.dotman.database.PlayerDataDAO
import net.minevn.dotman.utils.Utils
import net.minevn.libs.bukkit.runSync
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

const val PLAYER_DATA_UPDATE_INTERVAL = 1 * 60 * 1000L // 1 phút

class PlayerData(
    val data: Map<String, Int>,
    private val initialTime: Long = System.currentTimeMillis()
) {
    fun isExpired() = System.currentTimeMillis() - initialTime > PLAYER_DATA_UPDATE_INTERVAL

    companion object {
        private val dataCache = ConcurrentHashMap<String, PlayerData>()

        private fun getFromDB(uuid: String) = PlayerData(
            PlayerDataDAO.getInstance().getAllData(uuid).associate {
                it.first to it.second
            }
        )

        operator fun get(uuid: String) = dataCache[uuid].run {
            if (this?.isExpired() != false) {
                if (Bukkit.isPrimaryThread()) {
                    Utils.runAsync { getFromDB(uuid).also { runSync { dataCache[uuid] = it } } }
                    this ?: PlayerData(emptyMap(), 0L)
                } else {
                    getFromDB(uuid).also { dataCache[uuid] = it }
                }
            } else this
        }
    }
}
