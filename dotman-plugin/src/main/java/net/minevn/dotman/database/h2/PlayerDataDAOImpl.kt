package net.minevn.dotman.database.h2

import net.minevn.dotman.database.PlayerDataDAO

class PlayerDataDAOImpl : PlayerDataDAO() {
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
            d."uuid", COALESCE(i."name", d."name") as "name", "key", "value"
        FROM "dotman_player_data" d
        LEFT JOIN "dotman_player_info" i ON d."uuid" = i."uuid"
        WHERE "key" = ?
        ORDER BY "value" DESC
        LIMIT ?;
    """.trimIndent()

    override fun getDataScript() = """
        SELECT "value"
        FROM "dotman_player_data"
        WHERE "uuid" = ? AND "key" = ?;
    """.trimIndent()

    override fun getSumDataScript() = """
        SELECT SUM("value") as "value"
        FROM "dotman_player_data"
        WHERE "key" = ?;
    """.trimIndent()

    /**
     * Returns an H2 SQL query that selects all data keys and their values for a single player.
     *
     * The returned query expects one bind parameter: the player's UUID (for the WHERE "uuid" = ? clause).
     *
     * @return SQL string that selects "key" and "value" from "dotman_player_data" filtered by UUID.
     */
    override fun getAllDataScript() = """
        SELECT "key", "value"
        FROM "dotman_player_data"
        WHERE "uuid" = ?;
    """.trimIndent()

    /**
     * Returns an SQL script that deletes player data rows matching a UUID and a key pattern.
     *
     * The returned statement uses two placeholders: the first for the player's UUID, the second for a `LIKE` pattern
     * to match the `"key"` column (e.g. `'score_%'`).
     *
     * @return The SQL DELETE statement as a String.
     */
    override fun deleteDataByKeyLikeScript() = """
        DELETE FROM "dotman_player_data"
        WHERE "uuid" = ? AND "key" LIKE ?;
    """.trimIndent()
}
