package net.minevn.dotman.database

import net.minevn.dotman.DotMan
import net.minevn.dotman.card.Card
import net.minevn.dotman.card.CardWaiting
import net.minevn.dotman.utils.Utils
import net.minevn.libs.db.DataAccess
import net.minevn.libs.minMaxEpochTimestamp
import net.minevn.libs.timeToString
import org.bukkit.OfflinePlayer

abstract class LogDAO : DataAccess() {
    companion object {
        fun getInstance() = DotMan.instance.getDAO(LogDAO::class)
    }

    private fun getMain() = DotMan.instance

    // region scripts
    abstract fun insertLogScript(): String
    abstract fun setWaitingScript(): String
    abstract fun getWaitingCardsScript(): String
    abstract fun stopWaitingScript(): String
    abstract fun setTransactionIdScript(): String
    abstract fun getHistoryScriptAllPlayerAllTime(): String
    abstract fun getHistoryScriptAllPlayerByMonth(): String
    abstract fun getHistoryScriptByPlayerAllTime(): String
    abstract fun getHistoryScriptByPlayerByMonth(): String
    abstract fun getSumScriptAllPlayerAllTime(): String
    abstract fun getSumScriptAllPlayerByMonth(): String
    abstract fun getSumScriptByPlayerAllTime(): String
    abstract fun getSumScriptByPlayerByMonth(): String
    abstract fun updatePointReceivedScript(): String
    abstract fun updateTimeScript(): String
    // endregion

    // region queriers
    /**
     * Insert log trước khi tiến hành nạp
     * @param player OfflinePlayer
     * @param card Thẻ cần nạp
     */
    fun insertLog(player: OfflinePlayer, card: Card) = insertLog(player.uniqueId.toString(), card.seri, card.pin,
        card.type.name, card.price.value)

    fun insertLog(uuid: String, seri: String, pin: String, type: String, amount: Int) = run {
        insertLogScript().statementWithKey {
            setString(1, "")
            setString(2, uuid)
            setString(3, seri)
            setString(4, pin)
            setString(5, type)
            setInt(6, amount)
            setLong(7, System.currentTimeMillis())
            setString(8, getMain().config.server)

            executeUpdate()
            generatedKeys.use {
                if (it.next()) {
                    it.getInt(1).apply { Utils.info("Ghi log thành công: UUID: $uuid, ID: $this") }
                } else throw IllegalStateException("Ghi log thất bại cho UUID $uuid")
            }
        }
    }

    /**
     * Cập nhật trạng thái thẻ đang chờ xử lý
     * @param id ID của log
     */
    fun setWaiting(id: Int) {
        setWaitingScript().statement {
            setInt(1, id)
            executeUpdate()
        }
    }

    /**
     * Cập nhật trạng thái thẻ đã được xử lý
     */
    fun stopWaiting(id: Int, isSuccess: Boolean) {
        stopWaitingScript().statement {
            setInt(1, if (isSuccess) 1 else 0)
            setInt(2, id)
            executeUpdate()
        }
    }

    /**
     * Lấy danh sách thẻ đang chờ xử lý
     */
    fun getWaitingCards(playerUuids: Array<String>, server: String) = run {
        val uuids = playerUuids.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { "'$it'" }
            ?: return@run null

        getWaitingCardsScript()
            .replace("{UUIDS}", uuids)
            .statement {
                setString(1, server)

                fetchRecords {
                    CardWaiting(
                        getInt("id"),
                        getString("uuid"),
                        getString("seri"),
                        getString("type"),
                        getInt("price")
                    )
                }
            }
    }

    /**
     * Cập nhật mã giao dịch
     */
    fun setTransactionId(id: Int, transactionId: String, isSuccess: Boolean) {
        setTransactionIdScript().statement {
            setString(1, transactionId)
            setInt(2, if (isSuccess) 1 else 0)
            setInt(3, id)

            executeUpdate()
        }
    }

    /**
     * Lấy lịch sử nạp thẻ
     */
    @Suppress("UNUSED_CHANGED_VALUE")
    fun getHistory(playerName: String? = null, yearMonth: String? = null, page: Int) = run {
        val linePerPage = 20
        val script = if (yearMonth == null) {
            if (playerName == null) getHistoryScriptAllPlayerAllTime() else getHistoryScriptByPlayerAllTime()
        } else {
            if (playerName == null) getHistoryScriptAllPlayerByMonth() else getHistoryScriptByPlayerByMonth()
        }
        script.statement {
            var columnIndex = 1
            val offset = (page - 1) * linePerPage
            if (playerName != null) setString(columnIndex++, playerName)
            if (yearMonth != null) {
                val (min, max) = minMaxEpochTimestamp(yearMonth)
                setLong(columnIndex++, min)
                setLong(columnIndex++, max)
            }
            setInt(columnIndex++, offset)
            setInt(columnIndex++, linePerPage)
            fetchRecords {
                val rowNum = getInt("rownum")
                val name = getString("name")
                val type = getString("type")
                val price = getInt("price")
                val pointsReceived = getInt("pointsnhan")
                val time = getLong("time").timeToString()
                getMain().language.logOutPut
                    .replace("%ORDER%", rowNum.toString())
                    .replace("%PLAYER%", name)
                    .replace("%CARD_TYPE%", type)
                    .replace("%CARD_PRICE%", price.toString())
                    .replace("%POINTS_RECEIVED%", pointsReceived.toString())
                    .replace("%POINT_UNIT%", getMain().config.pointUnit)
                    .replace("%DATE%", time)
            }
        }
    }

    @Suppress("UNUSED_CHANGED_VALUE")
    fun getSum(playerName: String? = null, yearMonth: String? = null) = run {
        val script = if (yearMonth == null) {
            if (playerName == null) getSumScriptAllPlayerAllTime() else getSumScriptByPlayerAllTime()
        } else {
            if (playerName == null) getSumScriptAllPlayerByMonth() else getSumScriptByPlayerByMonth()
        }
        script.statement {
            var columnIndex = 1
            if (playerName != null) setString(columnIndex++, playerName)
            if (yearMonth != null) {
                val (min, max) = minMaxEpochTimestamp(yearMonth)
                setLong(columnIndex++, min)
                setLong(columnIndex++, max)
            }

            fetch {
                next()
                Pair(getInt(1), getInt(2))
            }
        }
    }

    /**
     * Cập nhật point nhận được
     */
    fun updatePointReceived(id: Int, points: Int) {
        updatePointReceivedScript().statement {
            setInt(1, points)
            setInt(2, id)
            executeUpdate()
        }
    }

    /**
     * Cập nhật thời gian nạp thẻ
     */
    fun updateTime(id: Int, time: Long) {
        updateTimeScript().statement {
            setLong(1, time)
            setInt(2, id)
            executeUpdate()
        }
    }
    // endregion
}
