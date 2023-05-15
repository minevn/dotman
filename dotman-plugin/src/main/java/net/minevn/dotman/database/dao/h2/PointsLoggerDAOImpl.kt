package net.minevn.dotman.database.dao.h2

import net.minevn.dotman.database.dao.PointsLoggerDAO

class PointsLoggerDAOImpl : PointsLoggerDAO {
    override fun insertLogScript() = """
            insert into "dotman_point_log"
                ("name", "uuid", "amount", "point_from", "point_to", "time", "server", "content")
            values (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
}
