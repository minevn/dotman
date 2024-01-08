package net.minevn.dotman.database.dao

import net.minevn.dotman.database.DataAccess
import net.minevn.dotman.database.getInstance
import net.minevn.dotman.database.statement
import net.minevn.dotman.utils.TopType
import org.bukkit.entity.Player

interface PlayerDataDAO : DataAccess {
    companion object {
        fun getInstance() = PlayerDataDAO::class.getInstance()
    }

    fun insertDataScript(): String

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

    fun insertAllType(player: Player, key: String, value: Int) = TopType.entries.forEach {
        insertData(player, it.parseKey(key), value)
    }
}