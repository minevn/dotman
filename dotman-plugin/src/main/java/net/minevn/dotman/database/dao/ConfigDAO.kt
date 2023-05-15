package net.minevn.dotman.database.dao

import net.minevn.dotman.database.DataAccess
import net.minevn.dotman.database.fetch
import net.minevn.dotman.database.getInstance
import net.minevn.dotman.database.statement

interface ConfigDAO : DataAccess {
    companion object {
        fun getInstance() = ConfigDAO::class.getInstance()
    }

    // region scripts
    fun isTableExistsScript(): String
    fun getScript(): String
    fun setScript(): String
    fun deleteScript(): String
    // endregion

    // region queriers
    fun isTableExists() = isTableExistsScript().statement {
        executeQuery().use { it.next() }
    }

    fun get(key: String) = if (!isTableExists()) null else getScript().statement {
        setString(1, key)
        fetch {
            if (next()) getString("value")
            else null
        }
    }

    fun set(key: String, value: String) = if (!isTableExists()) null else setScript().statement {
        setString(1, key)
        setString(2, value)
        executeUpdate()
    }

    fun delete(key: String) = deleteScript().statement {
        setString(1, key)
        executeUpdate()
    }
    // endregion
}
