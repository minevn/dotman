package net.minevn.dotman.database

import net.minevn.dotman.DotMan
import net.minevn.libs.db.DataAccess

abstract class PlayerInfoDAO : DataAccess() {
    companion object {
        fun getInstance() = DotMan.instance.getDAO(PlayerInfoDAO::class)
    }

    abstract fun updateScript(): String

    fun updateData(uuid: String, name: String) {
        updateScript().statement {
            setString(1, uuid)
            setString(2, name)
            setLong(3, System.currentTimeMillis())

            executeUpdate()
        }
    }
}