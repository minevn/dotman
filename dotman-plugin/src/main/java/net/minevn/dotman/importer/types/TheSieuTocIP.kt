package net.minevn.dotman.importer.types

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.Card
import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.importer.ImporterProvider
import net.minevn.dotman.utils.FileUtils
import net.minevn.dotman.utils.Utils.Companion.toUnixTime
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration

class TheSieuTocIP(private val config: YamlConfiguration) : ImporterProvider() {

    override fun import(sender: CommandSender) {
        var count = 0
        val importFile = "${DotMan.instance.dataFolder}/${config.getString("file")}"
        when (config.getString("storage")) {
            "FLATFILE" -> {
                if (!FileUtils.isFileExist(importFile)) {
                    sender.sendMessage("§cFile log không tồn tại")
                    return
                }
                FileUtils.readLines(importFile).forEach { it ->
                    // [13:24 14/04/2024] CursedKiwi | 20000270206130 | 022493549094317 | 10000 | Viettel | thanh cong
                    try {
                        val parts = it.split('|').map { it.trim() }
                        val timestampAndUser = parts[0]
                        val timestamp = timestampAndUser
                            .substringAfter('[')
                            .substringBefore(']')
                            .trim()
                            .toUnixTime()
                        val username = timestampAndUser.substringAfter(']').trim()
                        val seri = parts[1]
                        val mathe = parts[2]
                        val menhgia = parts[3].toInt()
                        val type = parts[4].uppercase()
                        val status = parts[5]
                        val dummyCard = Card(seri, mathe, CardPrice[menhgia]!!, CardType.valueOf(type))

                        sender.sendMessage("§aĐang import thẻ ${dummyCard.toString()}")
                        processImport(dummyCard, username, status, timestamp)
                        count++
                    } catch (e: Exception) {
                        sender.sendMessage("§cLỗi khi import thẻ, hãy kiểm tra lại file log của plugin cần import và đảm bảo dữ liệu trên 1 dòng cách nhau bởi dấu |")
                        sender.sendMessage("§cVí dụ: §e[13:24 14/04/2024] CursedKiwi | 20000270206130 | 022493549094317 | 10000 | Viettel | thanh cong")
                    }
                }
                sender.sendMessage("§aĐã import thành công $count thẻ")
            }

            else -> {
                sender.sendMessage("§cStorage không tồn tại")
            }
        }
    }
}
