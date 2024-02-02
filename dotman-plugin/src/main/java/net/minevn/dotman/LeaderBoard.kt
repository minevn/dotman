package net.minevn.dotman

import net.minevn.dotman.database.PlayerDataDAO
import net.minevn.dotman.utils.Utils.Companion.runAsync
import net.minevn.libs.bukkit.runSync
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

const val TOP_EXPIRE = 5 * 60 * 1000L // 5 phút
const val TOP_COUNT = 100

const val TOP_KEY_DONATE_TOTAL = "DONATE_TOTAL"
const val TOP_KEY_POINT_FROM_CARD = "POINT_FROM_CARD"

class LeaderBoard(
    private val list: List<Pair<String, Int>>,
    private val inititalTime: Long = System.currentTimeMillis()
) {


    fun isExpired() = System.currentTimeMillis() - inititalTime > TOP_EXPIRE

    operator fun get(rank: Int) = list.getOrNull(rank - 1)

    fun size() = list.size

    companion object {
        private val topCache = ConcurrentHashMap<String, LeaderBoard>()

        // cập nhật dữ liệu top
        private fun getFromDB(key: String) = LeaderBoard(PlayerDataDAO.getInstance().getTop(key, TOP_COUNT))

        /**
         * Lấy thông tin LeaderBoard dựa trên khóa (key) đã cho. Nếu LeaderBoard đã được lưu trong bộ nhớ đệm
         * và vẫn còn hiệu lực, nó sẽ được trả về ngay lập tức. Ngược lại, nếu hàm được gọi từ luồng chính
         * (primary thread), một nhiệm vụ bất đồng bộ sẽ được khởi chạy để lấy LeaderBoard từ cơ sở dữ liệu và
         * LeaderBoard hiện tại (nếu có) từ bộ nhớ đệm sẽ được trả về. Nếu hàm không được gọi từ luồng chính,
         * LeaderBoard sẽ được lấy trực tiếp từ cơ sở dữ liệu, cập nhật trong bộ nhớ đệm và trả về.
         *
         * @param key Khóa định danh duy nhất cho LeaderBoard.
         * @return LeaderBoard liên kết với khóa đã cho.
         */
        operator fun get(key: String) = topCache[key].run {
            if (this?.isExpired() != false) {
                if (Bukkit.isPrimaryThread()) {
                    runAsync { getFromDB(key).also { runSync { topCache[key] = it } } }
                    this ?: LeaderBoard(emptyList(), 0L)
                } else {
                    getFromDB(key).also { topCache[key] = it }
                }
            } else this
        }
    }
}
