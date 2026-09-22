-- =====================================================================
-- FILE TRUY VẤN CSDL - Hệ thống quản lý trung tâm bảo hành
-- Database: btl_center (MySQL 8.0+)
-- Chạy: mysql -u root -p --default-character-set=utf8mb4 < truy_van.sql
-- Hoặc copy từng câu chạy trong MySQL Workbench
-- =====================================================================
USE `btl_center`;

-- =====================================================================
-- A. TRUY VẤN CƠ BẢN (lọc, tìm kiếm, sắp xếp)
-- =====================================================================

-- Q1. Liệt kê 20 khách hàng mới nhất
SELECT id, full_name, phone, email, address, created_at
FROM customers
ORDER BY created_at DESC
LIMIT 20;

-- Q2. Tìm kiếm khách hàng theo tên (ví dụ tên chứa 'An')
SELECT id, full_name, phone, email
FROM customers
WHERE full_name LIKE '%An%'
ORDER BY full_name
LIMIT 20;

-- Q3. Liệt kê thiết bị còn bảo hành (tính đến hôm nay)
SELECT id, serial_number, brand, model, device_type, customer_id,
       purchase_date, warranty_end_date
FROM devices
WHERE warranty_end_date >= CURDATE()
ORDER BY warranty_end_date
LIMIT 20;

-- Q4. Liệt kê phiếu sửa chữa đang dở dang (chưa thu tiền)
SELECT id, code, device_id, technician_id, status, labor_cost, received_at
FROM repair_orders
WHERE status IN ('RECEIVED', 'IN_PROGRESS', 'WAITING_PARTS', 'COMPLETED')
ORDER BY received_at DESC
LIMIT 20;

-- =====================================================================
-- B. TRUY VẤN JOIN NHIỀU BẢNG
-- =====================================================================

-- Q5. Chi tiết phiếu: mã phiếu + khách hàng + thiết bị + kỹ thuật viên
SELECT ro.code AS ma_phieu, ro.status AS trang_thai,
       c.full_name AS khach_hang, c.phone AS sdt,
       d.serial_number AS serial, d.brand AS hang, d.model AS model,
       u.full_name AS ky_thuat_vien,
       ro.labor_cost AS tien_cong, ro.received_at AS ngay_nhan
FROM repair_orders ro
JOIN devices d      ON d.id = ro.device_id
JOIN customers c    ON c.id = d.customer_id
LEFT JOIN users u   ON u.id = ro.technician_id
ORDER BY ro.received_at DESC
LIMIT 20;

-- Q6. Tìm phiếu theo SĐT khách hàng (dùng phone thật, ví dụ '0900000001')
-- Gợi ý: app mã hóa SĐT nhưng vẫn lưu phone_hash = SHA2(phone) để tra cứu
SELECT ro.code, c.full_name, c.phone, d.serial_number, ro.status
FROM repair_orders ro
JOIN devices d   ON d.id = ro.device_id
JOIN customers c ON c.id = d.customer_id
WHERE c.phone = '0900000001'
   OR c.phone_hash = SHA2('0900000001', 256);

-- Q7. Chi tiết linh kiện trong 1 phiếu (ví dụ phiếu 'SR-2021-00001')
SELECT ro.code AS ma_phieu, ri.name AS linh_kien,
       ri.quantity AS so_luong, ri.unit_price AS don_gia,
       (ri.quantity * ri.unit_price) AS thanh_tien
FROM repair_items ri
JOIN repair_orders ro ON ro.id = ri.repair_order_id
WHERE ro.code = 'SR-2021-00001';

-- Q8. Tổng tiền 1 phiếu = tiền công + tiền linh kiện
SELECT ro.code AS ma_phieu, ro.status,
       ro.labor_cost AS tien_cong,
       IFNULL(SUM(ri.quantity * ri.unit_price), 0) AS tien_linh_kien,
       (ro.labor_cost + IFNULL(SUM(ri.quantity * ri.unit_price), 0)) AS tong_tien
FROM repair_orders ro
LEFT JOIN repair_items ri ON ri.repair_order_id = ro.id
GROUP BY ro.id, ro.code, ro.status, ro.labor_cost
ORDER BY tong_tien DESC
LIMIT 20;

-- =====================================================================
-- C. TRUY VẤN THỐNG KÊ (GROUP BY, HAVING, hàm tổng hợp)
-- =====================================================================

