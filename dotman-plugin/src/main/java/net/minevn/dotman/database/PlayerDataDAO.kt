package net.minevn.dotman.database

import net.minevn.dotman.DotMan
import net.minevn.dotman.utils.TopType
import net.minevn.libs.db.DataAccess

abstract class PlayerDataDAO : DataAccess() {
    companion object {
        fun getInstance() = DotMan.instance.getDAO(PlayerDataDAO::class)
    }

    abstract fun insertDataScript(): String
    abstract fun getTopScript(): String
    abstract fun getDataScript(): String
    abstract fun getSumDataScript(): String

    fun insertData(uuid: String, key: String, value: Int) {
        insertDataScript().statement {
            setString(1, uuid)
            setString(2, "converted_to_uuid")
            setString(3, key)
            setInt(4, value)
            setLong(5, System.currentTimeMillis())

            executeUpdate()
        }
    }

    fun getData(uuid: String, key: String) = getDataScript().statement {
        setString(1, uuid)
        setString(2, key)

        fetchRecords {
            getInt("value")
        }.firstOrNull() ?: 0
    }

    fun getSumData(key: String) = getSumDataScript().statement {
        setString(1, key)

        fetchRecords {
            getInt("value")
        }.firstOrNull() ?: 0
    }

    fun insertAllType(uuid: String, key: String, value: Int) = TopType.entries.forEach {
        insertData(uuid, it.parseKey(key), value)
    }

    fun getTop(key: String, limit: Int) = run {
        getTopScript().statement {
            setString(1, key)
            setInt(2, limit)

            fetchRecords {
                getString("name")!! to getInt("value")
            }
        }
    }
}