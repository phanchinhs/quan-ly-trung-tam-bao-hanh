# Báo cáo hoạt động 5 năm — Trung tâm Bảo hành & Sửa chữa Thiết bị Điện tử (2021 – 09/2026)

> Nguồn số liệu: database `btl_center` (file `btl_center.sql`), thống kê trực tiếp bằng SQL sau khi import.
> Phạm vi: 01/2021 → 09/2026. Tổng quy mô: **~7.475 bản ghi** trên 7 bảng chính.

## 1. Tổng quan quy mô

| Bảng | Số bản ghi | Mô tả |
|------|------------|-------|
| `users` | 1.000 | 1 ADMIN (`admin`) + 999 kỹ thuật viên (`staff...`) |
| `customers` | 1.000 | Khách hàng cá nhân |
| `devices` | 1.000 | Thiết bị gửi bảo hành / sửa chữa |
| `spare_parts` | 1.000 | Linh kiện trong kho (pin, màn hình, SSD, RAM, nguồn...) |
| `repair_orders` | 1.000 | Phiếu sửa chữa trong 5 năm |
| `repair_items` | 1.475 | Dòng linh kiện / dịch vụ trong phiếu (trung bình ~1,5 dòng/phiếu) |
| `audit_logs` | 1.000 | Nhật ký CREATE / UPDATE / DELETE (ghi tự động bằng AOP) |

Tài khoản mặc định:
- `admin / admin123` — Quản trị viên hệ thống
- `staff... / staff123` — Nhân viên kỹ thuật

## 2. Hoạt động theo từng năm

Thống kê theo `received_at` của `repair_orders`. Doanh thu dưới đây chỉ tính `labor_cost` của phiếu `PAID` (chưa gồm tiền linh kiện trong `repair_items`).

| Năm | Số phiếu | Doanh thu nhân công (VNĐ) | Diễn giải |
|-----|----------|----------------------------|-----------|
| 2021 | 183 | 129.540.000 | Năm đầu vận hành. Chủ yếu pin chai, vỡ màn hình. Khách tập trung Hà Nội & TP.HCM. |
| 2022 | 182 | 130.860.000 | Mở rộng quy mô, bổ sung gần 1.000 kỹ thuật viên, đa dạng kho linh kiện. Năm doanh thu cao nhất. |
| 2023 | 183 | 109.380.000 | Khách cũ quay lại nhiều, laptop & tivi chiếm tỉ trọng cao. |
| 2024 | 183 | 97.800.000 | Hoàn thiện nhật ký `audit_logs` CREATE/UPDATE/DELETE đầy đủ. |
| 2025 | 182 | 81.770.000 | Ổn định quy mô lớn, doanh thu nhân công giảm do nhiều phiếu bảo hành miễn phí / giá ưu đãi. |
| 2026 (9 tháng) | 87 | 29.050.000 | Đang hoạt động dở dang, nhiều phiếu ở trạng thái RECEIVED / IN_PROGRESS / WAITING_PARTS. |
| **Tổng** | **1.000** | **578.400.000** | Trung bình ~180 phiếu/năm đầy đủ, ~578 triệu tiền công sau 5 năm. |

## 3. Trạng thái phiếu sửa chữa

| Trạng thái | Số lượng | Ý nghĩa |
|------------|----------|---------|
| PAID | 771 | Đã thanh toán, ghi nhận doanh thu |
| COMPLETED | 80 | Sửa xong, chờ thanh toán |
| IN_PROGRESS | 61 | Đang sửa chữa |
| CANCELLED | 54 | Khách hủy / không đồng ý giá |
| WAITING_PARTS | 25 | Chờ linh kiện về kho |
| RECEIVED | 9 | Mới tiếp nhận, chưa phân công |
| **Tổng** | **1.000** | Tỉ lệ hoàn thành + thanh toán: **85,1%** |

Nhận xét:
- Tỉ lệ hủy ~5,4%, ở mức chấp nhận được.
- Cuối 2025 – 2026 tồn ~95 phiếu chưa thu tiền (RECEIVED + IN_PROGRESS + WAITING_PARTS + COMPLETED), là dư địa doanh thu short-term.

## 4. Cơ cấu thiết bị

### 4.1. Theo loại thiết bị (`devices.device_type`)

