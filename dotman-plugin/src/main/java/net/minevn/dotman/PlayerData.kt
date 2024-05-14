package net.minevn.dotman

import net.minevn.dotman.database.PlayerDataDAO
import net.minevn.dotman.utils.Utils
import net.minevn.libs.bukkit.runSync
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

const val PLAYER_DATA_EXPIRE = 1 * 60 * 1000L // 1 phút

class PlayerData(
    val data: Map<String, Int>,
    private val initialTime: Long = System.currentTimeMillis()
) {
    fun isExpired() = System.currentTimeMillis() - initialTime > PLAYER_DATA_EXPIRE

    companion object {
        private val dataCache = ConcurrentHashMap<String, PlayerData>()

        // cập nhật dữ liệu top
        private fun getFromDB(uuid: String) = PlayerData(
            mapOf(
                TOP_KEY_DONATE_TOTAL to PlayerDataDAO.getInstance().getData(uuid, TOP_KEY_DONATE_TOTAL),
                TOP_KEY_POINT_FROM_CARD to PlayerDataDAO.getInstance().getData(uuid, TOP_KEY_POINT_FROM_CARD)
            )
        )

        /**
         * Lấy thông tin PlayerData dựa trên uuid của người chơi đã cho. Nếu PlayerData đã được lưu trong bộ nhớ đệm
         * và vẫn còn hiệu lực, nó sẽ được trả về ngay lập tức. Ngược lại, nếu hàm được gọi từ luồng chính
         * (primary thread), một nhiệm vụ bất đồng bộ sẽ được khởi chạy để lấy PlayerData từ cơ sở dữ liệu và
         * PlayerData hiện tại (nếu có) từ bộ nhớ đệm sẽ được trả về. Nếu hàm không được gọi từ luồng chính,
         * PlayerData sẽ được lấy trực tiếp từ cơ sở dữ liệu, cập nhật trong bộ nhớ đệm và trả về.
         *
         * @param uuid Khóa định danh duy nhất cho LeaderBoard.
         * @return PlayerData liên kết với khóa đã cho.
         */
        operator fun get(uuid: String) = dataCache[uuid].run {
            if (this?.isExpired() != false) {
                if (Bukkit.isPrimaryThread()) {
                    Utils.runAsync { getFromDB(uuid).also { runSync { dataCache[uuid] = it } } }
                    this ?: PlayerData(emptyMap(), 0L)
                } else {
                    getFromDB(uuid).also { dataCache[uuid] = it }
                }
            } else this
        }
    }
}