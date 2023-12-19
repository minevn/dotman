package net.minevn.dotman.database.connections

import net.minevn.dotman.DotMan
import net.minevn.dotman.database.connections.types.H2DBC
import net.minevn.dotman.database.connections.types.MariaDBC
import net.minevn.dotman.database.connections.types.MySqlDBC
import net.minevn.dotman.database.dao.ConfigDAO
import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.severe
import net.minevn.libs.bukkit.db.DBMigrator
import net.minevn.libs.hikari.HikariDataSource
import org.bukkit.configuration.file.YamlConfiguration
import java.sql.Connection
import java.sql.SQLException

abstract class DatabaseConnection {

    protected val main = DotMan.instance
    lateinit var connection: Connection protected set
    protected lateinit var dataSource: HikariDataSource

    companion object {
        lateinit var instance: DatabaseConnection private set

        /**
         * call this in **[onEnable][net.minevn.dotman.DotMan.onEnable]**
         */
        fun init(dbType: String, config: YamlConfiguration) {
            val dbConfig = if (dbType == "mariadb") "mysql" else dbType
            val prefix = "database.$dbConfig"
            instance = when (dbType) {
                "mysql", "mariadb" -> {
                    val host = config.getString("$prefix.host")
                    val port = config.getInt("$prefix.port")
                    val database = config.getString("$prefix.database")
                    val user = config.getString("$prefix.user")
                    val password = config.getString("$prefix.password")
                    if (dbType == "mysql") MySqlDBC(host, port, database, user, password)
                    else MariaDBC(host, port, database, user, password)
                }

                "h2" -> {
                    val file = config.getString("$prefix.file")
                    H2DBC(file)
                }

                else -> throw UnsupportedOperationException("invalid database type")
            }

            instance.migrate()
        }

        /**
         * call this in **[onDisable][net.minevn.dotman.DotMan.onDisable]**
         */
        fun unload() {
            if (::instance.isInitialized) {
                instance.disconnect()
            }
        }
    }

    fun disconnect() {
        info("Disconnecting from the ${getTypeName()} database...")
        try {
            if (::connection.isInitialized) connection.close()
            if (::dataSource.isInitialized) dataSource.close()
        } catch (ex: SQLException) {
            ex.severe("Could not disconnect from the database")
        }
    }

    fun migrate() {
        val configDao = ConfigDAO.getInstance()
        val schemaVersion = configDao.get("migration_version") ?: "0"
        val path = "db/migrations/${getTypeName()}"
        val updated = DBMigrator(DotMan.instance, connection, path, schemaVersion.toInt()).migrate()
        configDao.set("migration_version", updated.toString())
    }

    abstract fun getTypeName(): String
}
