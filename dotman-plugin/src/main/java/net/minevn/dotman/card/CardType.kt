package net.minevn.dotman.card

import net.minevn.dotman.DotMan
import net.minevn.dotman.providers.CardProvider

enum class CardType(vararg val alternative: String = emptyArray()) {
    VIETTEL,
    MOBIFONE("mobiphone"),
    VINAPHONE,
    GATE,
    VCOIN,
    VIETNAMOBILE("vn-mobile"),
    ZING,
    GARENA,
    MEGACARD,
    ONCASH,
    ;

    fun isActive() = CardProvider.instance.getStatus(this)

    companion object {
        operator fun get(name: String) = (runCatching { valueOf(name.uppercase()) }.getOrNull()
            ?: entries.find { it.alternative.contains(name.lowercase()) })
            .takeIf { DotMan.instance.config.cardTypes[it] == true }
    }
}
