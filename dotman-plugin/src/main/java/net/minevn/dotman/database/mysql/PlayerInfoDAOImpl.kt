package net.minevn.dotman.database.mysql

import net.minevn.dotman.database.PlayerInfoDAO

class PlayerInfoDAOImpl : PlayerInfoDAO() {
    override fun updateScript() = """
        insert into dotman_player_info (uuid, name, last_updated)
        values (@uuid:=?, @name:=?, @time:=?)
        on duplicate key update name = @name, last_updated = @time;
    """.trimIndent()
}