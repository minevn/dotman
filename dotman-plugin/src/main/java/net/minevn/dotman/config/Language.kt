package net.minevn.dotman.config

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

    val logOutPut = get("log-output")
}
