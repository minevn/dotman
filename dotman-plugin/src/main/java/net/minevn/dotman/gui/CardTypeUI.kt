package net.minevn.dotman.gui

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.CardType
import net.minevn.dotman.database.ConfigDAO
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.guiapi.ConfiguredUI
import net.minevn.guiapi.GuiIcon.Companion.getGuiIcon
import net.minevn.guiapi.GuiItemStack
import net.minevn.libs.bukkit.MineVNLib.Companion.getGuiFillSlots
import net.minevn.libs.bukkit.color
import net.minevn.libs.bukkit.split
import org.bukkit.entity.Player

/**
 * Do UI này cần dummy init để init config nên viewer sẽ null
 * khi thực hiện dummy init.
 */
class CardTypeUI(viewer: Player?) : ConfiguredUI(viewer, "menu/napthe/loaithe.yml", DotMan.instance) {

    constructor() : this(null)
    private val lang = DotMan.instance.language

    init {
        if (viewer?.isOnline == true) buildAsync()
    }

    private fun build() {
        val configDao = ConfigDAO.getInstance()
        val viewer = viewer!!
        val config = getConfig()
        lock()

        // background
        setItem(
            config.getGuiFillSlots("background.fill"),
            config.getGuiIcon("background").toGuiItemStack()
        )

        // anouncement button
        setItem(
            config.getInt("info.slot"),
            config.getGuiIcon("info")
                .apply {
                    val message = configDao.get("announcement") ?: lang.uiNoAnnouncement
                    lore = message.color().replace("\\n", "\n").split(32)
                }
                .toGuiItemStack()
        )

        // cards list
        val cardSlots = config.getGuiFillSlots("cards.fill")
        val activeIcon = config.getGuiIcon("cards.active")
        val disabledIcon = config.getGuiIcon("cards.disabled")
        val blankIcon = config.getGuiIcon("cards.blank")
        val cardTypes = CardType.entries
            .sortedBy { !it.isActive() }
            .filter { DotMan.instance.config.cardTypes[it] == true }
            .toTypedArray()
        for (i in cardSlots.indices) {
            val slotId = cardSlots[i]
            val button: GuiItemStack
            if (i < cardTypes.size) {
                val cardType = cardTypes[i]
                val icon = (if (cardType.isActive()) activeIcon else disabledIcon).clone().apply {
                    name = name.replace("%CARD_TYPE%", cardType.name)
                    lore = lore.map { it.replace("%CARD_TYPE%", cardType.name) }
                }
                button = icon.toGuiItemStack {
                    if (cardType.isActive()) {
                        CardPriceUI(viewer, cardType)
                    }
                }
            } else {
                button = blankIcon.toGuiItemStack()
            }

            setItem(slotId, button)
        }

        unlock()

        // open to viewer
        open()
    }

    private fun buildAsync() = runNotSync { build() }
}
