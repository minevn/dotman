package net.minevn.dotman.gui

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardType
import net.minevn.dotman.providers.CardProvider
import net.minevn.dotman.utils.Utils.Companion.format
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.guiapi.ConfiguredUI
import net.minevn.guiapi.GuiIcon.Companion.getGuiIcon
import net.minevn.libs.bukkit.MineVNLib.Companion.getGuiFillSlots
import org.bukkit.entity.Player

class CardPriceUI(viewer: Player?, private val cardType: CardType) :
    ConfiguredUI(viewer, "menu/napthe/menhgia.yml", DotMan.instance) {

    constructor() : this(null, CardType.GARENA)

    init {
        if (viewer?.isOnline == true) buildAsync()
    }

    private fun build() {
        val viewer = viewer!!

        val config = getConfig()
        lock()

        // background
        setItem(
            config.getGuiFillSlots("background.fill"),
            config.getGuiIcon("background").toGuiItemStack()
        )

        // close button
        setItem(
            config.getInt("close.slot"),
            config.getGuiIcon("close").toGuiItemStack {
                CardTypeUI(viewer)
            }
        )

        // prices
        val priceSlots = config.getGuiFillSlots("prices.fill")
        val priceIcon = config.getGuiIcon("prices")
        for (i in priceSlots.indices) {
            val price = CardPrice.entries[i]
            val button = priceIcon.clone().apply {
                name = name
                    .replace("%CARD_TYPE%", cardType.name)
                    .replace("%PRICE%", price.value.format())
                lore = lore.map {
                    it
                        .replace("%CARD_TYPE%", cardType.name)
                        .replace("%PRICE%", price.value.format())
                }
            }.toGuiItemStack {
                CardProvider.instance.askCardInfo(viewer, cardType, price)
            }

            setItem(priceSlots[i], button)
        }

        unlock()

        // open to viewer
        open()
    }

    private fun buildAsync() = runNotSync { build() }
}