package net.minevn.dotman.utils

import net.minevn.libs.getWeekOfYear
import java.time.LocalDate
import java.util.*

fun getMonthKey() = LocalDate.now().run {
    val month = monthValue
    val year = year
    "${year}_m${month.toString().padStart(2, '0')}"
}

enum class TopType(val keyExtractor: (String) -> String) {
    ALL_TIME({ "${it}_ALL" }),
    MONTH({ "${it}_${getMonthKey()}" }),
    WEEK({ "${it}_${getWeekOfYear(Date())}" }),
    ;

    fun parseKey(key: String) = keyExtractor(key)
}