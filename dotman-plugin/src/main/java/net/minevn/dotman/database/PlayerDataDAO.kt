package net.minevn.dotman.database

import net.minevn.dotman.DotMan
import net.minevn.dotman.utils.TopType
import net.minevn.libs.bukkit.db.DataAccess
import org.bukkit.entity.Player

abstract class PlayerDataDAO : DataAccess() {
    companion object {
        fun getInstance() = DotMan.instance.getDAO(PlayerDataDAO::class)
    }

    abstract fun insertDataScript(): String
    abstract fun getTopScript(): String
    abstract fun getDataScript(): String

    fun insertData(player: Player, key: String, value: Int) {
        insertDataScript().statement {
            setString(1, player.uniqueId.toString())
            setString(2, player.name)
            setString(3, key)
            setInt(4, value)
            setLong(5, System.currentTimeMillis())

            executeUpdate()
        }
    }

    fun getData(player: Player, key: String) = getDataScript().statement {
        setString(1, player.uniqueId.toString())
        setString(2, key)

        fetchRecords {
            getInt("value")
        }.firstOrNull() ?: 0
    }

    fun insertAllType(player: Player, key: String, value: Int) = TopType.entries.forEach {
        insertData(player, it.parseKey(key), value)
    }

    fun getTop(key: String, limit: Int) = run {
        getTopScript().statement {
            setString(1, key)
            setInt(2, limit)

            fetchRecords {
                getString("name")!! to getInt("value")
            }
        }
    }
}