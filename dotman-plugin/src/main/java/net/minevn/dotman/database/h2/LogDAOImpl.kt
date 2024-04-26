package net.minevn.dotman.database.h2

import net.minevn.dotman.database.LogDAO

class LogDAOImpl : LogDAO() {
    override fun insertLogScript() = """
            INSERT INTO "dotman_napthe_log"
                ("name", "uuid", "seri", "pin", "type", "price", "time", "server")
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

    override fun setWaitingScript() = """UPDATE "dotman_napthe_log" SET "waiting" = 1 WHERE "id" = ?"""

    override fun stopWaitingScript() = """UPDATE "dotman_napthe_log" SET "waiting" = 0, "success" = ? WHERE "id" = ?"""

    override fun getWaitingCardsScript() =
        """SELECT * FROM "dotman_napthe_log" WHERE "server" = ? AND "waiting" = 1 AND "uuid" IN ({UUIDS})"""

    override fun setTransactionIdScript() = """
            UPDATE "dotman_napthe_log" 
            set "transaction_id" = ?,  "success" = ?
            where "id" = ?
        """.trimIndent()

    override fun getHistoryScriptAllPlayerAllTime() = """
            SELECT
                ROW_NUMBER() OVER (ORDER BY "time" DESC) AS "rownum",
                "id", i."name" as uuid, "type", "seri", "price", "pointsnhan", "time"
            FROM "dotman_napthe_log" l left join "dotman_player_info" i on i."uuid" = l."uuid"
            WHERE "success" = 1
            ORDER BY "time" DESC
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
        """.trimIndent()

    override fun getHistoryScriptAllPlayerByMonth() = """
            SELECT
                ROW_NUMBER() OVER (ORDER BY "time" DESC) AS "rownum",
                "id", i."name" as uuid, "type", "seri", "price", "pointsnhan", "time"
            FROM "dotman_napthe_log" l left join "dotman_player_info" i on i."uuid" = l."uuid"
            WHERE "success" = 1
            and "time" >= ? and "time" <= ?
            ORDER BY "time" DESC
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
        """.trimIndent()

    override fun getHistoryScriptByPlayerAllTime() = """
            SELECT
                ROW_NUMBER() OVER (ORDER BY "time" DESC) AS "rownum",
                "id", i."name" as uuid, "type", "seri", "price", "pointsnhan", "time"
            FROM "dotman_napthe_log" l left join "dotman_player_info" i on i."uuid" = l."uuid"
            WHERE "success" = 1 AND "name" = ?
            ORDER BY "time" DESC
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
        """.trimIndent()

    override fun getHistoryScriptByPlayerByMonth() = """
            SELECT
                ROW_NUMBER() OVER (ORDER BY "time" DESC) AS "rownum",
                "id", i."name" as uuid, "type", "seri", "price", "pointsnhan", "time"
            FROM "dotman_napthe_log" l left join "dotman_player_info" i on i."uuid" = l."uuid"
            WHERE "success" = 1 AND "name" = ?
            and "time" >= ? and "time" <= ?
            ORDER BY "time" DESC
            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
        """.trimIndent()

    override fun getSumScriptAllPlayerAllTime() = """
        SELECT
            SUM("price"), COUNT("id")
        FROM "dotman_napthe_log"
        WHERE "success" = 1
    """.trimIndent()

    override fun getSumScriptAllPlayerByMonth() = """
        SELECT
            SUM("price"), COUNT("id")
        FROM "dotman_napthe_log"
        WHERE "success" = 1
        and "time" >= ? and "time" <= ?
    """.trimIndent()

    override fun getSumScriptByPlayerAllTime() = """
        SELECT
            SUM("price"), COUNT("id")
        FROM "dotman_napthe_log"
        WHERE "success" = 1 AND "name" = ?
    """.trimIndent()

    override fun getSumScriptByPlayerByMonth() = """
        SELECT
            SUM("price"), COUNT("id")
        FROM "dotman_napthe_log"
        WHERE "success" = 1 AND "name" = ?
        and "time" >= ? and "time" <= ?
    """.trimIndent()

    override fun updatePointReceivedScript() = """
            UPDATE "dotman_napthe_log"
            SET "pointsnhan" = ?
            WHERE "id" = ?;
        """.trimIndent()

    override fun updateTimeScript() = """
            UPDATE `dotman_napthe_log`
            SET `time` = ?
            WHERE `id` = ?;
    """.trimIndent()
}
