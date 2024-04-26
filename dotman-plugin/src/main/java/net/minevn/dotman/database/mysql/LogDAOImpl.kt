package net.minevn.dotman.database.mysql

import net.minevn.dotman.database.LogDAO

class LogDAOImpl : LogDAO() {
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

    override fun getHistoryScriptAllPlayerAllTime() = """
            select
                row_number() over (order by time desc) as rownum,
                id, i.name as name, type, seri, price, pointsnhan, time
            from dotman_napthe_log l left join dotman_player_info i on i.uuid = l.uuid
            where success = 1
            order by time desc limit ?, ?;
        """.trimIndent()

    override fun getHistoryScriptAllPlayerByMonth() = """
            select
                row_number() over (order by time desc) as rownum,
                id, i.name as name, type, seri, price, pointsnhan, time
            from dotman_napthe_log l left join dotman_player_info i on i.uuid = l.uuid
            where success = 1
            and time >= ? and time <= ?
            order by time desc limit ?, ?;
        """.trimIndent()

    override fun getHistoryScriptByPlayerAllTime() = """
            select
                row_number() over (order by time desc) as rownum,
                id, i.name as name, type, seri, price, pointsnhan, time
            from dotman_napthe_log l left join dotman_player_info i on i.uuid = l.uuid
            where success = 1 and name = ?
            order by time desc limit ?, ?;
        """.trimIndent()

    override fun getHistoryScriptByPlayerByMonth() = """
            select
                row_number() over (order by time desc) as rownum,
                id, i.name as name, type, seri, price, pointsnhan, time
            from dotman_napthe_log l left join dotman_player_info i on i.uuid = l.uuid
            where success = 1 and name = ?
            and time >= ? and time <= ?
            order by time desc limit ?, ?;
        """.trimIndent()

    override fun getSumScriptAllPlayerAllTime() = """
        SELECT
            SUM(`price`), COUNT(`id`)
        FROM `dotman_napthe_log`
        WHERE `success` = 1
    """.trimIndent()

    override fun getSumScriptAllPlayerByMonth() = """
        SELECT
            SUM(`price`), COUNT(`id`)
        FROM `dotman_napthe_log`
        WHERE `success` = 1
        and `time` >= ? and `time` <= ?
    """.trimIndent()

    override fun getSumScriptByPlayerAllTime() = """
        SELECT
            SUM(`price`), COUNT(`id`)
        FROM `dotman_napthe_log`
        WHERE `success` = 1 AND `name` = ?
    """.trimIndent()

    override fun getSumScriptByPlayerByMonth() = """
        SELECT
            SUM(`price`), COUNT(`id`)
        FROM `dotman_napthe_log`
        WHERE `success` = 1 AND `name` = ?
        and `time` >= ? and `time` <= ?
    """.trimIndent()

    override fun updatePointReceivedScript() = """
            UPDATE `dotman_napthe_log`
            SET `pointsnhan` = ?
            WHERE `id` = ?;
        """.trimIndent()

    override fun updateTimeScript() = """
            UPDATE `dotman_napthe_log`
            SET `time` = ?
            WHERE `id` = ?;
    """.trimIndent()
}
