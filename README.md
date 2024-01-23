Donation Manager [![GitHub Release](https://img.shields.io/github/v/release/minevn/DotMan?style=flat)](https://github.com/minevn/dotman/releases) [![Discord](https://img.shields.io/discord/1068181110036635678.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://minevn.studio/discord) ![Supported server version](https://img.shields.io/badge/minecraft-1.8%20--_1.20-green)
===========

Plugin tích hợp donate qua thẻ cào tốt nhất cho server Minecraft, nhiều tính năng hữu ích, tăng cường sự quản lý của admin và hỗ trợ nhiều dịch vụ gạch thẻ

**Dịch vụ đang hỗ trợ:** [Gamebank](gamebank.vn), [TheSieuToc](https://thesieutoc.net/)

Tính năng hiện có
===========

- Nạp thẻ tự động
- Hẹn giờ kết thúc khuyến mãi
- Config giao diện linh hoạt, có thể thay đổi được vị trí của icon
- Top nạp thẻ
- Phần thưởng theo mốc nạp
- Lệnh nạp nhanh với auto-complete, hỗ trợ cho phiên bản 1.8

Hướng dẫn sử dụng
===========

**Cài đặt plugin:**

- Plugin cần có [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/) và [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) để hoạt động
- Tải plugin [tại đây](https://github.com/minevn/dotman/releases), giải nén và cài DotMan.jar và MineVNLib.jar và server của bạn
- MineVNLib là thư viện liên quan đến SQL và Kotlin nên dung lượng sẽ hơi nặng, nhưng sẽ không gây lag cho server của bạn

**Danh sách lệnh:**

| Lệnh | Chức năng | Quyền sử dụng |
| -----| ---------- | ------- |
| /napthe | Mở menu nạp thẻ | Người chơi |
| /napthe <loại thẻ> <mệnh giá> <số seri> <mã thẻ> | Nạp thẻ nhanh | Người chơi |
| /dotman reload | Reload lại config | OP |
| /dotman thongbao | Thay đổi thông báo trong menu nạp thẻ | OP |
| /dotman chuyenkhoan | Đặt vị trí xem hướng dẫn chuyển khoản | OP |
| /dotman lichsu | Xem lịch sử nạp thẻ | OP |

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
- Để thêm API Key cho các dịch vụ tương ứng, hãy thêm tại các file `gamebanj.yml` và `thesieutoc.yml`
- Cài đặt chung của plugin được đặt tại `config.yml`, các message đặt tại `messages.yml`
- Cài đặt mốc nạp tích luỹ tại `mocnap.yml`

Plugin đang trong giai đoạn beta (thử nghiệm), mong các bạn giúp mình trải nghiệm và đưa góp ý, nhận xét. Nếu bạn thích plugin, hãy [donate ủng hộ](https://minevn.studio/faqs/#how-can-i-pay-for-the-products) bọn mình và đánh giá 5 sao [tại forum](https://minecraftvn.net/resources/donation-manager-update-21-1-plugin-nap-the-tot-nhat-cho-minecraft.3657/) nhé