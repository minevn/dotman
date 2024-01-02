package net.minevn.dotman.database.dao.mysql

import net.minevn.dotman.database.dao.PlayerDataDAO

class PlayerDataDAOImpl : PlayerDataDAO {
    override fun insertDataScript() = """
        INSERT INTO `dotman_player_data` (`uuid`, `name`, `key`, `value`, `last_updated`)
        VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE 
            `name` = VALUES(`name`),
            `value` = `value` + VALUES(`value`),
            `last_updated` = VALUES(`last_updated`);
    """.trimIndent()
}