| Loại | Số lượng | Tỉ lệ |
|------|----------|-------|
| PHONE | 400 | 40% |
| LAPTOP | 280 | 28% |
| TABLET | 120 | 12% |
| TV | 80 | 8% |
| DESKTOP | 80 | 8% |
| AUDIO | 40 | 4% |

Điện thoại và laptop chiếm **68%**, là nguồn việc chính. Tivi / desktop ổn định, audio tuy ít nhưng giá trị linh kiện cao.

### 4.2. Theo hãng (`devices.brand`, top 10)

| Hãng | Số lượng |
|------|----------|
| Apple | 176 |
| Dell | 160 |
| Samsung | 137 |
| HP | 80 |
| OPPO | 58 |
| Xiaomi | 58 |
| Nokia | 57 |
| OnePlus | 57 |
| Vivo | 57 |
| Acer | 40 |

Apple / Dell / Samsung chiếm ~47%, phản ánh thị phần phổ biến và giá trị sửa chữa cao (màn hình, pin, main).

## 5. Khách hàng – Thiết bị – Bảo hành

- **1.000 khách hàng** tương ứng **1.000 thiết bị** (trung bình 1 thiết bị/khách trong dữ liệu mẫu).
- Mỗi thiết bị có `serial_number` duy nhất (`uk_devices_serial`), quản lý `purchase_date`, `warranty_end_date` để phân biệt sửa bảo hành vs. sửa dịch vụ.
- SĐT / email / địa chỉ khách hàng được **mã hóa AES/GCM** (tiền tố `ENC:`) khi lưu qua app, kèm `phone_hash` (SHA-256) để kiểm tra trùng và tìm kiếm chính xác.

## 6. Kho linh kiện & chi tiết sửa chữa

- **1.000 mã linh kiện** (`spare_parts`), mỗi mã có `code` duy nhất, `unit_price`, `quantity_in_stock`, `min_stock` để cảnh báo hết hàng.
- **1.475 dòng `repair_items`**: mỗi phiếu dùng trung bình 1–2 loại linh kiện/dịch vụ, liên kết `repair_order_id → repair_orders`, `spare_part_id → spare_parts`.
- Luồng chuẩn: `RECEIVED → IN_PROGRESS → (WAITING_PARTS) → COMPLETED → PAID`, hủy chuyển sang `CANCELLED`.

## 7. Nhật ký hệ thống & quản trị

- **1.000 bản ghi `audit_logs`**: ghi `action`, `entity_name`, `entity_id`, `username`, `details`, `timestamp`, phục vụ truy vết ai tạo/sửa/xóa phiếu, khách hàng, thiết bị.
- Phân quyền Spring Security: `ADMIN` quản trị toàn hệ thống, `STAFF` là kỹ thuật viên xử lý phiếu được gán (`technician_id → users`).
- Giao diện Thymeleaf + xuất báo cáo PDF (OpenPDF) / Excel (POI), gửi email SMTP (tắt mặc định `app.email.enabled=false` để chạy demo).

## 8. Kết luận 5 năm

1. **Tăng trưởng nhanh 2021–2022**, đạt đỉnh doanh thu nhân công ~131 triệu/năm.
2. **2023–2025** chuyển sang giữ chân khách cũ, tỉ trọng laptop/tivi tăng, doanh thu nhân công giảm dần nhưng số phiếu giữ ổn định ~182–183/năm.
3. **2026** vẫn đang hoạt động (87 phiếu sau 9 tháng), tồn kho công việc ~95 phiếu chưa thanh toán.
4. Hệ thống đạt quy mô **1.000+ khách, 1.000+ thiết bị, 1.000+ phiếu, 1.000+ linh kiện, 1.000+ log**, đủ dữ liệu để demo phân trang, tìm kiếm, thống kê doanh thu, tồn kho và hiệu suất kỹ thuật viên.

---
*Tạo tự động từ database thực tế ngày 2026-09-23. Câu lệnh kiểm chứng:*
```sql
SELECT YEAR(received_at), COUNT(*),
       SUM(CASE WHEN status='PAID' THEN labor_cost ELSE 0 END)
FROM repair_orders GROUP BY YEAR(received_at);

SELECT status, COUNT(*) FROM repair_orders GROUP BY status;
SELECT device_type, COUNT(*) FROM devices GROUP BY device_type;
SELECT brand, COUNT(*) FROM devices GROUP BY brand ORDER BY COUNT(*) DESC;
```
