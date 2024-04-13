package net.minevn.dotman.database.h2

import net.minevn.dotman.database.PlayerInfoDAO

class PlayerInfoDAOImpl : PlayerInfoDAO() {
    override fun updateScript() = """
        MERGE INTO "dotman_player_info" AS target
            USING (
                VALUES (?, ?, ?)
                ) AS source ("uuid", "name", "last_updated")
        ON target."uuid" = source."uuid"
        WHEN MATCHED THEN
            UPDATE SET
                       target."name" = source."name",
                       target."last_updated" = source."last_updated"
        WHEN NOT MATCHED THEN
            INSERT ("uuid", "name", "last_updated")
            VALUES (source."uuid", source."name", source."last_updated");
    """.trimIndent()

    override fun getUUIDScript() = """
        SELECT "uuid" FROM "dotman_player_info" WHERE "name" = ?
    """.trimIndent()

}
