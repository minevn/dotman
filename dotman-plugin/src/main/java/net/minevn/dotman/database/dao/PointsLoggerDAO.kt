package net.minevn.dotman.database.dao

import net.minevn.dotman.config.MainConfig
import net.minevn.dotman.database.DataAccess
import net.minevn.dotman.database.getInstance
import net.minevn.dotman.database.statement
import org.bukkit.entity.Player

interface PointsLoggerDAO : DataAccess {
	companion object {
		fun getInstance() = PointsLoggerDAO::class.getInstance()
	}

	fun insertLogScript(): String

	fun insertLog(player: Player, amount: Int, pointFrom: Int, pointTo: Int, content: String? = null) {
		insertLogScript().statement {
			setString(1, player.name)
			setString(2, player.uniqueId.toString())
			setInt(3, amount)
			setInt(4, pointFrom)
			setInt(5, pointTo)
			setLong(6, System.currentTimeMillis())
			setString(7, MainConfig.get().server)
			setString(8, content)

			executeUpdate()
		}
	}
}