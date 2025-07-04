package net.minevn.dotman.providers.types

import net.minevn.dotman.card.*
import net.minevn.dotman.database.LogDAO
import net.minevn.dotman.providers.CardProvider
import net.minevn.dotman.utils.Utils.Companion.md5
import net.minevn.libs.asIntOrNull
import net.minevn.libs.asStringOrNull
import net.minevn.libs.bukkit.sendMessages
import net.minevn.libs.get
import net.minevn.libs.parseJson
import org.bukkit.entity.Player

class Card2KCP(private val partnerId: String, private val partnerKey: String) : CardProvider() {

    init {
        statusCards = CardType.entries.filter { it.getTypeId() != null }.associateWith { true }
    }

    override fun getApiUrl() = "https://card2k.com/chargingws/v2"

    override fun getRequestParameters(playerName: String, card: Card) = mapOf(
        "partner_id" to partnerId,
        "request_id" to ('A'..'Z').let {
            charpool -> (1..10).map { charpool.random() }.joinToString("") + (0..9).random()
        },
        "code" to card.pin,
        "serial" to card.seri,
        "telco" to card.type.getTypeId()!!,
        "amount" to card.price.value.toString(),
        "sign" to (partnerKey + card.pin + card.seri).md5(),
        "command" to "charging"
    )

    override fun parseResponse(card: Card, response: String) = CardResult(card).apply {
        println(response)
        response.parseJson().asJsonObject.let {
            isSuccess = it["status"].asStringOrNull() == "1" || it["status"].asStringOrNull() == "99"
            message = it["message"].asStringOrNull()

            // Cập nhật mã giao dịch
            it["request_id"].asStringOrNull()?.let { transactionId ->
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
        CardType.ZING,
        CardType.GARENA,
        CardType.VCOIN -> name
        CardType.VIETNAMOBILE -> "VNMOBI"

        else -> null
    }

    private fun CardPrice.getPriceId() = (CardPrice.entries.indexOf(this) + 1).toString()

    override fun CardWaiting.isProcessed(): Boolean {
        val card = toCard()
        val params = mapOf(
            "partner_id" to partnerId,
            "request_id" to transactionId,
            "telco" to card.type.getTypeId()!!,
            "amount" to price.toString(),
            "code" to card.pin,
            "serial" to card.seri,
            "sign" to (partnerKey + card.pin + card.seri).md5(),
            "command" to "check"
        )

        return get(getApiUrl(), parameters = params)
            .parseJson()
            .asJsonObject
            .let {
                val status = it["status"].asIntOrNull() ?: return@let false
                isSuccess = status == 1
                message = when (status) {
                    1 -> null
                    100 -> "Thẻ sai hoặc đã sử dụng"
                    3 -> "Lỗi hệ thống, vui lòng liên hệ Admin để được hỗ trợ!"
                    2 -> "Bạn đã nhập sai mệnh giá, thẻ của bạn đã bị mất."
                    else -> "Lỗi không xác định (status: $status)"
                }
                status != 99
            }
    }

    override fun updateStatus() = checkWaitingCards()
}
