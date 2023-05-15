package net.minevn.dotman.database

import net.minevn.dotman.database.connections.DatabaseConnection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface DataAccess {
	companion object {
		private var instanceList = mutableMapOf<KClass<out DataAccess>, DataAccess>()

		fun <T : DataAccess> getInstance(type : KClass<T>) : T {
			var instance = instanceList[type]
			if (instance == null) {
				val basePackage = "net.minevn.dotman.database.dao"
				val dbType = DatabaseConnection.instance.getTypeName()
				val daoClass = Class.forName("$basePackage.$dbType.${type.simpleName}Impl")
				instance = type.cast(daoClass.getDeclaredConstructor().newInstance())
				instanceList[type] = instance
			}
			return type.cast(instance)
		}
	}
}

fun <T : DataAccess> KClass<T>.getInstance() = DataAccess.getInstance(this)

/**
 * Initialize the PreparedStatement with the given SQL statement
 */
internal fun <R> String.statement(action: PreparedStatement.() -> R) =
	DatabaseConnection.instance.connection.prepareStatement(this).use { it.action() }


/**
 * Initialize the PreparedStatement with the given SQL statement, with the option to return generated keys
 */
internal fun <R> String.statementWithKey(action: PreparedStatement.() -> R) =
	DatabaseConnection.instance.connection.prepareStatement(this, Statement.RETURN_GENERATED_KEYS).use { it.action() }

/**
 * Process the ResultSet
 */
internal fun <R> PreparedStatement.fetch(action: ResultSet.() -> R) =
	executeQuery().use { it.action() }

/**
 * Iterate through all records in the ResultSet
 */
internal fun <R> PreparedStatement.fetchIterate(action: ResultSet.() -> R) =
	fetch { while (next()) { action() } }

/**
 * Map all records in the ResultSet to a list
 */
internal fun <R> PreparedStatement.fetchRecords(action: ResultSet.() -> R): List<R> =
	fetch { generateSequence { if (next()) action() else null }.toList() }
