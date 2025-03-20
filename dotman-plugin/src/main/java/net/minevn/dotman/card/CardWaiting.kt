package net.minevn.dotman.card

class CardWaiting(
    val id: Int,
    val uuid: String,
    val seri: String,
    val pin: String = "",
    val type: String,
    val price: Int,
    val transactionId: String
) {
    var isSuccess: Boolean = false
    var message: String? = null

    fun toCard() = Card(seri, pin, CardPrice[price]!!, CardType[type]!!).apply { logId = id }
}