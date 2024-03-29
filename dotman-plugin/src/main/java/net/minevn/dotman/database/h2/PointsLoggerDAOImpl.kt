package net.minevn.dotman.database.h2

import net.minevn.dotman.database.PointsLoggerDAO

class PointsLoggerDAOImpl : PointsLoggerDAO() {
    override fun insertLogScript() = """
            insert into "dotman_point_log"
                ("name", "uuid", "amount", "point_from", "point_to", "time", "server", "content")
            values (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
}
