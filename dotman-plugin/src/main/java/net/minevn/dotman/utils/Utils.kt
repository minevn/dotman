package net.minevn.dotman.utils

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.minevn.dotman.DotMan
import net.minevn.dotman.config.MainConfig
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask
import java.util.logging.Level

class Utils {
    companion object {
        fun Throwable.severe(message: String) = DotMan.instance.logger.log(Level.SEVERE, message, this)

        fun Throwable.warning(message: String) = DotMan.instance.logger.log(Level.WARNING, message, this)

        fun warning(message: String) = DotMan.instance.logger.warning(message)

        fun info(message: String) = DotMan.instance.logger.info(message)

        fun sendServerMessages(statusMessages: List<String>) = statusMessages.forEach {
            Bukkit.broadcastMessage("${MainConfig.get().prefix} $it".color())
        }

        fun CommandSender.send(message: String) = sendMessage("${MainConfig.get().prefix} $message".color())

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

        fun makePagination(command: String, page: Int, maxPage: Int, makeButton: Boolean) = ComponentBuilder("")
            .run {
                if (makeButton && page > 1) {
                    append("<< Trang trước")
                    color(net.md_5.bungee.api.ChatColor.GREEN)
                    event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$command ${page - 1}"))
                    event(HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ComponentBuilder("§aClick để quay về trang trước").create()
                    ))
                    append(" | ").reset().color(net.md_5.bungee.api.ChatColor.GRAY)
                }

                append("Trang $page/$maxPage")

                color(net.md_5.bungee.api.ChatColor.YELLOW)
                if (makeButton && page < maxPage) {
                    append(" | ").color(net.md_5.bungee.api.ChatColor.GRAY)
                    append("Trang sau >>")
                    color(net.md_5.bungee.api.ChatColor.GREEN)
                    event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$command ${page + 1}"))
                    event(HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ComponentBuilder("§aClick để đi đến trang sau").create()
                    ))
                }
                create()
            }
    }
}
