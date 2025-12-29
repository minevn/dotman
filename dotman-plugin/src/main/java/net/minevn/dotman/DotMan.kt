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
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.guiapi.ConfiguredUI
import net.minevn.libs.bukkit.MineVNPlugin
import net.minevn.libs.bukkit.color
import net.minevn.libs.bukkit.db.BukkitDBMigrator
import net.minevn.libs.db.Transaction
import org.black_ixx.playerpoints.PlayerPoints
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level

class DotMan : MineVNPlugin() {

    lateinit var expansion: Expansion private set
    lateinit var playerPoints: PlayerPoints private set
    var prefix = "&6&lDotMan >&r".color(); private set

    // configurations
    lateinit var config: MainConfig private set
    lateinit var language: Language private set
    lateinit var milestones: Milestones private set
    lateinit var milestonesMaster: MilestonesMaster private set
    lateinit var discord: Discord private set
    lateinit var plannedExtras: PlannedExtras private set

    override fun onEnable() {
        instance = this
        server.pluginManager.registerEvents(DotManListener(), this)
        Metrics(this, 23982)

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
        plannedExtras = PlannedExtras()

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

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (::expansion.isInitialized) {
                expansion.unregister()
            }
            expansion = Expansion()
        } else {
            warning("PlaceholderAPI chưa được cài đặt, một số tính năng sẽ không hoạt động.")
        }
    }

    override fun onDisable() {
        dbPool?.disconnect()
        if (::expansion.isInitialized) expansion.unregister()
        if (::milestonesMaster.isInitialized) milestonesMaster.removeBossBars()
    }

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
            val timeStr = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"))
            val replacements = mapOf(
                "%PLAYER%" to playerName,
                "%AMOUNT%" to amount.format(),
                "%POINT_AMOUNT%" to pointAmount.toString(),
                "%POINT_UNIT%" to config.pointUnit,
                "%BALANCE%" to balance.toString(),
                "%METHOD%" to type.typeName,
                "%TIME%" to timeStr,
                "%SERVER%" to config.server
            )
            discord.webhooks.forEach { sender ->
                sender.send(replacements)
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
