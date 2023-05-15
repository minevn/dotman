package net.minevn.dotman.database.connections.types

import net.minevn.dotman.database.connections.DatabaseConnection
import net.minevn.dotman.utils.Utils.Companion.severe
import net.minevn.libs.hikari.HikariDataSource

class MySqlDBC(host: String, port: Int, database: String, user: String, password: String) : DatabaseConnection() {

	init {
		try {
			main.logger.info("Connecting to the database (MySQL)...")

			dataSource = HikariDataSource().apply {
				jdbcUrl = "jdbc:mysql://$host:$port/$database"
				username = user
				setPassword(password)
				maximumPoolSize = 12
				minimumIdle = 12
				maxLifetime = 1800000
				keepaliveTime = 60000L
				connectionTimeout = 20000
				addDataSourceProperty("cachePrepStmts", "true")
				addDataSourceProperty("prepStmtCacheSize", "250")
				addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
				addDataSourceProperty("useServerPrepStmts", "true")
				addDataSourceProperty("useLocalSessionState", "true")
				addDataSourceProperty("useLocalTransactionState", "true")
				addDataSourceProperty("rewriteBatchedStatements", "true")
				addDataSourceProperty("cacheResultSetMetadata", "true")
				addDataSourceProperty("cacheServerConfiguration", "true")
				addDataSourceProperty("elideSetAutoCommits", "true")
				addDataSourceProperty("maintainTimeStats", "false")
			}
			connection = dataSource.connection

			main.logger.info("Connected to the database (MySQL)")
		} catch (ex: Exception) {
			ex.severe("Could not connect to the database")
			main.server.pluginManager.disablePlugin(main)
			throw ex
		}
	}

	override fun getTypeName() = "mysql"
}