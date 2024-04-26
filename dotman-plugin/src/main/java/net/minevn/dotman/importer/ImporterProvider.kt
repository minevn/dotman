package net.minevn.dotman.importer

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.Card
import net.minevn.dotman.database.LogDAO
import net.minevn.dotman.importer.types.TheSieuTocIP
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration

abstract class ImporterProvider {
    companion object {
        lateinit var instance: ImporterProvider private set

        fun init(provider: String, config: YamlConfiguration) {
            instance = when (provider) {
                "thesieutoc" -> {
                    TheSieuTocIP(config)
                }

                else -> {
                    throw IllegalArgumentException("Provider $provider not found")
                }
            }
        }
    }

    protected val main = DotMan.instance
    protected val log = LogDAO.getInstance()
    protected val mainConfig = main.config
    abstract fun import(sender: CommandSender)

    @Suppress("DEPRECATION")
    fun processImport(dummyCard: Card, username: String, status: String, timestamp: Long) {
        val player = main.server.getPlayer(username)
        if (player == null) {
            val uuid = main.server.getOfflinePlayer(username).uniqueId
            val offlinePlayer = main.server.getOfflinePlayer(uuid)
            dummyCard.logId = log.insertLog(offlinePlayer, dummyCard)
        } else {
            dummyCard.logId = log.insertLog(player, dummyCard)
        }
        log.stopWaiting(dummyCard.logId!!, status == "thanh cong")
        log.updatePointReceived(dummyCard.logId!!, dummyCard.price.getPointAmount())
        log.updateTime(dummyCard.logId!!, timestamp)
    }
}
