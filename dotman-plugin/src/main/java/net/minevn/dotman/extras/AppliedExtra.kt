package net.minevn.dotman.extras

/**
 * Khuyến mãi đang thực sự được áp dụng: mục có tỉ lệ cao nhất đang chạy trong khuyenmai.yml,
 * hoặc khuyến mãi legacy trong config.yml (extra-rate) nếu không có mục nào đang chạy
 *
 * @param name Tên hiển thị (có thể chứa mã màu)
 * @param rate Tỉ lệ khuyến mãi, 0.5 = 50%
 * @param planned Mục trong khuyenmai.yml, null nếu là khuyến mãi legacy
 */
class AppliedExtra(val name: String, val rate: Double, val planned: PlannedExtraEntry?) {
    /**
     * Lấy phần trăm khuyến mãi
     *
     * @return Phần trăm khuyến mãi
     */
    fun getPercentage(): Int {
        return (rate * 100).toInt()
    }
}
