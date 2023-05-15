package net.minevn.dotman.gui

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.CardType
import net.minevn.dotman.config.Language
import net.minevn.dotman.database.dao.ConfigDAO
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.guiapi.ConfiguredUI
import net.minevn.guiapi.GuiIcon.Companion.getGuiIcon
import net.minevn.guiapi.GuiItemStack
import net.minevn.libs.bukkit.MineVNLib.Companion.getGuiFillSlots
import net.minevn.libs.bukkit.asLocation
import net.minevn.libs.bukkit.color
import net.minevn.libs.bukkit.runSync
import net.minevn.libs.bukkit.split
import org.bukkit.entity.Player

/**
 * Do UI này cần dummy init để init config nên viewer sẽ null
 * khi thực hiện dummy init.
 */
class CardTypeUI(viewer: Player?) : ConfiguredUI(viewer, "menu/napthe/loaithe.yml", DotMan.instance) {

    constructor() : this(null)

    private val configDao = ConfigDAO.getInstance()
    private val lang = Language.get()

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
                viewer.closeInventory()
            }
        )

        // anouncement button
        setItem(
            config.getInt("info.slot"),
            config.getGuiIcon("info")
                .apply {
                    val message = configDao.get("announcement")?.color() ?: lang.uiNoAnnouncement
                    lore = message.split(32)
                }
                .toGuiItemStack()
        )

        // banking recommendation button
        setItem(
            config.getInt("banking-recommend.slot"),
            config.getGuiIcon("banking-recommend").toGuiItemStack { runNotSync {
                val location = configDao.get("banking-location")?.asLocation() ?: run {
                    viewer.send(lang.uiNoBankingLocation)
                    return@runNotSync
                }
                runSync {
                    viewer.closeInventory()
                    viewer.teleport(location)
                }
            }}
        )

        // cards list
        val cardSlots = config.getGuiFillSlots("cards.fill")
        val activeIcon = config.getGuiIcon("cards.active")
        val disabledIcon = config.getGuiIcon("cards.disabled")
        val blankIcon = config.getGuiIcon("cards.blank")
        val cardTypes = CardType.entries.sortedBy { !it.isActive() }.toTypedArray()
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