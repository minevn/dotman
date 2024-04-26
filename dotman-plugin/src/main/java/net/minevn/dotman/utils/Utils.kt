package net.minevn.dotman.utils

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.minevn.dotman.DotMan
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.logging.Level

class Utils {
    companion object {
        fun Throwable.severe(message: String) = DotMan.instance.logger.log(Level.SEVERE, message, this)

        fun Throwable.warning(message: String) = DotMan.instance.logger.log(Level.WARNING, message, this)

        fun warning(message: String) = DotMan.instance.logger.warning(message)

        fun info(message: String) = DotMan.instance.logger.info(message)

        fun sendServerMessages(statusMessages: List<String>) = statusMessages.forEach {
            Bukkit.broadcastMessage("${DotMan.instance.config.prefix} $it".color())
        }

        fun CommandSender.send(message: String) = sendMessage("${DotMan.instance.config.prefix} $message".color())

        fun runAsyncTimer(delay: Long, period: Long, r: Runnable): BukkitTask =
            Bukkit.getScheduler().runTaskTimerAsynchronously(DotMan.instance, r, delay, period)

        fun runAsync(r: Runnable) {
            Bukkit.getScheduler().runTaskAsynchronously(DotMan.instance, r)
        }

        fun runNotSync(r: Runnable) {
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(DotMan.instance, r)
            } else {
                r.run()
            }
        }

        fun runSync(r: Runnable) {
            if (Bukkit.isPrimaryThread()) {
                r.run()
            } else {
                Bukkit.getScheduler().runTask(DotMan.instance, r)
            }
        }

        fun List<String>.color() = map { it.color() }

        fun String.color(): String = ChatColor.translateAlternateColorCodes('&', this)

        fun makePagination(command: String, page: Int, maxPage: Int, makeButton: Boolean) : Array<BaseComponent> {
            return ComponentBuilder("\n§r").run {
                if (makeButton && page > 1) {
                    append("<< Trang trước")
                    color(ChatColor.GREEN)
                    event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$command ${page - 1}"))
                    event(HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ComponentBuilder("§aClick để quay về trang trước").create()
                    ))
                    append(" | ").reset().color(ChatColor.GRAY)
                }

                append("Trang $page/$maxPage")

                color(ChatColor.YELLOW)
                if (makeButton && page < maxPage) {
                    append(" | ").color(ChatColor.GRAY)
                    append("Trang sau >>")
                    color(ChatColor.GREEN)
                    event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$command ${page + 1}"))
                    event(HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ComponentBuilder("§aClick để đi đến trang sau").create()
                    ))
                }
                create()
            }
        }

        // 13:24 14/04/2024 to unix
        fun String.toUnixTime(): Long {
            val parts = this.split(" ")
            val time = parts[0].split(":")
            val date = parts[1].split("/")
            return LocalDateTime.of(
                date[2].toInt(),
                date[1].toInt(),
                date[0].toInt(),
                time[0].toInt(),
                time[1].toInt()
            ).toEpochSecond(ZoneOffset.UTC)
        }
    }
}
