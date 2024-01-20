package net.minevn.dotman.utils

enum class TopType(val keyExtractor: (String) -> String) {
    ALL({ "${it}_ALL" }),
    ;

    fun parseKey(key: String) = keyExtractor(key)
}