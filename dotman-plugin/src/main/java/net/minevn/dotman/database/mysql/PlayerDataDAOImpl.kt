package net.minevn.dotman.database.mysql

import net.minevn.dotman.database.PlayerDataDAO

class PlayerDataDAOImpl : PlayerDataDAO() {
    override fun insertDataScript() = """
        INSERT INTO `dotman_player_data` (`uuid`, `name`, `key`, `value`, `last_updated`)
        VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE 
            `name` = VALUES(`name`),
            `value` = `value` + VALUES(`value`),
            `last_updated` = VALUES(`last_updated`);
    """.trimIndent()

    override fun getTopScript(): String = """
        SELECT
            row_number() over (order by `value` desc) as rownum,
            d.uuid, ifnull(i.name, d.name) as name, `key`, value
        FROM `dotman_player_data` d
        LEFT JOIN `dotman_player_info` i ON d.uuid = i.uuid
        WHERE `key` = ?
        ORDER BY `value` DESC
        LIMIT ?;
    """.trimIndent()

    override fun getDataScript() = """
        SELECT `value`
        FROM `dotman_player_data`
        WHERE `uuid` = ? AND `key` = ?;
    """.trimIndent()

    override fun getSumDataScript() = """
        SELECT SUM(`value`) as `value`
        FROM `dotman_player_data`
        WHERE `key` = ?;
    """.trimIndent()

    /**
     * Returns an SQL script that selects all key/value pairs for a specific player UUID.
     *
     * @return A prepared-statement SQL string: selects `key` and `value` from `dotman_player_data` where `uuid` = ?.
     */
    override fun getAllDataScript() = """
        SELECT `key`, `value`
        FROM `dotman_player_data`
        WHERE `uuid` = ?;
    """.trimIndent()

    /**
     * Returns a prepared DELETE SQL script that removes rows from `dotman_player_data`.
     *
     * The script deletes entries for a specific `uuid` where the `key` matches a SQL pattern
     * (uses `LIKE`). It uses prepared-statement placeholders in this order: `uuid`, `keyPattern`.
     *
     * @return A SQL string with `?` placeholders for `uuid` and the `key` pattern.
     */
    override fun deleteDataByKeyLikeScript() = """
        DELETE FROM `dotman_player_data`
        WHERE `uuid` = ? AND `key` LIKE ?;
    """.trimIndent()
}
