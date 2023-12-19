package net.minevn.dotman.card

class CardWaiting(
    val id: Int,
    val uuid: String,
    val seri: String,
    val type: String,
    val price: Int
) {
    var isSuccess: Boolean = false
    var message: String? = null

    fun toCard() = Card(seri, "", CardPrice[price]!!, CardType[type]!!)
}