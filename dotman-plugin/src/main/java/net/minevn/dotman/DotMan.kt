package net.minevn.dotman

import net.minevn.dotman.commands.AdminCmd
import net.minevn.dotman.commands.MainCmd
import net.minevn.dotman.commands.TopNapCmd
import net.minevn.dotman.config.FileConfig
import net.minevn.dotman.config.Language
import net.minevn.dotman.config.MainConfig
import net.minevn.dotman.config.Milestones
import net.minevn.dotman.database.connections.DatabaseConnection
import net.minevn.dotman.gui.CardPriceUI
import net.minevn.dotman.gui.CardTypeUI
import net.minevn.dotman.providers.CardProvider
import net.minevn.guiapi.ConfiguredUI
import net.minevn.libs.bukkit.color
import org.black_ixx.playerpoints.PlayerPoints
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class DotMan : JavaPlugin() {

    lateinit var expansion: Expansion private set
    lateinit var playerPoints: PlayerPoints private set
    var prefix = "&6&lDotMan >&r".color(); private set

    // configurations
    lateinit var config: MainConfig private set
    lateinit var language: Language private set
    lateinit var minestones: Milestones private set

    override fun onEnable() {
        instance = this

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
        prefix = config.prefix
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

    companion object {
        lateinit var instance: DotMan private set
    }
}
