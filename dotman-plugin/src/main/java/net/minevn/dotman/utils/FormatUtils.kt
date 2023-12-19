package net.minevn.dotman.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat

val nonDecimalFormat = DecimalFormat("###,###")
fun formatNonDecimalDouble(value: Double) = nonDecimalFormat.format(value)

val locationFormat = DecimalFormat("###.#")
fun formatLocationDouble(value: Double) = locationFormat.format(value)

fun formatFloat(value: Float) = String.format("%.1f", value)

val dateAndTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
fun getDateAndTime(time: Long) = dateAndTimeFormat.format(time)

fun getTimeString(time: Long): String {
    val totalsecond = time / 1000
    var timeString = ""
    val day = totalsecond / 86400
    val hour = totalsecond % 86400 / 3600
    val min = totalsecond % 86400 % 3600 / 60
    val second = totalsecond % 86400 % 3600 % 60
    if (day != 0L) timeString += "$day ngày "
    if (hour != 0L) timeString += "$hour giờ "
    if (min != 0L) timeString += "$min phút "
    val milis = time % 1000 / 100
    timeString += if (totalsecond <= 0) String.format("%s.%s giây", second, milis) else "$second giây"
    return timeString
}
