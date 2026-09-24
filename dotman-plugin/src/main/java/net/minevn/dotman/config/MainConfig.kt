package net.minevn.dotman.config

import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.utils.Utils.Companion.color
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.dotman.utils.parseConfigDateTime
import java.text.ParseException

class MainConfig : FileConfig("config") {
    val checkUpdate = config.getBoolean("check-update", true)
    val announceCharge = config.getBoolean("announce-charge", true)
    val prefix = config.getString("prefix", "&6&lDotMan > &r")!!.color()
    val pointUnit = config.getString("point-unit", "point")!!.color()
    val extraUntil = parseExtraUntil(config.getString("extra-until", "01/01/1970 00:00")!!)
    val extraRate = config.getDouble("extra-rate")
    val provider = config.getString("provider", "")!!
    val server = config.getString("server", "")!!
    val cardTypes = config.getConfigurationSection("card-types")!!.run {
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
    val useAnvilGui = config.getBoolean("use-anvilgui", true)

    /**
     * Đọc extra-until, nhận dd/MM/yyyy HH:mm:ss hoặc dd/MM/yyyy HH:mm.
     * Sai định dạng thì cảnh báo và coi như khuyến mãi đã hết hạn, không làm hỏng việc nạp config.
     */
    private fun parseExtraUntil(value: String): Long {
        return try {
            parseConfigDateTime(value)
        } catch (e: ParseException) {
            warning("extra-until trong config.yml không hợp lệ (${e.message}), khuyến mãi trong config.yml sẽ không được áp dụng")
            0
        }
    }
}
