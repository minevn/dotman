package net.minevn.dotman

import net.minevn.dotman.commands.AdminCmd
import net.minevn.dotman.commands.MainCmd
import net.minevn.dotman.commands.TopNapCmd
import net.minevn.dotman.config.FileConfig
import net.minevn.dotman.config.Language
import net.minevn.dotman.config.MainConfig
import net.minevn.dotman.config.Milestones
import net.minevn.dotman.database.ConfigDAO
import net.minevn.dotman.database.PlayerInfoDAO
import net.minevn.dotman.gui.CardPriceUI
import net.minevn.dotman.gui.CardTypeUI
import net.minevn.dotman.providers.CardProvider
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.guiapi.ConfiguredUI
import net.minevn.libs.bukkit.MineVNPlugin
import net.minevn.libs.bukkit.color
import net.minevn.libs.bukkit.db.BukkitDBMigrator
import net.minevn.libs.db.Transaction
import org.black_ixx.playerpoints.PlayerPoints
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.logging.Level

class DotMan : MineVNPlugin(), Listener {

    lateinit var expansion: Expansion private set
    lateinit var playerPoints: PlayerPoints private set
    var prefix = "&6&lDotMan >&r".color(); private set

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
            server.pluginManager.disablePlugin(this)
            return
        }
        this.playerPoints = playerPoints
        reload()
        MainCmd.init()
        AdminCmd.init()
        TopNapCmd.init()
        UpdateChecker.init()
        expansion = Expansion().apply { register() }
    }

    private fun migrate() {
        val configDao = ConfigDAO.getInstance()
        val schemaVersion = configDao.get("migration_version") ?: "0"
        val path = "db/migrations/${dbPool!!.getTypeName()}"
        val updated = dbPool!!.getConnection().use {
            BukkitDBMigrator(this, it, path, schemaVersion.toInt()).migrate()
        }
        configDao.set("migration_version", updated.toString())
    }

    fun reload() {
        config = MainConfig()
        prefix = config.prefix
        initDatabase(config.config.getConfigurationSection("database"))
        migrate()
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
        dbPool?.disconnect()
    }

    // region events
    private fun updateUUID(player: Player) {
        val uuid = player.uniqueId.toString()
        val name = player.name
        runNotSync { PlayerInfoDAO.getInstance().updateData(uuid, name) }
    }

    @EventHandler
    fun onLogin(e: PlayerLoginEvent) = updateUUID(e.player)

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) = updateUUID(e.player)
    // endregion

    companion object {
        lateinit var instance: DotMan private set

        fun transactional(action: Transaction.() -> Unit) {
            if (Bukkit.isPrimaryThread()) {
                throw IllegalStateException("Cannot run transactional code on the main thread")
            }
            net.minevn.libs.db.transactional(instance.dbPool!!.getConnection(), action)
        }
    }
}
