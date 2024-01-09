package net.minevn.dotman

private class Top(
    val list: Map<String, Int>,
    val expire: Long
) {
    fun isExpired() = System.currentTimeMillis() > expire
}

const val TOP_EXPIRE = 10 * 60 * 1000L

private val topCache = mutableMapOf<String, Top>()