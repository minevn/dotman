package net.minevn.dotman.commands

import net.minevn.dotman.DotMan
import net.minevn.dotman.LeaderBoard
import net.minevn.dotman.TOP_KEY_DONATE_TOTAL
import net.minevn.dotman.utils.Utils.Companion.makePagination
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.libs.bukkit.command
import org.bukkit.entity.Player
import kotlin.math.ceil

class TopNapCmd { companion object { fun init() {
    command {
        description("Xem top nạp thẻ")

        action { runNotSync {
            val page = args.getOrNull(0)?.toIntOrNull() ?: 1
            val top = LeaderBoard["${TOP_KEY_DONATE_TOTAL}_ALL"]
            val maxPage = ceil(top.size() / 20.0).toInt()
//            if (page > maxPage) {
//                sender.sendMessage("§cKhông tìm thấy trang $page")
//                return@runNotSync
//            }
            val pagination = makePagination("/topnap", page, maxPage, sender is Player)

            sender.sendMessage("§aTop nạp thẻ: Trang $page/$maxPage\n")
            for (i in (page - 1) * 20 until page * 20) {
                val rank = i + 1
                val entry = top[rank] ?: break
                sender.sendMessage("§a${rank}. §b${entry.first} §e${entry.second} VNĐ")
            }
            sender.spigot().sendMessage(*pagination)
        }}

        register(DotMan.instance, "topnap")
    }
}}}