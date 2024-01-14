package net.minevn.dotman

import net.minevn.dotman.commands.AdminCmd
import net.minevn.dotman.commands.MainCmd
import net.minevn.dotman.commands.TopNapCmd
import net.minevn.dotman.config.FileConfig
import net.minevn.dotman.config.Language
import net.minevn.dotman.config.MainConfig
import net.minevn.dotman.config.Milestones
import net.minevn.dotman.database.connections.DatabaseConnection
import net.minevn.dotman.database.dao.PlayerDataDAO
import net.minevn.dotman.database.dao.PointsLoggerDAO
import net.minevn.dotman.gui.CardPriceUI
import net.minevn.dotman.gui.CardTypeUI
import net.minevn.dotman.providers.CardProvider
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.guiapi.ConfiguredUI
import org.black_ixx.playerpoints.PlayerPoints
import org.black_ixx.playerpoints.event.PlayerPointsChangeEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level
import kotlin.math.absoluteValue

class DotMan : JavaPlugin(), Listener {

    lateinit var expansion: Expansion private set
    lateinit var playerPoints: PlayerPoints private set

    // configurations
    lateinit var config: MainConfig private set
    lateinit var language: Language private set
    lateinit var minestones: Milestones private set

    override fun onEnable() {
        instance = this
        server.pluginManager.registerEvents(this, this)

        val playerPoints = server.pluginManager.getPlugin("PlayerPoints") as PlayerPoints?
        if (playerPoints == null) {
            logger.log(Level.WARNING, "Could not find PlayerPoints.")
            server.pluginManager.disablePlugin(instance)
            return
        }
        this.playerPoints = playerPoints
        reload()
        MainCmd.init()
        AdminCmd.init()
        TopNapCmd.init()
        expansion = Expansion().apply { register() }
    }

    fun reload() {
        config = MainConfig()
        DatabaseConnection.init(config.dbEngine, config.config)
        language = Language()
        minestones = Milestones()

        // init Gui configs
        CardTypeUI()
        CardPriceUI()
        ConfiguredUI.reloadConfigs(this)
        val providerConfig = FileConfig("providers/${config.provider}").apply { reload() }
        CardProvider.init(config.provider, providerConfig.config)
    }

    override fun onDisable() {
        expansion.unregister()
        DatabaseConnection.unload()
    }

    // region events
    @EventHandler
    fun onPointChanging(event: PlayerPointsChangeEvent) {
        val player = Bukkit.getPlayer(event.playerId) ?: run {
            warning("Point used: Could not find player ${event.playerId}")
            return@onPointChanging
        }
        if (ignoreLoggingList.contains(player)) return
        val amount = event.change
        val pointFrom = instance.playerPoints.api.look(player.uniqueId)
        val pointTo = pointFrom + amount
        runNotSync {
            PointsLoggerDAO.getInstance().insertLog(player, amount, pointFrom, pointTo)
            val dataKey = if (amount > 0) TOP_KEY_POINT_RECEIVED else TOP_KEY_POINT_USED
            PlayerDataDAO.getInstance().insertAllType(player, dataKey, amount.absoluteValue)
        }
    }
    // endregion


    companion object {
        lateinit var instance: DotMan private set
        val ignoreLoggingList = mutableSetOf<Player>()

        /**
         * Cộng point: Nên chạy async
         *
         * @param player Người chơi
         * @param amount Số point cộng
         * @param reason Lý do cộng
         */
        @JvmStatic
        fun addPoints(player: Player, amount: Int, reason: String) {
            val pointFrom = instance.playerPoints.api.look(player.uniqueId)
            val pointTo = pointFrom + amount
            PointsLoggerDAO.getInstance().insertLog(player, amount, pointFrom, pointTo, reason)
            val dataKey = if (amount > 0) TOP_KEY_POINT_RECEIVED else TOP_KEY_POINT_USED
            PlayerDataDAO.getInstance().insertAllType(player, dataKey, amount.absoluteValue)
            ignoreLoggingList.add(player)
            try {
                instance.playerPoints.api.give(player.uniqueId, amount)
            } finally {
                ignoreLoggingList.remove(player)
            }
        }
    }
}
