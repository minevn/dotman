package net.minevn.dotman.database.mysql

import net.minevn.dotman.database.ConfigDAO

class ConfigDAOImpl : ConfigDAO() {
    override fun isTableExistsScript() = "SHOW TABLES LIKE 'dotman_config'"

    override fun getScript() = "SELECT * FROM dotman_config WHERE `key` = ?"

    override fun setScript() =
        "INSERT INTO dotman_config (`key`, `value`) VALUES (?, @value:=?) ON DUPLICATE KEY UPDATE `value` = @value"

    override fun deleteScript() = "DELETE FROM dotman_config WHERE `key` = ?"
}
