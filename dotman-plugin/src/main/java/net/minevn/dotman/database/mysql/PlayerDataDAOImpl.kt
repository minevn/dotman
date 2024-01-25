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
            row_number() over (order by time desc) as rownum,
            uuid, name, key, value
        FROM `dotman_player_data`
        WHERE `key` = ?
        ORDER BY `value` DESC
        LIMIT ?;
    """.trimIndent()

    override fun getDataScript() = """
        SELECT `value`
        FROM `dotman_player_data`
        WHERE `uuid` = ? AND `key` = ?;
    """.trimIndent()
}