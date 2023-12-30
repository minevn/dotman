package net.minevn.dotman.commands

import net.minevn.dotman.DotMan
import net.minevn.dotman.database.dao.ConfigDAO
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.libs.bukkit.Command
import net.minevn.libs.bukkit.asString
import net.minevn.libs.bukkit.command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AdminCmd {
    companion object {
        private lateinit var instance: Command

        fun init() {
            instance = command {
                addSubCommand(reload(), "reload")
                addSubCommand(thongbao(), "thongbao")
                addSubCommand(setBankLocation(), "chuyenkhoan")

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
            val config = ConfigDAO.getInstance()

            description("Xem lịch sử nạp thẻ")

            action { runNotSync {
                // TODO
            }}
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
