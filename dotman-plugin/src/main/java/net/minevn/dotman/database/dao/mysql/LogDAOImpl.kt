package net.minevn.dotman.database.dao.mysql

import net.minevn.dotman.database.dao.LogDAO

class LogDAOImpl : LogDAO {
	override fun insertLogScript() = """
			INSERT INTO `dotman_napthe_log`
				(`name`, `uuid`, `seri`, `pin`, `type`, `price`, `time`, `server`)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?)
		""".trimIndent()

	override fun setWaitingScript() = "UPDATE `dotman_napthe_log` SET `waiting` = 1 WHERE `id` = ?"

	override fun stopWaitingScript() = """
			UPDATE `dotman_napthe_log` 
			SET `waiting` = 0, `success` = ?
			WHERE `id` = ?;
		""".trimIndent()

	override fun getWaitingCardsScript() = """
			SELECT * FROM `dotman_napthe_log` 
			WHERE `server` = ? AND `waiting` = 1 AND `uuid` IN ({UUIDS});
		""".trimIndent()

	override fun setTransactionIdScript() = """
			UPDATE `dotman_napthe_log`
			SET `transaction_id` = ?, `success` = ?
			WHERE `id` = ?;
		""".trimIndent()
}
