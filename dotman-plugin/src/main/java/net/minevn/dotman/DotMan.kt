package net.minevn.dotman

import net.minevn.dotman.commands.AdminCmd
import net.minevn.dotman.commands.MainCmd
import net.minevn.dotman.commands.TopNapCmd
import net.minevn.dotman.config.*
import net.minevn.dotman.database.ConfigDAO
import net.minevn.dotman.database.PlayerDataDAO
import net.minevn.dotman.database.PlayerInfoDAO
import net.minevn.dotman.gui.CardPriceUI
import net.minevn.dotman.gui.CardTypeUI
import net.minevn.dotman.providers.CardProvider
import net.minevn.dotman.utils.Utils.Companion.format
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
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*
import java.util.logging.Level

class DotMan : MineVNPlugin(), Listener {

    lateinit var expansion: Expansion private set
    lateinit var playerPoints: PlayerPoints private set
    var prefix = "&6&lDotMan >&r".color(); private set

    // configurations
    lateinit var config: MainConfig private set
    lateinit var language: Language private set
    lateinit var milestones: Milestones private set
    lateinit var milestonesMaster: MilestonesMaster private set
    lateinit var discord: Discord private set

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
        val currentVersion = (configDao.get("migration_version") ?: "0").toInt()
        val path = "db/migrations/${dbPool!!.getTypeName()}"
        val latestVersion = dbPool!!.getConnection().use {
            BukkitDBMigrator(this, it, path, currentVersion).migrate()
        }
        if (latestVersion > currentVersion) {
            configDao.set("migration_version", latestVersion.toString())
        }
    }

    fun reload() {
        config = MainConfig()
        prefix = config.prefix
        initDatabase(config.config.getConfigurationSection("database")!!)
        migrate()
        language = Language()
        milestones = Milestones()
        if (::milestonesMaster.isInitialized) {
            milestonesMaster.removeBossBars()
        }
        milestonesMaster = MilestonesMaster()
        discord = Discord()

        // init Gui configs
        CardTypeUI()
        CardPriceUI()
        ConfiguredUI.reloadConfigs(this)
        val providerConfig = try {
             FileConfig("providers/${config.provider}").apply { reload() }
        } catch (e: Exception) {
            logger.severe("Không tìm thấy hệ thống gạch thẻ \"${config.provider}\", hãy kiểm tra lại config.")
            server.pluginManager.disablePlugin(this)
            return
        }
        CardProvider.init(config.provider, providerConfig.config)
    }

    override fun onDisable() {
        expansion.unregister()
        dbPool?.disconnect()
        milestonesMaster.removeBossBars()
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

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val player = e.player

        milestonesMaster.onJoin(player)
        UpdateChecker.sendUpdateMessage(player)
    }
    // endregion

    /**
     * Cập nhật bảng xếp hạng, mốc nạp
     *
     * @param uuid UUID của người chơi
     * @param amount Số tiền nạp
     * @param pointAmount Số point nhận được
     */
    fun updateLeaderBoard(uuid: UUID, amount: Int, pointAmount: Int, type: TopupType = TopupType.CARD) {
        val dataDAO = PlayerDataDAO.getInstance()

        // Tích điểm
        val uuidStr = uuid.toString()
        dataDAO.insertAllType(uuidStr, TOP_KEY_DONATE_TOTAL, amount)
        dataDAO.insertAllType(uuidStr, TOP_KEY_POINT_FROM_CARD, pointAmount)

        // Mốc nạp
        val currentPlayer = dataDAO.getData(uuidStr, "${TOP_KEY_DONATE_TOTAL}_ALL")
        val currentServer = dataDAO.getSumData("${TOP_KEY_DONATE_TOTAL}_ALL")
        Bukkit.getPlayer(uuid)?.takeIf { it.isOnline }?.let { player ->
            milestones.getAll().filter { it.type == "all" }.forEach {
                it.check(player, currentPlayer, amount)
            }
        }
        milestonesMaster.getAll().filter { it.type == "all" }.forEach {
            it.sumCheck(currentServer, amount)
        }

        // Thông báo nạp qua discord
        runNotSync {
            val balance = playerPoints.api.look(uuid)
            val playerName = PlayerInfoDAO.getInstance().getName(uuidStr) ?: return@runNotSync
            discord.list.forEach {
                val content = it.contentTmpl
                    .replace("%PLAYER%", playerName)
                    .replace("%AMOUNT%", amount.format())
                    .replace("%POINT_AMOUNT%", pointAmount.toString())
                    .replace("%POINT_UNIT%", config.pointUnit)
                    .replace("%BALANCE%", balance.toString())
                    .replace("%METHOD%", type.typeName)
                it.send(content)
            }
        }
    }

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
