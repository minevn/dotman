package net.minevn.dotman.commands

import net.minevn.libs.bukkit.command

class TopNapCmd { companion object { fun init() {
    command {
        description("Xem top nạp thẻ")

        action {
            sender.sendMessage("§cLệnh này đang được phát triển")
        }
    }
}}}