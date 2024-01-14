package net.minevn.dotman.commands

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.Card
import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.gui.CardTypeUI
import net.minevn.dotman.providers.CardProvider
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.libs.bukkit.command
import org.bukkit.entity.Player

class MainCmd {
    companion object {
        fun init() {
            val main = DotMan.instance

            // lệnh nạp thẻ
            // cú pháp nạp nhanh: /napthe <loại thẻ> <mệnh giá> <số seri> <mã thẻ>
            command {
                val provider = CardProvider.instance

                // tab complete cho lệnh nạp nhanh
                tabComplete {
                    when (args.size) {
                        1 -> provider.getAvailableCardType().map { it.name.lowercase() }
                            .filter { it.startsWith(args.last().lowercase()) }
                        2 -> CardPrice.entries.map { it.value.toString() }
                            .filter { it.startsWith(args.last()) }
                        3 -> listOf("seri")
                        4 -> listOf("code")
                        else -> emptyList()
                    }
                }

                action a@{
                    if (sender !is Player) {
                        sender.sendMessage("§cLệnh này chỉ dành cho người chơi")
                        return@a
                    }
                    val player = sender as Player

                    // nạp nhanh
                    if (args.size == 4) {
                        val cardType = CardType[args[0]]
                        val cardPrice = CardPrice[args[1].toInt()]
                        val cardSeri = args[2]
                        val cardCode = args[3]
                        if (cardType == null) {
                            player.send(main.language.errorUnknownCardType)
                            return@a
                        }
                        if (cardPrice == null) {
                            player.send(main.language.errorUnknownCardPrice)
                            return@a
                        }

                        val card = Card(cardSeri, cardCode, cardPrice, cardType)
                        CardProvider.instance.processCard(player, card)
                    } else {
                        CardTypeUI(player)
                    }
                }

                register(DotMan.instance, "napthe")
            }
        }
    }
}
