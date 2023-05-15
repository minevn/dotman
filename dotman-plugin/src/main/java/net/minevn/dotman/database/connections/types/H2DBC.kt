package net.minevn.dotman.database.connections.types

import net.minevn.dotman.database.connections.DatabaseConnection
import net.minevn.dotman.utils.Utils.Companion.severe
import net.minevn.libs.hikari.HikariDataSource
import java.io.File

class H2DBC(fileName: String) : DatabaseConnection() {

	init {
		try {
			val file = File(main.dataFolder, fileName)
			main.logger.info("Connecting to the database (H2)...")

			dataSource = HikariDataSource().apply {
				addDataSourceProperty("url", "jdbc:h2:${file.absolutePath}")
				dataSourceClassName = "org.h2.jdbcx.JdbcDataSource"
				username = ""
				password = ""
				keepaliveTime = 60000L
			}
			connection = dataSource.connection

			main.logger.info("Connected to the database (H2)")
		} catch (ex: Exception) {
			ex.severe("Could not connect to the database")
			main.server.pluginManager.disablePlugin(main)
			throw ex
		}
	}

	override fun getTypeName() = "h2"
}