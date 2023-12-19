package net.minevn.dotman.card

class CardResult(
    val card: Card,
    var acceptedAmount: Int? = null,
    var isSuccess: Boolean = false,
    var message: String? = null,
) {
    override fun toString() =
        "CardResult(card=$card, acceptedAmount=$acceptedAmount, isSuccess=$isSuccess, message=$message)"
}