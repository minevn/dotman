package net.minevn.dotman.extras

import java.time.ZonedDateTime

/**
 * Một khuyến mãi trong khuyenmai.yml
 *
 * @param name Tên khuyến mãi (có thể chứa mã màu)
 * @param rate Tỉ lệ khuyến mãi, 0.5 = 50%
 * @param schedule Lịch hoạt động
 */
class PlannedExtraEntry(val name: String, val rate: Double, val schedule: Schedule) {
    /**
     * Kiểm tra xem khuyến mãi có đang hoạt động tại thời điểm chỉ định không
     *
     * @param now Thời điểm cần kiểm tra, mặc định là hiện tại
     * @return true nếu khuyến mãi đang hoạt động
     */
    fun isActive(now: ZonedDateTime = ZonedDateTime.now()) = schedule.isActive(now)

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
