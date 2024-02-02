package net.minevn.dotman.database.mysql

import net.minevn.dotman.database.PointsLoggerDAO

class PointsLoggerDAOImpl : PointsLoggerDAO() {
    override fun insertLogScript() = """
            INSERT INTO `dotman_point_log`
                (`name`, `uuid`, `amount`, `point_from`, `point_to`, `time`, `server`, `content`)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()
}
