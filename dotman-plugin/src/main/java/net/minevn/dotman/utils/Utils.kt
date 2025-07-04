package net.minevn.dotman.utils

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.minevn.dotman.DotMan
import net.minevn.libs.anvilgui.AnvilGUI
import net.minevn.libs.bukkit.FoliaUtils
import net.minevn.libs.xseries.XMaterial
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask
import java.security.MessageDigest
import java.text.DecimalFormat
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
            if (FoliaUtils.isFolia()) {
                FoliaUtils.runAsyncTimer(DotMan.instance, r, delay, period)
            } else {
                Bukkit.getScheduler().runTaskTimerAsynchronously(DotMan.instance, r, delay, period)
            }

        fun runAsync(r: Runnable) {
            if (FoliaUtils.isFolia()) {
                FoliaUtils.runAsync(DotMan.instance, r)
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(DotMan.instance, r)
            }
        }

        fun runNotSync(r: Runnable) {
            if (Bukkit.isPrimaryThread()) {
                if (FoliaUtils.isFolia()) {
                    FoliaUtils.runAsync(DotMan.instance, r)
                } else {
                    Bukkit.getScheduler().runTaskAsynchronously(DotMan.instance, r)
                }
            } else {
                r.run()
            }
        }

        fun runSync(r: Runnable) {
            if (Bukkit.isPrimaryThread()) {
                r.run()
            } else {
                if (FoliaUtils.isFolia()) {
                    FoliaUtils.runGlobal(DotMan.instance, r)
                } else {
                    Bukkit.getScheduler().runTask(DotMan.instance, r)
                }
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

        fun Int.format(): String {
            val format = DecimalFormat("#,###")
            val decimalFormatSymbols = format.decimalFormatSymbols
            decimalFormatSymbols.groupingSeparator = '.'
            format.decimalFormatSymbols = decimalFormatSymbols
            return format.format(this)
        }

        fun closeAnvilAction(runnable: Runnable) = listOf(AnvilGUI.ResponseAction.run(runnable),
            AnvilGUI.ResponseAction.close())

        fun createItem(material: XMaterial, name: String, vararg lore: String) = material.parseItem()!!.apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(name.color())
                this.lore = lore.toList().color()
            }
        }

        fun String.md5(): String {
            return MessageDigest.getInstance("MD5").digest(toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
