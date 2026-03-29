Donation Manager [![GitHub Release](https://img.shields.io/github/v/release/minevn/DotMan?style=flat)](https://github.com/minevn/dotman/releases) [![Discord](https://img.shields.io/discord/1068181110036635678.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://minevn.studio/discord) ![Supported server version](https://img.shields.io/badge/minecraft-1.8%20--_1.20-green)
===========

Plugin tích hợp donate qua thẻ cào tốt nhất cho server Minecraft, nhiều tính năng hữu ích, tăng cường sự quản lý của admin và hỗ trợ nhiều dịch vụ gạch thẻ

**Dịch vụ đang hỗ trợ:** [gamebank](https://sv.gamebank.vn/user/nap-the), [thesieutoc](https://thesieutoc.net/), [gachthe5s](https://gachthe5s.com/) (premium)

Tính năng hiện có
===========

- Nạp thẻ tự động
- Hẹn giờ kết thúc khuyến mãi
- Config giao diện linh hoạt, có thể thay đổi được vị trí của icon
- Top nạp thẻ
- Phần thưởng theo mốc nạp
- Lệnh nạp nhanh với auto-complete, hỗ trợ cho phiên bản 1.8
- Top nạp theo tuần/tháng (✨ premium)
- Mốc nạp theo tuần/tháng (✨ premium)
- Giao diện Bedrock (✨ premium)
- Lịch sử giao dịch point (✨ premium)
- Hỗ trợ nhiều API gạch thẻ hơn, và bổ sung API gạch thẻ theo yêu cầu (✨ premium)

Hướng dẫn sử dụng
===========

**Cài đặt plugin:**

- Plugin cần có [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/) và [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) để hoạt động
- Tải plugin [tại đây](https://github.com/minevn/dotman/releases), giải nén và cài DotMan.jar và MineVNLib.jar và server của bạn
- MineVNLib là thư viện liên quan đến SQL và Kotlin nên dung lượng sẽ hơi nặng, nhưng sẽ không gây lag cho server của bạn

**Danh sách lệnh:**

| Lệnh                                             | Chức năng                             | Permission    |
|--------------------------------------------------|---------------------------------------|---------------|
| /napthe                                          | Mở menu nạp thẻ                       | Người chơi    |
| /napthe <loại thẻ> <mệnh giá> <số seri> <mã thẻ> | Nạp thẻ nhanh                         | Người chơi    |
| /topnap                                          | Xem top nạp thẻ                       | dotman.topnap |
| /dotman reload                                   | Reload lại config                     | dotman.admin  |
| /dotman thongbao                                 | Thay đổi thông báo trong menu nạp thẻ | dotman.admin  |
| /dotman chuyenkhoan                              | Đặt vị trí xem hướng dẫn chuyển khoản | dotman.admin  |
| /dotman lichsu                                   | Xem lịch sử nạp thẻ                   | dotman.admin  |

**Placeholder:**

Placeholder có thể sử dụng để hiển thị top nạp và dữ liệu cá nhân

| Placeholder                              | Chức năng                                      | Ghi chú                                                                                                                                                                          |
|------------------------------------------|------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| %DOTMAN_TOP_DONATE_TOTAL_XXX_PLAYER%     | Trả về tên người chơi đứng top XXX            | Thay `TOP` thành `TOPWEEK`, `TOPMONTH`, `TOPDAY` để lấy top tuần/tháng/ngày<br>Dùng `TOPFRAME_<frameId>` để lấy top theo khung giờ tuỳ chỉnh (✨ premium)                      |
| %DOTMAN_TOP_DONATE_TOTAL_XXX_VALUE%      | Trả về giá trị nạp của người đứng top XXX     | Thay `TOP` thành `TOPWEEK`, `TOPMONTH`, `TOPDAY` để lấy top tuần/tháng/ngày<br>Dùng `TOPFRAME_<frameId>` để lấy top theo khung giờ tuỳ chỉnh (✨ premium)                      |
| %DOTMAN_DATA_DONATE_TOTAL%               | Trả về số point người chơi hiện tại đã nạp    | Thay `DATA` thành `DATAWEEK`, `DATAMONTH`, `DATADAY` để lấy dữ liệu tuần/tháng/ngày<br>Dùng `DATAFRAME_<frameId>` để lấy dữ liệu theo khung giờ tuỳ chỉnh (✨ premium)        |
| %DOTMAN_MASTERDATA_DONATE_TOTAL%         | Trả về tổng point toàn server đã nạp          | Thay `MASTERDATA` thành `MASTERDATAWEEK`, `MASTERDATAMONTH`, `MASTERDATADAY` để lấy dữ liệu tuần/tháng/ngày<br>Dùng `MASTERDATAFRAME_<frameId>` cho khung giờ tuỳ chỉnh (✨ premium) |

> Ví dụ: `%DOTMAN_TOP_DONATE_TOTAL_1_PLAYER%` trả về tên người đứng top 1, `%DOTMAN_TOP_DONATE_TOTAL_1_VALUE%` trả về số tiền nạp của người đó, `%DOTMAN_DATA_DONATE_TOTAL%` trả về số point cá nhân đã nạp tổng cộng

**Config plugin:**

Cấu trúc thư mục `./plugins/DotMan` như sau
```
DotMan/
├── menu/
│   └── napthe/
│       ├── loaithe.yml
│       └── menhgia.yml
├── providers/
│   ├── gamebank.yml
│   └── thesieutoc.yml
├── config.yml
├── messages.yml
└── mocnap.yml
```

- Bạn có thể config giao diện tại các file `loaithe.yml` và `menhgia.yml`
- Để thêm API Key cho các dịch vụ tương ứng, hãy thêm tại các file `gamebank.yml` và `thesieutoc.yml`
- Cài đặt chung của plugin được đặt tại `config.yml`, các message đặt tại `messages.yml`
- Cài đặt mốc nạp tích luỹ tại `mocnap.yml`

Hình ảnh
===========
![](https://i.imgur.com/Wds3sRi.png)

![](https://i.imgur.com/LUYYbdR.png)

![](https://i.imgur.com/giSRIkg.png)

![](https://i.imgur.com/zr3Mhdy.gif)

![](https://i.imgur.com/4Xng6eR.gif)

Plugin được thiết kế và phát triển kĩ lưỡng nhằm đảm bảo tính ổn định và chất lượng. Nếu plugin có ích cho server của bạn, hãy [mua bản premium](https://minecraftvn.net/resources/donation-manager-update-21-1-plugin-nap-the-tot-nhat-cho-minecraft.3657/) để trải nghiệm các tính năng mở rộng và đánh giá 5 sao [tại forum](https://minecraftvn.net/resources/donation-manager-update-21-1-plugin-nap-the-tot-nhat-cho-minecraft.3657/) nhé
