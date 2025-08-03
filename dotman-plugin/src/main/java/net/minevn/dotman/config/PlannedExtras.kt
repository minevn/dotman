package net.minevn.dotman.config

import net.minevn.dotman.utils.Utils.Companion.info
import net.minevn.dotman.utils.Utils.Companion.warning
import java.text.SimpleDateFormat

class PlannedExtras : FileConfig("khuyenmai") {

    private var components: List<Component> = emptyList()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

    init {
        loadComponents()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadComponents() {
        components = (config.getList("khuyenmai") ?: emptyList()).mapNotNull {
            try {
                it as Map<*, *>
                val name = it["name"] as String
                val rate = it["rate"] as Double
                val fromStr = it["from"] as String
                val toStr = it["to"] as String

                val from = dateFormat.parse(fromStr).time
                val to = dateFormat.parse(toStr).time

                Component(name, rate, from, to)
            } catch (e: Exception) {
                e.warning("Có một khuyến mãi không hợp lệ: ${e.message}")
                null
            }
        }

        info("Đã nạp ${components.size} khuyến mãi.")
    }

    override fun reload() {
        super.reload()
        loadComponents()
    }

    /**
     * Lấy khuyến mãi đang hoạt động tại thời điểm hiện tại
     * Nếu có nhiều khuyến mãi cùng lúc, sẽ lấy khuyến mãi có tỉ lệ cao nhất
     *
     * @return Khuyến mãi đang hoạt động, hoặc null nếu không có
     */
    fun getCurrentExtra(): Component? {
        val currentTime = System.currentTimeMillis()
        return components
            .filter { it.from <= currentTime && it.to >= currentTime }
            .maxByOrNull { it.rate }
    }

    class Component(val name: String, val rate: Double, val from: Long, val to: Long) {
        /**
         * Kiểm tra xem khuyến mãi có đang hoạt động tại thời điểm hiện tại không
         *
         * @return true nếu khuyến mãi đang hoạt động
         */
        fun isActive(): Boolean {
            val currentTime = System.currentTimeMillis()
            return from <= currentTime && to >= currentTime
        }

        /**
         * Tính toán số tiền khuyến mãi dựa trên số tiền gốc
         *
         * @param baseAmount Số tiền gốc
         * @return Số tiền sau khi áp dụng khuyến mãi
         */
        fun calculateAmount(baseAmount: Int): Int {
            return baseAmount + (baseAmount * rate).toInt()
        }

        /**
         * Lấy phần trăm khuyến mãi
         *
         * @return Phần trăm khuyến mãi
         */
        fun getPercentage(): Int {
            return (rate * 100).toInt()
        }
    }
}
