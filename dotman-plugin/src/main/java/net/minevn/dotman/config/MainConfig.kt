package net.minevn.dotman.config

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.utils.Utils.Companion.color
import net.minevn.dotman.utils.dateAndTimeFormat
import net.minevn.libs.bukkit.FileConfig

class MainConfig : FileConfig(DotMan.instance, "config") {

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

    val amounts = run {
        val section = config.getConfigurationSection("donate-amounts")!!
        CardPrice.entries.associateWith { section.getInt(it.value.toString()) }
    }
}
