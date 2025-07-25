# Dotman Banking Expansion Plugin

![Minecraft Plugin](https://img.shields.io/badge/Minecraft-1.20+-brightgreen) ![Discord Integration](https://img.shields.io/badge/Discord-Integrated-blue) ![Streak System](https://img.shields.io/badge/Streak-System-orange)

Một plugin Minecraft cao cấp tích hợp hệ thống streak và kết nối Discord để tự động xử lý quyên góp và thưởng cho người chơi.

## ✨ Tính năng chính

- **Tích hợp Discord**: Tự động xử lý thông báo quyên góp từ Discord
- **Hệ thống Streak**: Theo dõi chuỗi hoạt động của người chơi
- **Quản lý Token**: Token đóng băng, khôi phục và revert
- **Giao diện Boss Bar**: Hiển thị thời gian streak trực quan trong game
- **Hệ thống phần thưởng**: Tự động trao thưởng dựa trên mức quyên góp và streak
- **Logging chi tiết**: Ghi lại tất cả hoạt động vào file log

## 🛠️ Cài đặt

1. Tải file jar plugin [tại đây](https://github.com/Herzchens/DotmanBankingExpansion/tree/main/plugin)
2. Đặt file vào thư mục `plugins` của server Minecraft
3. Khởi động lại server
4. Chỉnh sửa file `config.yml` theo hướng dẫn bên dưới

## ⚙️ Cấu hình (config.yml)

```yaml
discord:
  token: "your_bot_token"
  guild-id: "your_guild_id"
  channel-id: "your_channel_id"
  accept-user-messages: false

clusters:
  - SkyFree
  - OtherCluster

accepted-methods:
  - Ngân hàng
  - Thẻ Cào

commands:
  10000:
    - "give {player} dirt 1"
  50000:
    - "give {player} coal 1"
  100000:
    - "give {player} emerald 1"

milestone-bonus:
  enabled: true
  amount: 50000
  commands:
    - "give {player} string {bonus_times}"

streak:
  enabled: true
  cycle-hours: 24
  commands:
    3:
      - "say {player} is on a 3-day streak!"
    7:
      - "give {player} netherite_block 1"
```

## 📋 Lệnh

### Lệnh chính
```
/dbe help - Hiển thị trợ giúp
/dbe reload - Tải lại cấu hình plugin
```

### Lệnh streak
```
/dbe streakinfo [player] - Xem thông tin streak
/dbe streak [frozen|restore|revert] [give|take|takeall] <player> [amount] - Give token cho người chơi
/dbe streak resetall - Reset toàn bộ streak
/dbe streak frozen <player> <days> - Đóng băng streak
/dbe streak restore <player> - Khôi phục streak 
/dbe streak revert [player] - Revert streak
/dbe streak set <player> <amount> - Set streak
```

## 🔐 Quyền

| Quyền        | Mô tả                            |
|--------------|----------------------------------|
| `dbe.admin`  | Truy cập toàn bộ tính năng admin |
| `dbe.streak` | Sử dụng các lệnh streak cơ bản   |

## 🎮 Hướng dẫn sử dụng

### Thiết lập Discord
1. Tạo bot Discord và lấy token
2. Điền token vào config.yml
3. Thêm bot vào server Discord của bạn

### Ví dụ xử lý quyên góp
1. Người chơi nạp 150,000 VNĐ
2. Bot Discord nhận thông báo
3. Plugin thực hiện:
   - Cộng streak cho người chơi
   - Trao phần thưởng tương ứng mức 100,000 VNĐ, 50,000VNĐ, 20,000VNĐ, 10,000VNĐ
   - Trao 3 phần thưởng milestone (150,000/50,000 = 3)

## 📊 Hệ thống Token

| Loại Token | Chức năng                                   |
|------------|---------------------------------------------|
| Frozen     | Đóng băng streak không bị mất               |
| Restore    | Khôi phục streak đã hết hạn 1 ngày trước đó |
| Revert     | Khôi phục streak cao nhất từ trước          |

## 📜 Logging
Plugin tự động tạo file log hàng ngày trong thư mục `logs` với định dạng:
```
[HH:mm:ss.SSS] ✅ Command: 'give player dirt 1' - Status: SUCCESS
```

## 🤝 Liên hệ

Gặp vấn đề hoặc cần hỗ trợ? Liên hệ qua Discord: `itztli_herzchen`

---
**Dotman Banking Expansion** - Nâng cấp trải nghiệm quyên góp và tương tác cộng đồng cho server Minecraft của bạn! (Dùng AI nên văn hơi đụt, mong mọi ngừi thông cảm)
