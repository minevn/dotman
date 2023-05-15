package net.minevn.dotman.card

import net.minevn.dotman.config.MainConfig

enum class CardPrice(val value: Int) {
	CP_10K(10000),
	CP_20K(20000),
	CP_30K(30000),
	CP_50K(50000),
	CP_100K(100000),
	CP_200K(200000),
	CP_300K(300000),
	CP_500K(500000),
	CP_1000K(1000000);

	fun getPointAmount() = MainConfig.get().amounts[this]!!

	companion object {
		operator fun get(value: Int): CardPrice? = entries.find { type -> type.value == value }
	}
}
