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
    /**
 * Returns the SQL script used to retrieve a single player data row by UUID and key.
 *
 * The returned string must be a prepared SQL statement that accepts two parameters in this order: `uuid`, `key`.
 */
abstract fun getDataScript(): String
    /**
 * Provides the SQL script used by getSumData to retrieve the summed value for a given key.
 *
 * The returned SQL must be a prepared-statement string that accepts a single parameter (the key)
 * and returns at least one numeric column named "value". getSumData binds the key to the statement
 * and reads the first row's "value" column (or uses 0 if no rows are returned).
 */
abstract fun getSumDataScript(): String
    /**
 * Returns the SQL script used by getAllData to fetch all key/value pairs for a player.
 *
 * The returned SQL must accept a single parameter (player UUID) and produce rows with
 * columns named `key` (String) and `value` (Int). The query's result will be mapped into
 * a Map where each row's `key` -> `value` pair is preserved.
 */
abstract fun getAllDataScript(): String
    /**
 * Provides the SQL script used to delete player data rows whose key matches a pattern.
 *
 * The returned SQL must be a prepared-statement string that accepts two parameters in this order:
 * 1. uuid — the player's UUID
 * 2. likePattern — the SQL LIKE pattern to match keys (e.g. "prefix%")
 *
 * @return The SQL delete statement as a String.
 */
abstract fun deleteDataByKeyLikeScript(): String

    /**
     * Inserts a player data record using the DAO's SQL insert script.
     *
     * Prepares the statement returned by [insertDataScript], binds the provided
     * UUID, key and value, and uses the current system time as the record timestamp.
     *
     * @param uuid Player identifier as a string (UUID).
     * @param key Data key/name to store.
     * @param value Integer value to store for the given key.
     */
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

    /**
     * Retrieves all stored key/value pairs for the given player UUID.
     *
     * Executes the DAO's `getAllDataScript()` and returns a map from data key to integer value
     * for the specified player. If no records are found, an empty map is returned.
     *
     * @param uuid Player UUID whose data should be fetched.
     * @return A Map where keys are data keys (String) and values are their associated Int values.
     */
    fun getAllData(uuid: String) = run {
        getAllDataScript().statement {
            setString(1, uuid)

            fetchRecords {
                getString("key")!! to getInt("value")
            }.toMap()
        }
    }

    /**
     * Deletes player data rows whose keys match the provided SQL LIKE pattern and returns how many rows were removed.
     *
     * Uses the SQL statement provided by [deleteDataByKeyLikeScript()] and binds `uuid` and `likePattern` as the first
     * and second parameters respectively.
     *
     * @param uuid Player UUID whose data rows should be targeted.
     * @param likePattern SQL `LIKE` pattern to match keys (e.g. `"score_%"`). Must be a valid pattern for the underlying database.
     * @return The number of rows affected (deleted).
     */
    fun deleteDataByKeyLike(uuid: String, likePattern: String): Int {
        return deleteDataByKeyLikeScript().statement {
            setString(1, uuid)
            setString(2, likePattern)
            update()
        }
    }
}