-- Q9. Doanh thu nhân công theo năm (chỉ tính phiếu PAID)
SELECT YEAR(received_at) AS nam, COUNT(*) AS so_phieu,
       SUM(CASE WHEN status = 'PAID' THEN labor_cost ELSE 0 END) AS doanh_thu
FROM repair_orders
GROUP BY YEAR(received_at)
ORDER BY nam;

-- Q10. Doanh thu theo tháng trong năm 2025
SELECT MONTH(received_at) AS thang, COUNT(*) AS so_phieu,
       SUM(CASE WHEN status = 'PAID' THEN labor_cost ELSE 0 END) AS doanh_thu
FROM repair_orders
WHERE YEAR(received_at) = 2025
GROUP BY MONTH(received_at)
ORDER BY thang;

-- Q11. Thống kê phiếu theo trạng thái
SELECT status, COUNT(*) AS so_luong,
       ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM repair_orders), 2) AS ty_le_phan_tram
FROM repair_orders
GROUP BY status
ORDER BY so_luong DESC;

-- Q12. Top 10 kỹ thuật viên sửa nhiều phiếu nhất
SELECT u.username, u.full_name, COUNT(ro.id) AS so_phieu,
       SUM(CASE WHEN ro.status = 'PAID' THEN ro.labor_cost ELSE 0 END) AS doanh_thu
FROM users u
LEFT JOIN repair_orders ro ON ro.technician_id = u.id
WHERE u.role = 'STAFF'
GROUP BY u.id, u.username, u.full_name
ORDER BY so_phieu DESC
LIMIT 10;

-- Q13. Top 10 khách hàng sửa nhiều nhất
SELECT c.id, c.full_name, c.phone, COUNT(ro.id) AS so_phieu
FROM customers c
JOIN devices d        ON d.customer_id = c.id
JOIN repair_orders ro ON ro.device_id = d.id
GROUP BY c.id, c.full_name, c.phone
ORDER BY so_phieu DESC
LIMIT 10;

-- Q14. Thống kê thiết bị theo loại và theo hãng
SELECT device_type, COUNT(*) AS so_luong
FROM devices
GROUP BY device_type
ORDER BY so_luong DESC;

SELECT brand, COUNT(*) AS so_luong
FROM devices
GROUP BY brand
ORDER BY so_luong DESC
LIMIT 10;

-- =====================================================================
-- D. TRUY VẤN NÂNG CAO (HAVING, subquery, ngày tháng, tồn kho)
-- =====================================================================

-- Q15. Linh kiện sắp hết hàng (tồn kho <= định mức tối thiểu)
SELECT code, name, quantity_in_stock, min_stock, unit_price
FROM spare_parts
WHERE quantity_in_stock <= min_stock
ORDER BY quantity_in_stock
LIMIT 20;

-- Q16. Top 10 linh kiện được dùng nhiều nhất trong phiếu sửa
SELECT sp.code, sp.name, SUM(ri.quantity) AS tong_so_luong,
       SUM(ri.quantity * ri.unit_price) AS tong_tien
FROM repair_items ri
JOIN spare_parts sp ON sp.id = ri.spare_part_id
GROUP BY sp.id, sp.code, sp.name
ORDER BY tong_so_luong DESC
LIMIT 10;

-- Q17. Phiếu quá hẹn trả (quá expected_finish_date mà chưa COMPLETED/PAID)
SELECT ro.code, ro.status, c.full_name AS khach_hang,
       ro.received_at, ro.expected_finish_date,
       DATEDIFF(CURDATE(), ro.expected_finish_date) AS so_ngay_tre
FROM repair_orders ro
JOIN devices d   ON d.id = ro.device_id
JOIN customers c ON c.id = d.customer_id
WHERE ro.expected_finish_date < CURDATE()
  AND ro.status NOT IN ('COMPLETED', 'PAID', 'CANCELLED')
ORDER BY so_ngay_tre DESC
LIMIT 20;

-- Q18. Nhật ký hoạt động của 1 kỹ thuật viên (ví dụ 'staff0002')
SELECT action, entity_name, entity_id, username, details, timestamp
FROM audit_logs
WHERE username = 'staff0002'
ORDER BY timestamp DESC
LIMIT 20;
