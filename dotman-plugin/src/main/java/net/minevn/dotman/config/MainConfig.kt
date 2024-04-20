package net.minevn.dotman.config

import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.utils.Utils.Companion.color
import net.minevn.dotman.utils.dateAndTimeFormat

class MainConfig : FileConfig("config") {
    val checkUpdate = config.getBoolean("check-update", true)
    val announceCharge = config.getBoolean("announce-charge", true)
    val prefix = config.getString("prefix", "&6&lDotMan > &r")!!.color()
    val pointUnit = config.getString("point-unit", "point")!!.color()
    val extraUntil = dateAndTimeFormat.parse(config.getString("extra-until", "01/01/1970 00:00")).time
    val extraRate = config.getDouble("extra-rate")
    val enableStatusNotification = config.getBoolean("enable-status-notification") // TODO
    val dbEngine = config.getString("database.engine", "h2")!!
    val provider = config.getString("provider", "")!!
    val server = config.getString("server", "")!!
    val cardTypes = config.getConfigurationSection("card-types").run {
        CardType.entries.associateWith { getBoolean(it.name.lowercase()) }
    }

    val amounts = config.getConfigurationSection("donate-amounts")!!.run {
        CardPrice.entries.associateWith { getInt(it.value.toString()) }
    }

    val commands = config.getConfigurationSection("donate-commands")!!.run {
        CardPrice.entries.associateWith { getStringList(it.value.toString()).toList() }
    }

    val manualBase = config.getDouble("manual.point-base")
    val manualExtra = config.getDouble("manual.point-extra")
}
