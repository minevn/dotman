package net.minevn.dotman.providers

import net.minevn.dotman.DotMan
import net.minevn.dotman.TOP_KEY_DONATE_TOTAL
import net.minevn.dotman.TOP_KEY_POINT_FROM_CARD
import net.minevn.dotman.card.Card
import net.minevn.dotman.card.CardPrice
import net.minevn.dotman.card.CardResult
import net.minevn.dotman.card.CardType
import net.minevn.dotman.database.LogDAO
import net.minevn.dotman.database.PlayerDataDAO
import net.minevn.dotman.providers.types.GameBankCP
import net.minevn.dotman.providers.types.TheSieuTocCP
import net.minevn.dotman.utils.Utils.Companion.runNotSync
import net.minevn.dotman.utils.Utils.Companion.send
import net.minevn.dotman.utils.Utils.Companion.warning
import net.minevn.libs.bukkit.chat.ChatListener
import net.minevn.libs.bukkit.sendMessages
import net.minevn.libs.post
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

abstract class CardProvider {
    val task = Bukkit.getScheduler()
        .runTaskTimerAsynchronously(DotMan.instance, ::updateStatus, 0L, 20 * 60)!!

    companion object {
        lateinit var instance: CardProvider private set

        fun init(provider: String, config: YamlConfiguration) {
            if (::instance.isInitialized) {
                instance.task.cancel()
            }

            instance = when (provider) {
                "thesieutoc" -> {
                    val apiKey = config.getString("api-key")
                    val apiSecret = config.getString("api-secret")
                    TheSieuTocCP(apiKey, apiSecret)
                }

                "gamebank" -> {
                    val merchantID = config.getInt("merchant_id")
                    val apiUser = config.getString("api_user")
                    val apiPassword = config.getString("api_password")
                    GameBankCP(merchantID, apiUser, apiPassword)
                }

                else -> {
                    throw IllegalArgumentException("Provider $provider not found")
                }
            }
        }
    }

    protected val main = DotMan.instance
    abstract fun getApiUrl(): String
    open fun getStatusUrl() = ""
    protected var statusCards = CardType.entries.associateWith { true }

    fun processCard(player: Player, card: Card) {
        val lang = main.language
        player.sendMessages(lang.cardCharging.map {
            it  .replace("%CARD_TYPE%", card.type.name)
                .replace("%CARD_PRICE%", card.price.value.toString())
                .replace("%SERI%", card.seri)
                .replace("%CODE%", card.pin)
        })

        runNotSync {
            runCatching {
                val log = LogDAO.getInstance()
                card.logId = log.insertLog(player, card)
                val result = doRequest(player.name, card)

                if (result.isSuccess) {
                    onRequestSuccess(player, result)
                } else {
                    player.sendMessages(lang.cardChargedFailed.map {
                        it.replace("%ERROR%", result.message ?: lang.errorUnknown)
                    })
                }
            }.onFailure {
                it.warning("Loi nap the: ${it.message}")
                player.sendMessages(lang.cardChargedError)
            }
        }
    }

    open fun onRequestSuccess(player: Player, result: CardResult) {
        onChargeSuccess(player, result.card)
    }

    protected open fun onChargeSuccess(player: Player, card: Card) {
        var amount = card.price.getPointAmount()
        val config = main.config
        val extraRate = config.extraRate
        var extraPercent = 0
        if (extraRate > 0 && config.extraUntil > System.currentTimeMillis()) {
            amount += (amount * config.extraRate).toInt()
            extraPercent = (extraRate * 100).toInt()
        }
        DotMan.instance.playerPoints.api.give(player.uniqueId, amount)
        player.sendMessages(
            main.language.cardChargedSuccessfully.map {
                it  .replace("%AMOUNT%", amount.toString())
                    .replace("%POINT_UNIT%", main.config.pointUnit)
            }
        )
        if (config.announceCharge) {
            main.language.cardChargedAnnounce.forEach {
                Bukkit.broadcastMessage(it
                    .replace("%PLAYER%", player.name)
                    .replace("%AMOUNT%", amount.toString())
                    .replace("%POINT_UNIT%", main.config.pointUnit)
                )
            }
        }
        if (extraPercent > 0) {
            player.send(main.language.cardChargedWithExtra.replace("%RATE%", extraPercent.toString()))
        }
        if (card.logId != null) {
            LogDAO.getInstance().updatePointReceived(card.logId!!, amount)
        }

        val dataDAO = PlayerDataDAO.getInstance()

        // Tích điểm
        dataDAO.insertAllType(player, TOP_KEY_DONATE_TOTAL, card.price.value)
        dataDAO.insertAllType(player, TOP_KEY_POINT_FROM_CARD, amount)

        // Mốc nạp
        main.minestones.getAll().filter { it.type == "all" }.forEach {
            it.check(player, dataDAO.getData(player, "${TOP_KEY_DONATE_TOTAL}_ALL"), card.price.value)
        }
    }

    /**
     * Tiến hành nạp thẻ
     *
     * (nên chạy trong luồng khác)
     *
     * @param playerName Tên người chơi
     * @param card Thông tin thẻ cào
     */
    open fun doRequest(playerName: String, card: Card): CardResult {
        val parameters = getRequestParameters(playerName, card)
        val headers = getRequestHeaders(playerName, card)
        return parseResponse(card, post(getApiUrl(), parameters = parameters, headers = headers))
    }

    protected abstract fun parseResponse(card: Card, response: String): CardResult

    protected abstract fun getRequestParameters(playerName: String, card: Card): Map<String, String>

    protected open fun getRequestHeaders(playerName: String, card: Card): Map<String, String>? = null

    fun getStatus(type: CardType) = statusCards.getOrDefault(type, false)

    fun getAvailableCardType() = statusCards.filter { it.value }.keys

    fun askCardInfo(player: Player, type: CardType, price: CardPrice) {
        val lang = main.language
        player.send(lang.inputSeri)
        player.send(lang.inputCancel)

        ChatListener(player) seri@{
            if (message.equals("huy", true)) {
                player.send(lang.inputCanceled)
                return@seri
            }

            val seri = message

            player.send(lang.inputPin)
            player.send(lang.inputCancel)

            ChatListener(player) pin@{
                if (message.equals("huy", true)) {
                    player.send(lang.inputCanceled)
                    return@pin
                }
                val pin = message

                val card = Card(seri, pin, price, type)

                runNotSync {
                    processCard(player, card)
                }
            }
        }
    }

    protected open fun updateStatus() {
    }
}
