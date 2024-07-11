package net.minevn.dotman

import net.minevn.dotman.database.PlayerInfoDAO
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

class DotManListener : Listener {
    private fun updateUUID(player: Player) {
        val uuid = player.uniqueId.toString()
        val name = player.name
        runNotSync { PlayerInfoDAO.getInstance().updateData(uuid, name) }
    }

    @EventHandler
    fun onLogin(e: PlayerLoginEvent) = updateUUID(e.player)

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) = updateUUID(e.player)

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val player = e.player

        DotMan.instance.milestonesMaster.onJoin(player)
        UpdateChecker.sendUpdateMessage(player)
    }
}