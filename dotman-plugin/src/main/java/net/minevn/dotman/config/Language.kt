package net.minevn.dotman.config

class Language : FileConfig("messages") {

    val updateLatest = get("update-latest")
    val updateAvailable = get("update-available")
    val updateAvailableLink = get("update-available-link")
    val errorUnknown = get("error-unknown")
    val errorUnknownCardType = get("error-unknown-card-type")
    val errorUnknownCardPrice = get("error-unknown-card-price")

    val errorSeriTooLong = get("error-seri-too-long")
    val errorPinTooLong = get("error-pin-too-long")

    val cardStatus = get("card-status") // TODO

    val cardCharging = getList("card-charging")
    val cardChargedSuccessfully = getList("card-charged-successfully")
    val cardChargedWithExtra = get("card-charged-with-extra")
    val cardChargedSent = getList("card-charged-sent")
    val cardChargedAnnounce = getList("card-charged-announce")
    val cardChargedFailed = getList("card-charged-failed")
    val cardChargedError = getList("card-charged-error")

    val uiNoAnnouncement = get("ui-no-annoucement")
    val uiNoBankingLocation = get("ui-no-banking-location")

    val inputCancel = get("input-cancel")
    val inputCanceled = get("input-canceled")
    val inputSeri = get("input-seri")
    val inputPin = get("input-pin")
    val inputAnvilSeri = get("input-anvil-seri")
    val inputAnvilPin = get("input-anvil-pin")
    val inputAnvilClick = get("input-anvil-click")

    val logOutPut = get("log-output")
}
