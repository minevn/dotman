package net.minevn.dotman.providers.types

import net.minevn.dotman.card.*
import net.minevn.dotman.database.LogDAO
import net.minevn.dotman.providers.CardProvider
import net.minevn.dotman.utils.Utils.Companion.md5
import net.minevn.libs.asStringOrNull
import net.minevn.libs.bukkit.sendMessages
import net.minevn.libs.get
import net.minevn.libs.parseJson
import org.bukkit.entity.Player
import kotlin.random.Random

class Card2KCP(private val partnerId: String, private val partnerKey: String) : CardProvider() {

    init {
        statusCards = CardType.entries.filter { it.getTypeId() != null }.associateWith { true }
    }

    override fun getApiUrl() = "https://card2k.com/chargingws/v2"

    private fun getCardCheckUrl() = "https://card2k.com/api/checkCard"

    override fun getRequestParameters(playerName: String, card: Card) = mapOf(
        "partner_id" to partnerId,
        "request_id" to ('A'..'Z').let { charpool -> (1..8).map { charpool.random() }.joinToString("") },
        "code" to card.pin,
        "serial" to card.seri,
        "telco" to card.type.getTypeId()!!,
        "amount" to card.price.getPriceId(),
        "sign" to (partnerKey + card.pin + card.seri).md5(),
        "command" to "charging"
    )

    override fun parseResponse(card: Card, response: String) = CardResult(card).apply {
        println(response)
        response.parseJson().asJsonObject.let {
            isSuccess = it["status"].asStringOrNull() == "1"
            message = it["message"].asStringOrNull()

            // Cập nhật mã giao dịch
            it["transaction_id"].asStringOrNull()?.let { transactionId ->
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
        CardType.VIETNAMOBILE -> "VNMOBI"
        CardType.ZING,
        CardType.GARENA,
        CardType.VCOIN -> name.lowercase().replaceFirstChar { it.uppercase() }

        else -> null
    }

    private fun CardPrice.getPriceId() = (CardPrice.entries.indexOf(this) + 1).toString()

    override fun CardWaiting.isProcessed(): Boolean {
        val card = toCard()
        val params = mapOf(
            "partner_id" to partnerId,
            "request_id" to uuid,
            "telco" to card.type.getTypeId()!!,
            "amount" to price.toString(),
            "code" to seri,
            "serial" to type,
            "sign" to (partnerKey + card.pin + card.seri).md5()
        )

        return get(getCardCheckUrl(), parameters = params)
            .parseJson()
            .asJsonObject
            .let {
                val status = it["status"].asStringOrNull()
                message = it["message"].asStringOrNull()
                isSuccess = status == "00"
                status != "-9"
            }
    }

    override fun updateStatus() = checkWaitingCards()
}
