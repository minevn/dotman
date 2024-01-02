package net.minevn.dotman.database.dao.h2

import net.minevn.dotman.database.dao.PlayerDataDAO

class PlayerDataDAOImpl : PlayerDataDAO {
    override fun insertDataScript() = """
        MERGE INTO "dotman_player_data" USING (
            SELECT ?, ?, ?, ?, ? FROM DUAL
        ) AS source ("uuid", "name", "key", "value", "last_updated")
        ON "dotman_player_data"."uuid" = source."uuid" AND "dotman_player_data"."key" = source."key"
        WHEN MATCHED THEN
            UPDATE SET
                       "dotman_player_data"."name" = source."name",
                       "dotman_player_data"."value" = "dotman_player_data"."value" + source."value",
                       "dotman_player_data"."last_updated" = source."last_updated"
        WHEN NOT MATCHED THEN
            INSERT ("uuid", "name", "key", "value", "last_updated")
            VALUES (source."uuid", source."name", source."key", source."value", source."last_updated");
    """.trimIndent()
}