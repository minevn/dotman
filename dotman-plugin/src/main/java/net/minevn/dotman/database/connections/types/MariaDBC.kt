package net.minevn.dotman.database.connections.types

import net.minevn.dotman.database.connections.DatabaseConnection
import net.minevn.dotman.utils.Utils.Companion.severe
import net.minevn.libs.hikari.HikariDataSource

class MariaDBC(host: String, port: Int, database: String, user: String, password: String) : DatabaseConnection() {
    init {
        try {
            main.logger.info("Connecting to the database (MariaDB)...")

            dataSource = HikariDataSource().apply {
                addDataSourceProperty("url", "jdbc:mariadb://$host:$port/$database")
                dataSourceClassName = "org.mariadb.jdbc.MariaDbDataSource"
                username = user
                setPassword(password)
                keepaliveTime = 60000L
            }
            connection = dataSource.connection

            main.logger.info("Connected to the database (MariaDB)")
        } catch (ex: Exception) {
            ex.severe("Could not connect to the database")
            main.server.pluginManager.disablePlugin(main)
            throw ex
        }
    }


    override fun getTypeName() = "mysql"
}
