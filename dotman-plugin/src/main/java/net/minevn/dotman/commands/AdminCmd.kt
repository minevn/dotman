package net.minevn.dotman.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.minevn.dotman.DotMan
import net.minevn.dotman.database.dao.ConfigDAO
import net.minevn.dotman.database.dao.LogDAO
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.libs.bukkit.Command
import net.minevn.libs.bukkit.asString
import net.minevn.libs.bukkit.command
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.LocalDate
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
            val usage = "[-p <tên người chơi>] [-m <tháng cần tra>] <Số trang>"
            val logDao = LogDAO.getInstance()

            description("Xem lịch sử nạp thẻ")

            tabComplete {
                when(args.size) {
                    0 -> emptyList()
                    1 -> if(args.last().isEmpty()) listOf(usage) else emptyList()
                    else -> when(args.takeLast(2).first()) {
                        "-p" -> Bukkit.getOnlinePlayers().map { it.name }
                        "-m" -> run {
                            val value = args.last()
                            val year = LocalDate.now().year
                            (1..12).map { "${it.toString().padStart(2, '0')}/$year" }
                                .filter { month -> month.startsWith(value) || month.startsWith("0$value") }
                        }
                        else -> listOf("1")
                    }
                }
            }

            action {
                if (args.isEmpty()) {
                    sender.send("Tra cứu lịch sử nạp thẻ của người chơi hoặc toàn server")
                    sender.send("Cách dùng: /dotman history $usage")
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
                            "§eTổng nạp tháng $month: §d§l$sum VNĐ"
                        } else "§eTổng nạp từ trước đến nay: §d§l$sum VNĐ"
                        val logs = logDao.getHistory(playerName, month, page)

                        val paginationBuilder = StringBuilder("/dotman history").run {
                            if (playerName != null) append(" -p $playerName")
                            if (month != null) append(" -m $month")
                            toString()
                        }

                        val pagination = ComponentBuilder("").run {
                            if (page > 1) {
                                append("<< Trang trước")
                                color(ChatColor.GREEN)
                                event(ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "$paginationBuilder ${page - 1}"))
                                event(HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    ComponentBuilder("§aClick để quay về trang trước").create()))
                                append(" | ").reset().color(ChatColor.GRAY)
                            }
                            append("Trang $page/$maxPage")
                            color(ChatColor.YELLOW)
                            if (page < maxPage) {
                                append(" | ").color(ChatColor.GRAY)
                                append("Trang sau >>")
                                color(ChatColor.GREEN)
                                event(ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "$paginationBuilder ${page + 1}"))
                                event(HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    ComponentBuilder("§aClick để đi đến trang sau").create()))
                            }
                            create()
                        }
                        val paginationConsole = "Trang $page/$maxPage"

                        sender.send(title)
                        sender.sendMessage(total)
                        sender.sendMessage("§f")
                        logs.forEach(sender::sendMessage)
                        if (sender is Player) {
                            sender.spigot().sendMessage(*pagination)
                        } else {
                            sender.sendMessage(paginationConsole)
                        }
                    } catch (e: DateTimeParseException) {
                        sender.send("§cSai định dạng tháng. Ví dụ định dạng đúng: 01/2024")
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
