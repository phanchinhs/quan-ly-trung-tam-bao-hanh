# Hệ thống Quản lý Trung tâm Bảo hành & Sửa chữa Thiết bị Điện tử

Bài tập lớn môn Java / Cơ sở dữ liệu — Spring Boot + MySQL + Thymeleaf.

## 1. Công nghệ sử dụng

- Java 17+ (đang chạy ổn trên Java 23), Maven
- Spring Boot 4.1.1: Web, Data JPA, Security, Thymeleaf, Validation, Mail
- MySQL 8.0+ (đang dùng MySQL 9.7), H2 cho test
- Thymeleaf + thymeleaf-extras-springsecurity6
- OpenPDF (xuất PDF), Apache POI (xuất Excel)
- Bảo mật: Spring Security + BCrypt, mã hóa AES/GCM cho SĐT/email/địa chỉ khách hàng

## 2. Chức năng chính

- Dashboard thống kê phiếu, doanh thu, tồn kho
- Quản lý khách hàng (customers): mã hóa SĐT/email, chống trùng qua `phone_hash`
- Quản lý thiết bị (devices): serial duy nhất, theo dõi bảo hành
- Phiếu sửa chữa (repair_orders): luồng `RECEIVED → IN_PROGRESS → WAITING_PARTS → COMPLETED → PAID / CANCELLED`
- Chi tiết sửa chữa (repair_items) + kho linh kiện (spare_parts) + cảnh báo hết hàng
- Báo cáo PDF/Excel, hóa đơn PDF, gửi email (tắt mặc định để demo)
- Nhật ký hệ thống (audit_logs) ghi tự động bằng AOP
- Phân quyền ADMIN / STAFF, trang tra cứu tiến độ sửa chữa

## 3. Cấu trúc project

```
BTL/
├── pom.xml
├── btl_center.sql              # DB full: cấu trúc + 1000+ bản ghi/bảng (2021-2026)
├── truy_van.sql                # 18 câu truy vấn mẫu để nộp/demo cho GV
├── BAO_CAO_HOAT_DONG_5_NAM.md  # Báo cáo hoạt động 5 năm từ số liệu thật
├── src/main/java/com/example/btl/
│   ├── controller/  # Dashboard, Customer, Device, Order, Part, Report, Auth...
│   ├── service/     # Business logic, PDF/Excel, Email, Crypto, Audit
│   ├── repository/  # Spring Data JPA (7 repositories)
│   ├── entity/      # User, Customer, Device, RepairOrder, RepairItem, SparePart, AuditLog
│   ├── config/      # Security, AuditLogAspect, DataSeeder
│   └── util/        # PageUtil, CurrencyUtils...
├── src/main/resources/
│   ├── application.properties
│   └── templates/   # Thymeleaf templates
└── src/test/        # Test với H2 in-memory
```

## 4. Cài đặt & chạy

### Yêu cầu
- JDK 17+, Maven 3.9+, MySQL 8.0+ đang chạy ở `localhost:3306`

### Bước 1 — Import database mẫu (1000+ bản ghi/bảng)
```powershell
# Cách 1: MySQL Workbench -> Open btl_center.sql -> Execute
# Cách 2: dòng lệnh (password root của bạn)
cmd /c '"C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe" -u root -p12345 --default-character-set=utf8mb4 < btl_center.sql'
```

### Bước 2 — Cấu hình kết nối DB
Mở `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/btl_center?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=12345   # <-- đổi thành password MySQL của bạn
```

### Bước 3 — Chạy app
```powershell
mvn spring-boot:run
```
Mở trình duyệt: http://localhost:8080

## 5. Tài khoản đăng nhập

| Username | Password | Quyền |
|----------|----------|-------|
| `admin` | `admin123` | ADMIN — toàn quyền |
| `staff0002` ... `staff1000` | `staff123` | STAFF — kỹ thuật viên |

> Password trong DB đã được hash BCrypt sẵn trong `btl_center.sql`.

## 6. File nộp cho GV

| File | Mục đích |
|------|----------|
| `btl_center.sql` | File CSDL: tạo DB + dữ liệu lớn demo 5 năm |
| `truy_van.sql` | File truy vấn: 18 câu SELECT (cơ bản / JOIN / thống kê / nâng cao) |
| `BAO_CAO_HOAT_DONG_5_NAM.md` | Mô tả hoạt động 2021–2026 từ số liệu thật |

Chạy file truy vấn:
```powershell
cmd /c '"C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe" -u root -p12345 < truy_van.sql'
```

## 7. Ghi chú

- `spring.jpa.hibernate.ddl-auto=update` nên chạy lại không mất dữ liệu. Muốn reset về DB mẫu thì import lại `btl_center.sql`.
- Email đang tắt (`app.email.enabled=false`) để chạy demo không cần SMTP thật.
- Đổi `app.crypto.secret` sang key riêng nếu mang lên production (đổi key thì dữ liệu mã hóa cũ không giải mã được).
