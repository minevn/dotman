package net.minevn.dotman.database.dao

import net.minevn.dotman.card.Card
import net.minevn.dotman.card.CardWaiting
import net.minevn.dotman.config.MainConfig
import net.minevn.dotman.database.*
import net.minevn.dotman.utils.Utils
import org.bukkit.entity.Player

interface LogDAO : DataAccess {
    companion object {
        fun getInstance() = LogDAO::class.getInstance()
    }

    // region scripts
    fun insertLogScript(): String
    fun setWaitingScript(): String
    fun getWaitingCardsScript(): String
    fun stopWaitingScript(): String
    fun setTransactionIdScript(): String
    // endregion

    // region queriers
    /**
     * Insert log trước khi tiến hành nạp
     * @param player Player
     * @param card Thẻ cần nạp
     */
    fun insertLog(player: Player, card: Card) = run {
        Utils.info("${player.name} Nạp thẻ: $card")

        insertLogScript().statementWithKey {
            setString(1, player.name)
            setString(2, player.uniqueId.toString())
            setString(3, card.seri)
            setString(4, card.pin)
            setString(5, card.type.name)
            setInt(6, card.price.value)
            setLong(7, System.currentTimeMillis())
            setString(8, MainConfig.get().server)

            executeUpdate()
            generatedKeys.use {
                if (it.next()) {
                    it.getInt(1).apply { Utils.info("Ghi log thành công cho ${player.name}, ID: $this") }
                } else throw IllegalStateException("Ghi log thất bại cho ${player.name}")
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
     * WIP Lấy lịch sử nạp thẻ
     */
    fun getHistory(playerName: String? = null, page: Int) {
        val sqlTestScript = """
            select
                row_number() over (order by time desc) as rownum,
                name, type, seri, price, pointsnhan, time
            from dotman_napthe_log
            where success = 1
            order by time desc limit ?, 10;
        """.trimIndent()
        sqlTestScript.statement {
            val offset = (page - 1) * 10
            setInt(1, offset)

        }
    }

    /**
     * WIP Cập nhật point nhận được
     */
    fun updatePointReceived(id: Int, points: Int) {
        val sqlTestScript = """
            update dotman_napthe_log
            set pointsnhan = ?
            where id = ?;
        """.trimIndent()
        sqlTestScript.statement {
            setInt(1, points)
            setInt(2, id)
            executeUpdate()
        }
    }
    // endregion
}
