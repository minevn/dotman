package net.minevn.dotman.config

import net.minevn.libs.bukkit.color

class Language : FileConfig("messages") {

    companion object {
        private lateinit var instance: Language

        fun get(): Language {
            if (!::instance.isInitialized) {
                instance = Language()
            }
            return instance
        }

        fun reload() {
            instance = Language()
        }
    }

    fun get(key: String): String = config.getString(key, "").color()

    fun getList(key: String) = config.getStringList(key).map {
        it.replace("%PREFIX%", MainConfig.get().prefix)
    }.color()

    val errorUnknown = get("error-unknown")
    val errorUnknownCardType = get("error-unknown-card-type")
    val errorUnknownCardPrice = get("error-unknown-card-price")

    val cardStatus = get("card-status") // TODO

    val cardCharging = getList("card-charging")
    val cardChargedSuccessfully = getList("card-charged-successfully")
    val cardChargedWithExtra = get("card-charged-with-extra")
    val cardChargedSent = getList("card-charged-sent")
    val cardChargedFailed = getList("card-charged-failed")
    val cardChargedError = getList("card-charged-error")

    val uiNoAnnouncement = get("ui-no-annoucement")
    val uiNoBankingLocation = get("ui-no-banking-location")

    val inputCancel = get("input-cancel")
    val inputCanceled = get("input-canceled")
    val inputSeri = get("input-seri")
    val inputPin = get("input-pin")
}
