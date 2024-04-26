package net.minevn.dotman.commands

import net.minevn.dotman.DotMan
import net.minevn.dotman.DotMan.Companion.transactional
import net.minevn.dotman.database.ConfigDAO
import net.minevn.dotman.database.LogDAO
import net.minevn.dotman.database.PlayerInfoDAO
import net.minevn.dotman.importer.ImporterProvider
import net.minevn.dotman.utils.Utils.Companion.makePagination
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.libs.bukkit.asString
import net.minevn.libs.bukkit.command
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.math.ceil

class AdminCmd {
    companion object {
        fun init() = command {
            addSubCommand(reload(), "reload")
            addSubCommand(thongbao(), "thongbao")
            addSubCommand(setBankLocation(), "chuyenkhoan")
            addSubCommand(history(), "lichsu", "history")
            addSubCommand(napThuCong(), "napthucong", "manual")
            addSubCommand(import(), "import")

            action {
                sender.sendMessage("§b§lCác lệnh của plugin DotMan")
                sendSubCommandsUsage(sender, commandTree)
            }

            register(DotMan.instance, "dotman")
        }

        private fun reload() = command {
            description("Reload plugin")

            action {
                DotMan.instance.reload()
                sender.send("§aĐã reload plugin")
            }
        }

        private fun thongbao() = command {
            description("Thay đổi thông báo trong giao diện nạp thẻ")

            action { runNotSync {
                val config = ConfigDAO.getInstance()

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
            description("Đặt vị trí xem hướng dẫn chuyển khoản")

            action { runNotSync {
                val config = ConfigDAO.getInstance()

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

            description("Xem lịch sử nạp thẻ")

            tabComplete {
                when(args.size) {
                    0 -> emptyList()
                    1 -> if(args.last().isEmpty()) listOf(usage) else emptyList()
                    else -> when(args.takeLast(2).first()) {
                        "-p" -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args.last()) }
                        "-m" -> run {
                            val value = args.last()
                            val year = LocalDate.now().year
                            (1..12).map { "${it.toString().padStart(2, '0')}/$year" }
                                .filter { month -> month.startsWith(value) || month.startsWith("0$value") }
                        }
                        else -> listOf("<số trang>", "-p", "-m").filter { it.startsWith(args.last()) }
                    }
                }
            }

            action {
                val logDao = LogDAO.getInstance()

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

                        val pagination = makePagination(paginationBuilder, page, maxPage, sender is Player)

                        sender.send(title)
                        sender.sendMessage(total)
                        sender.sendMessage("§f")
                        logs.forEach(sender::sendMessage)
                        sender.spigot().sendMessage(*pagination)
                    } catch (e: DateTimeParseException) {
                        sender.send("§cSai định dạng tháng. Ví dụ định dạng đúng: 01/2024")
                    }
                }
            }
        }

        private fun napThuCong() = command {
            val usage = "<tên người chơi> <số tiền> [-p <số point nhận>] [-d <nội dung nạp>] [-f force offline]"

            description("Nạp tiền thủ công cho người chơi")

            tabComplete {
                when(args.size) {
                    0 -> emptyList()
                    1 -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args.last()) }
                    2 -> if (args.last().isEmpty()) listOf("<số tiền nạp>") else emptyList()
                    else -> when(args.takeLast(2).first()) {
                        "-p" -> if (args.last().isEmpty()) listOf("<số point nhận>") else emptyList()
                        "-d" -> if (args.last().isEmpty()) listOf("<nội dung nạp>") else emptyList()
                        else -> if (args.last().isEmpty()) listOf("-p", "-d", "-f") else emptyList()
                    }
                }
            }

            action {
                val args = args.toMutableList()
                val main = DotMan.instance
                val cfg = main.config

                var playerName: String? = null
                var amount: Int? = null
                var point: Double? = null
                var content = "THỦ CÔNG"
                var forceOffline = false

                var index = 0
                while (args.isNotEmpty()) {
                    when (val current = args.removeFirst()) {
                        "-p" -> point = args.removeFirst().toDoubleOrNull() ?: run {
                            sender.send("§cSố point nhận phải là số")
                            return@action
                        }
                        "-d" -> {
                            val sb = StringBuilder()
                            while (args.isNotEmpty()) {
                                if (args.first().matches("-[a-z]".toRegex())) break
                                sb.append(args.removeFirst()).append(" ")
                                content = sb.toString().trim().takeIf { it.length <= 20 } ?: run {
                                    sender.send("§cNội dung không được quá 20 ký tự")
                                    return@action

                                }
                            }
                        }
                        "-f" -> forceOffline = true
                        else -> when(index++) {
                            0 -> playerName = current
                            1 -> amount = current.toIntOrNull() ?: run {
                                sender.send("§cSố tiền phải là số")
                                return@action
                            }
                            else -> {
                                sender.send("§cCách dùng: /$commandTree $usage")
                                return@action
                            }
                        }
                    }
                }

                if (playerName == null || amount == null) {
                    sender.send("§cCách dùng: /$commandTree $usage")
                    return@action
                }

                if (point == null) {
                    val isExtra = cfg.extraUntil > System.currentTimeMillis()
                    val extraRate = if (isExtra) cfg.extraRate else 0.0
                    val pointPer1K = cfg.manualBase + cfg.manualExtra + (cfg.manualBase * extraRate)
                    point = (amount / 1000) * pointPer1K
                }

                runNotSync { transactional {
                    try {
                        val infoDao = PlayerInfoDAO.getInstance()
                        val uuidStr = infoDao.getUUID(playerName) ?: run {
                            sender.send("§cNgười chơi $playerName không tồn tại. Có thể họ chưa vào server bao giờ?")
                            return@transactional
                        }

                        val uuid = UUID.fromString(uuidStr)
                        if (Bukkit.getPlayer(uuid)?.isOnline != true && !forceOffline) {
                            sender.send("§cNgười chơi $playerName hiện không online, vì vậy phần thưởng mốc nạp sẽ " +
                                    "không được thực hiện nếu họ đạt mốc.")
                            sender.send("§cHãy thêm tùy chọn §a-f §cvào lệnh nếu bạn vẫn muốn thực hiện nạp thủ công.")
                            return@transactional
                        }

                        main.playerPoints.api.give(UUID.fromString(uuidStr), amount)
                        main.updateLeaderBoard(uuid, amount, point.toInt())
                        val logDao = LogDAO.getInstance()
                        logDao.insertLog(uuidStr, content, "--", "MANUAL", amount).let {
                            logDao.stopWaiting(it, true)
                            logDao.updatePointReceived(it, point.toInt())
                        }
                        sender.send("§aĐã nạp §d$amount VNĐ §acho người chơi §b$playerName " +
                                "§avà nhận §b${point.toInt()} §apoint")

                    } catch (e: Exception) {
                        sender.send("§cCó lỗi xảy ra: ${e.message} (chi tiết hãy xem Console và báo lỗi cho MineVN Studio)")
                        throw e
                    }
                }}
            }
        }

        private fun import() = command {
            description("Import dữ liệu từ plugin khác")

            action {
                ImporterProvider.instance.import(sender)
            }
        }
    }
}