package net.minevn.dotman.database.dao.h2

import net.minevn.dotman.database.dao.PlayerDataDAO

class PlayerDataDAOImpl : PlayerDataDAO {
    override fun insertDataScript() = """
        MERGE INTO "dotman_player_data" AS target
        USING (
            VALUES (?, ?, ?, ?, ?)
        ) AS source ("uuid", "name", "key", "value", "last_updated")
        ON target."uuid" = source."uuid" AND target."key" = source."key"
        WHEN MATCHED THEN
            UPDATE SET
                target."name" = source."name",
                target."value" = target."value" + source."value",
                target."last_updated" = source."last_updated"
        WHEN NOT MATCHED THEN
            INSERT ("uuid", "name", "key", "value", "last_updated")
            VALUES (source."uuid", source."name", source."key", source."value", source."last_updated");
    """.trimIndent()

    override fun getTopScript(): String = """
        SELECT 
            ROW_NUMBER() OVER (ORDER BY "value" DESC) AS "rownum",
            "uuid", "name", "key", "value"
        FROM "dotman_player_data"
        WHERE "key" = ?
        ORDER BY "value" DESC
        LIMIT ?;
    """.trimIndent()

    override fun getDataScript() = """
        SELECT "value"
        FROM "dotman_player_data"
        WHERE "uuid" = ? AND "key" = ?;
    """.trimIndent()
}