package net.minevn.dotman.providers.types

import net.minevn.dotman.card.*
import net.minevn.dotman.database.LogDAO
import net.minevn.dotman.providers.CardProvider
import net.minevn.libs.bukkit.sendMessages
import net.minevn.libs.get
import net.minevn.libs.parseJson
import org.bukkit.entity.Player

class TheSieuTocCP(private val apiKey: String, private val apiSecret: String) : CardProvider() {

    init {
        statusCards = CardType.entries.filter { it.getTypeId() != null }.associateWith { true }
    }

    override fun getApiUrl() = "https://thesieutoc.net/API/transaction"

    private fun getCardCheckUrl() = "https://thesieutoc.net/API/get_status_card.php"

    override fun getRequestParameters(playerName: String, card: Card) = mapOf(
        "APIkey" to apiKey,
        "APIsecret" to apiSecret,
        "mathe" to card.pin,
        "seri" to card.seri,
        "type" to card.type.getTypeId()!!,
        "menhgia" to card.price.getPriceId(),
        "content" to ('A'..'Z').let { charpool -> (1..8).map { charpool.random() }.joinToString("") }
    )

    override fun parseResponse(card: Card, response: String) = CardResult(card).apply {
        println(response)
        response.parseJson().asJsonObject.let {
            isSuccess = it["status"].asString == "00"
            message = it["msg"].asString

            // Cập nhật mã giao dịch
            it["transaction_id"]?.asString?.let { transactionId ->
                LogDAO.getInstance().setTransactionId(card.logId!!, transactionId, false)
            }
        }
    }

    override fun onRequestSuccess(player: Player, result: CardResult) {
        LogDAO.getInstance().setWaiting(result.card.logId!!)
        player.sendMessages(main.language.cardChargedSent)
    }

    private fun CardType.getTypeId() = when (this) {
        CardType.VIETTEL,
        CardType.MOBIFONE,
        CardType.VINAPHONE,
        CardType.GATE,
        CardType.VIETNAMOBILE,
        CardType.ZING,
        CardType.GARENA,
        CardType.VCOIN -> name.lowercase().replaceFirstChar { it.uppercase() }

        else -> null
    }

    private fun CardPrice.getPriceId() = (CardPrice.entries.indexOf(this) + 1).toString()

    override fun CardWaiting.isProcessed(): Boolean {
        val params = mapOf(
            "APIkey" to apiKey,
            "APIsecret" to apiSecret,
            "transaction_id" to transactionId
        )

        return get(getCardCheckUrl(), parameters = params)
            .parseJson()
            .asJsonObject
            .let {
                val status = it["status"].asString
                message = it["msg"].asString
                isSuccess = status == "00"
                status != "-9"
            }
    }

    override fun updateStatus() = checkWaitingCards()
}
