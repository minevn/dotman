package net.minevn.dotman.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.minevn.dotman.DotMan
import net.minevn.dotman.database.dao.ConfigDAO
import net.minevn.dotman.database.dao.LogDAO
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.libs.bukkit.Command
import net.minevn.libs.bukkit.asString
import net.minevn.libs.bukkit.command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.format.DateTimeParseException
import kotlin.math.ceil

class AdminCmd {
    companion object {
        private lateinit var instance: Command

        fun init() {
            instance = command {
                addSubCommand(reload(), "reload")
                addSubCommand(thongbao(), "thongbao")
                addSubCommand(setBankLocation(), "chuyenkhoan")
                addSubCommand(history(), "lichsu", "history")

                action { sendHelp(sender) }
                register(DotMan.instance, "dotman")
            }
        }

        private fun reload() = command {
            description("Reload plugin")

            action {
                DotMan.instance.reload()
                sender.send("§aĐã reload plugin")
            }
        }

        private fun thongbao() = command {
            val config = ConfigDAO.getInstance()

            description("Thay đổi thông báo trong giao diện nạp thẻ")

            action { runNotSync {
                val message = args.joinToString(" ")
                if (message.isEmpty()) {
                    config.delete("announcement")
                    sender.send("§aĐã xóa thông báo thành công")
                } else {
                    config.set("announcement", args.joinToString(" "))
                    sender.send("§aĐã thay đổi thông báo thành công")
                }
            }}
        }

        private fun setBankLocation() = command {
            val config = ConfigDAO.getInstance()

            description("Đặt vị trí xem hướng dẫn chuyển khoản")

            action { runNotSync {
                val player = sender as? Player ?: run {
                    sender.send("Vào server rồi thực hiện lệnh này.")
                    return@runNotSync
                }
                config.set("banking-location", player.location.asString())
                player.send("Đã đặt vị trí xem hướng dẫn chuyển khoản")
            }}
        }

        private fun history() = command {
            val logDao = LogDAO.getInstance()

            description("Xem lịch sử nạp thẻ")

            action {
                if (args.isEmpty()) {
                    sender.send("Tra cứu lịch sử nạp thẻ của người chơi hoặc toàn server")
                    sender.send("Cách dùng: /dotman history [-p <tên người chơi>] [-m <tháng cần tra>] <Số trang>")
                    return@action
                }

                // parse args: [-p <player>] [-m <month>] <page>
                val args = args.toMutableList()
                var playerName: String? = null
                var month: String? = null
                var page = 1
                while (args.isNotEmpty()) {
                    when (val current = args.removeFirst()) {
                        "-p" -> playerName = args.removeFirst()
                        "-m" -> month = args.removeFirst()
                        else -> page = current.toIntOrNull() ?: 1
                    }
                }

                runNotSync {
                    try {
                        val (sum, count) = logDao.getSum(playerName, month)
                        val maxPage = ceil(count / 20.0).toInt()
                        val title = "§aLịch sử nạp thẻ của §b%PLAYER_NAME%"
                            .replace("%PLAYER_NAME%", playerName ?: "toàn server")
                        val total = if (month != null) {
                            "Tổng nạp tháng $month: $sum"
                        } else "Tổng nạp từ trước đến nay: $sum"
                        val logs = logDao.getHistory(playerName, month, page)

                        val pagination = ComponentBuilder("").run {
                            if (page > 1) {
                                append("<< Trang trước")
                                color(ChatColor.GREEN)
                                append(" | ").color(ChatColor.GRAY)
                            }
                            append("Trang $page")
                            color(ChatColor.YELLOW)
                            if (page < maxPage) {
                                append(" | ").color(ChatColor.GRAY)
                                append("Trang sau >>")
                                color(ChatColor.GREEN)
                            }
                            create()
                        }
                        val paginationConsole = "Trang $page/$maxPage"

                        sender.send(title)
                        sender.sendMessage(total)
                        logs.forEach(sender::sendMessage)
                        if (sender is Player) {
                            sender.spigot().sendMessage(*pagination)
                        } else {
                            sender.sendMessage(paginationConsole)
                        }
                    } catch (e: DateTimeParseException) {
                        sender.send("§cSai định dạng tháng. Ví dụ định dạng đúng: 01/2021")
                    }
                }
            }
        }

        private fun sendHelp(sender: CommandSender) {
            // TODO: Trang trí cho đẹp
            sender.sendMessage("§b§lCác lệnh của plugin DotMan")
            instance.getSubCommands().distinctBy { it.second }.filter { it.second.getDescription() != null }.forEach {
                sender.sendMessage("§a/dotman ${it.first} §7- ${it.second.getDescription()}")
            }
        }
    }
}
