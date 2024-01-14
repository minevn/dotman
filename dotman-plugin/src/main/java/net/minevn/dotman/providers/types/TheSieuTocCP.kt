package net.minevn.dotman.providers.types

import net.minevn.dotman.card.*
import net.minevn.dotman.database.dao.LogDAO
import net.minevn.dotman.providers.CardProvider
import net.minevn.libs.bukkit.getOrNull
import net.minevn.libs.bukkit.parseJson
import net.minevn.libs.bukkit.sendMessages
import net.minevn.libs.get
import org.bukkit.Bukkit
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
        "content" to "dotman_${card.logId}"
    )

    override fun parseResponse(card: Card, response: String) = CardResult(card).apply {
        println(response)
        response.parseJson().asJsonObject.let {
            isSuccess = it["status"].getOrNull()?.asString == "00"
            message = it["msg"].getOrNull()?.asString
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

    private fun CardWaiting.getTransactId() = "dotman_$id"

    private fun CardWaiting.isProcessed(): Boolean {
        val params = mapOf(
            "APIkey" to apiKey,
            "APIsecret" to apiSecret,
            "transaction_id" to getTransactId()
        )

        return get(getCardCheckUrl(), parameters = params)
            .parseJson()
            .asJsonObject
            .let {
                val status = it["status"].getOrNull()?.asString
                message = it["msg"].getOrNull()?.asString
                isSuccess = status == "00"
                status != "-9"
            }
    }

    override fun updateStatus() {
        val players = Bukkit.getOnlinePlayers().associateBy { it.uniqueId.toString() }
        val uuids = players.keys.toTypedArray()
        val server = main.config.server
        val cardLogger = LogDAO.getInstance()
        val lang = main.language

        cardLogger.getWaitingCards(uuids, server)
            ?.filter { it.isProcessed() }
            ?.forEach {
                val player = players[it.uuid]
                if (player?.isOnline != true) return@forEach

                cardLogger.stopWaiting(it.id, it.isSuccess)

                if (it.isSuccess) {
                    onChargeSuccess(player, it.toCard())
                } else {
                    val message = it.message ?: lang.errorUnknown
                    player.sendMessages(lang.cardChargedFailed.map { str ->
                        str.replace("%ERROR%", message)
                    })
                }
            }
    }
}
