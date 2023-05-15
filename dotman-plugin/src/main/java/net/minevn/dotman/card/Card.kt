package net.minevn.dotman.card

class Card(var seri: String, var pin: String, val price: CardPrice, val type: CardType) {
	var logId: Int? = null

	override fun toString() = "Card(seri=$seri, pin=$pin, price=$price, type=$type)"
}