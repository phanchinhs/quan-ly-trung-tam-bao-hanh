-- =====================================================================
-- HỆ THỐNG QUẢN LÝ TRUNG TÂM BẢO HÀNH VÀ SỬA CHỮA THIẾT BỊ ĐIỆN TỬ
-- File SQL cấu trúc + dữ liệu LỚN database "btl_center"
-- >= 1000 bản ghi cho mỗi bảng (users, customers, devices, spare_parts,
-- repair_orders, repair_items, audit_logs)
-- Yêu cầu MySQL 8.0+ (dùng Recursive CTE).
-- Chạy:  mysql -u root -p --default-character-set=utf8mb4 < btl_center.sql
-- =====================================================================

-- =====================================================================
-- MÔ TẢ HỆ THỐNG HOẠT ĐỘNG TRONG 5 NĂM (01/2021 -> 09/2026)
--   Năm 1 (2021): hệ thống mới vận hành, ~183 phiếu sửa chữa, chủ yếu
--     pin chai / vỡ màn hình, khách tập trung ở Hà Nội & TP.HCM.
--     Các phiếu đã thanh toán (PAID) ghi nhận doanh thu ~129,5 triệu đ.
--   Năm 2 (2022): mở rộng quy mô, bổ sung gần 1000 nhân viên kỹ thuật,
--     kho linh kiện đa dạng (pin, màn hình, SSD, RAM, nguồn...).
--     ~182 phiếu, doanh thu ~130,8 triệu đ.
--   Năm 3 (2023): khách hàng cũ quay lại nhiều, laptop & tivi chiếm
--     tỉ trọng cao. ~183 phiếu, doanh thu ~109,3 triệu đ.
--   Năm 4 (2024): hệ thống ghi nhật ký (audit_logs) CREATE/UPDATE/DELETE
--     đầy đủ. ~183 phiếu, doanh thu ~97,8 triệu đ.
--   Năm 5 (2025-2026): ổn định quy mô lớn; 2025 ~182 phiếu ~81,7 triệu đ,
--     2026 (9 tháng) đã có 87 phiếu mới và đang xử lý dở (RECEIVED /
--     IN_PROGRESS / WAITING_PARTS) => hệ thống vẫn đang hoạt động.
--   KẾT QUẢ 5 NĂM: 1000+ khách hàng, 1000+ thiết bị được bảo hành/sửa
--   chữa, 1000+ phiếu sửa chữa, 1000+ linh kiện trong kho, 1000+ nhật ký.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `btl_center`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `btl_center`;

SET FOREIGN_KEY_CHECKS = 0;
SET SESSION cte_max_recursion_depth = 10000;

-- =====================================================================
-- 1. BẢNG users (tài khoản đăng nhập: quản trị viên / nhân viên kỹ thuật)
-- =====================================================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(50)  NOT NULL,
  `password`   VARCHAR(100) NOT NULL,
  `full_name`  VARCHAR(100) NOT NULL,
  `phone`      VARCHAR(15)  DEFAULT NULL,
  `email`      VARCHAR(100) DEFAULT NULL,
  `role`       VARCHAR(20)  NOT NULL DEFAULT 'STAFF',   -- ADMIN | STAFF
  `active`     BIT(1)       NOT NULL DEFAULT 1,
  `created_at` DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- 2. BẢNG customers (khách hàng)
-- LƯU Ý: phone/email/address được MÃ HÓA (AES/GCM, tiền tố "ENC:") khi app lưu.
--         phone_hash (SHA-256) dùng để kiểm tra trùng SĐT & tìm kiếm chính xác.
--         Dữ liệu SQL dưới đây ghi chữ thường; app tự mã hóa khi cập nhật.
-- =====================================================================
DROP TABLE IF EXISTS `customers`;
CREATE TABLE `customers` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `full_name`  VARCHAR(100) NOT NULL,
  `phone`      VARCHAR(100) NOT NULL,
  `phone_hash` VARCHAR(100) NOT NULL,
  `email`      VARCHAR(255) DEFAULT NULL,
  `address`    VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME(6)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customers_phone_hash` (`phone_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- 3. BẢNG devices (thiết bị của khách hàng)
-- =====================================================================
DROP TABLE IF EXISTS `devices`;
CREATE TABLE `devices` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT,
  `serial_number`     VARCHAR(100) NOT NULL,
  `brand`             VARCHAR(100) DEFAULT NULL,
  `model`             VARCHAR(100) DEFAULT NULL,
  `device_type`       VARCHAR(20)  DEFAULT 'OTHER',
  -- PHONE | LAPTOP | TABLET | DESKTOP | TV | AUDIO | CAMERA | OTHER
  `customer_id`       BIGINT      NOT NULL,
  `purchase_date`     DATE        DEFAULT NULL,
  `warranty_end_date` DATE        DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_devices_serial` (`serial_number`),
  KEY `fk_devices_customer` (`customer_id`),
  CONSTRAINT `fk_devices_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- 4. BẢNG repair_orders (phiếu sửa chữa)
-- =====================================================================
DROP TABLE IF EXISTS `repair_orders`;
CREATE TABLE `repair_orders` (
  `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
  `code`                VARCHAR(30)   NOT NULL,
  `device_id`           BIGINT        NOT NULL,
  `technician_id`       BIGINT        DEFAULT NULL,
  `status`              VARCHAR(20)   NOT NULL DEFAULT 'RECEIVED',
  -- RECEIVED | IN_PROGRESS | WAITING_PARTS | COMPLETED | PAID | CANCELLED
  `issue_description`   VARCHAR(2000) DEFAULT NULL,
  `diagnosis`           VARCHAR(2000) DEFAULT NULL,
  `product_condition`   VARCHAR(1000) DEFAULT NULL,
  `accessories`         VARCHAR(1000) DEFAULT NULL,
  `note`                VARCHAR(1000) DEFAULT NULL,
  `received_at`         DATETIME(6)   NOT NULL,
  `completed_at`        DATETIME(6)   DEFAULT NULL,
  `paid_at`             DATETIME(6)   DEFAULT NULL,
  `expected_finish_date` DATE         DEFAULT NULL,
  `labor_cost`          DECIMAL(12,0) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repair_orders_code` (`code`),
  KEY `fk_orders_device` (`device_id`),
  KEY `fk_orders_technician` (`technician_id`),
  CONSTRAINT `fk_orders_device`     FOREIGN KEY (`device_id`)     REFERENCES `devices` (`id`),
  CONSTRAINT `fk_orders_technician` FOREIGN KEY (`technician_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- 5. BẢNG repair_items (linh kiện / dịch vụ trong phiếu sửa chữa)
-- =====================================================================
DROP TABLE IF EXISTS `repair_items`;
CREATE TABLE `repair_items` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `repair_order_id` BIGINT        NOT NULL,
  `spare_part_id`   BIGINT        DEFAULT NULL,
  `name`            VARCHAR(150)  NOT NULL,
  `quantity`        INT           NOT NULL DEFAULT 1,
  `unit_price`      DECIMAL(12,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `fk_items_order` (`repair_order_id`),
  KEY `fk_items_part` (`spare_part_id`),
  CONSTRAINT `fk_items_order` FOREIGN KEY (`repair_order_id`) REFERENCES `repair_orders` (`id`),
  CONSTRAINT `fk_items_part`   FOREIGN KEY (`spare_part_id`)   REFERENCES `spare_parts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- 6. BẢNG spare_parts (linh kiện trong kho)
-- =====================================================================
DROP TABLE IF EXISTS `spare_parts`;
CREATE TABLE `spare_parts` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT,
  `code`              VARCHAR(50)   NOT NULL,
  `name`              VARCHAR(150)  NOT NULL,
  `description`       VARCHAR(500)  DEFAULT NULL,
  `unit_price`        DECIMAL(12,0) NOT NULL DEFAULT 0,
  `quantity_in_stock` INT           NOT NULL DEFAULT 0,
  `min_stock`         INT           NOT NULL DEFAULT 0,
  `created_at`        DATETIME(6)   NOT NULL,
  `updated_at`        DATETIME(6)   DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spare_parts_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- 7. BẢNG audit_logs (nhật ký hoạt động - ghi tự động bằng AOP)
-- =====================================================================
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `action`      VARCHAR(20)   NOT NULL,
  `entity_name` VARCHAR(50)   NOT NULL,
  `entity_id`   BIGINT        DEFAULT NULL,
  `username`    VARCHAR(50)   NOT NULL,
  `details`     VARCHAR(500)  DEFAULT NULL,
  `timestamp`   DATETIME(6)   NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_entity` (`entity_name`, `entity_id`),
  KEY `idx_audit_user`   (`username`),
  KEY `idx_audit_time`   (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- DỮ LIỆU MẪU LỚN - 1000+ BẢN GHI / BẢNG (mô phỏng 5 năm hoạt động)
-- password mặc định: admin/admin123, còn lại staff/staff123
-- =====================================================================

-- -------------------------------------------------------------------------------
-- 2.1 USERS: tài khoản admin + 999 nhân viên kỹ thuật (TỔNG 1000)
--     BCrypt hash = cùng chuỗi hash của "admin123" / "staff123"
-- -------------------------------------------------------------------------------
INSERT INTO `users` (`username`, `password`, `full_name`, `phone`, `email`, `role`, `active`, `created_at`)
SELECT t.username, t.password, t.full_name, t.phone, t.email, t.role, 1, t.created_at FROM (
  SELECT
    'admin'                                                    AS username,
    '$2a$10$pM3Phm/34N5aojlEV67VAufeKTSbQuBDrlQdVLCnZMXVwNY1Z8pIG' AS password,
    'Quản trị viên hệ thống'                                   AS full_name,
    NULL                                                       AS phone,
    NULL                                                       AS email,
    'ADMIN'                                                    AS role,
    '2021-01-01 08:00:00'                                      AS created_at
  UNION ALL
  SELECT
    CONCAT('staff', LPAD(n, 4, '0')),
    '$2a$10$clUwPcnC6x/l4udnT2LHFOLfg8M3hhpFec3h4C1AELolKuy9H60rm',
    CONCAT('Kỹ thuật viên ',
           ELT(1 + ((n - 2) % 10), 'Nguyễn','Trần','Lê','Phạm','Hoàng','Đặng','Bùi','Đỗ','Hồ','Ngô'),
           ' ',
           ELT(1 + ((n - 2) % 8), 'Văn','Hữu','Minh','Đức','Quốc','Tiến','Duy','Trí')),
    CONCAT('098', LPAD(1000 + n, 7, '0')),
    CONCAT('nhanvien', LPAD(n, 4, '0'), '@btl-center.com'),
    'STAFF',
    DATE_ADD('2021-01-15', INTERVAL ((n - 2) * 2) DAY)
  FROM (
    WITH RECURSIVE seq AS (SELECT 2 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
    SELECT n FROM seq
  ) nums
) t;

-- -------------------------------------------------------------------------------
-- 2.2 CUSTOMERS: 1000 khách hàng (tên tiếng Việt, SĐT, email, địa chỉ)
--     phone_hash = SHA-256 của phone (đúng cơ chế app để tra cứu được)
-- -------------------------------------------------------------------------------
INSERT INTO `customers` (`full_name`, `phone`, `phone_hash`, `email`, `address`, `created_at`)
SELECT
  CONCAT(
    ELT(1 + ((n - 1) % 16), 'Nguyễn','Trần','Lê','Phạm','Hoàng','Huỳnh','Phan','Vũ','Võ','Đặng','Bùi','Đỗ','Hồ','Ngô','Dương','Lý'),
    ' ',
    IF(n % 2 = 0,
       ELT(1 + ((n * 7) % 12), 'Văn','Hữu','Quốc','Minh','Đức','Thành','Ngọc','Tiến','Duy','Trí','Đình','Công'),
       ELT(1 + ((n * 11) % 11), 'Thị','Ngọc','Thanh','Kim','Thu','Mai','Linh','Phương','Hồng','Cẩm','Thúy')),
    ' ',
    IF(n % 2 = 0,
       ELT(1 + ((n * 13) % 20), 'An','Bình','Cường','Dũng','Giang','Hải','Hoàng','Khang','Khoa','Long','Minh','Nam','Phong','Quân','Sơn','Thắng','Tuấn','Vinh','Xuân','Hùng'),
       ELT(1 + ((n * 17) % 20), 'Anh','Bình','Cúc','Diệu','Giang','Hằng','Lan','Mai','Nga','Oanh','Phượng','Quyên','Trang','Uyên','Yến','Hạnh','Thảo','Lệ','Xuân','Ly'))
  ),
  CONCAT('09', LPAD(n, 8, '0')),
  SHA2(CONCAT('09', LPAD(n, 8, '0')), 256),
  CONCAT('khach', LPAD(n, 4, '0'), '@gmail.com'),
  CONCAT(
    ELT(1 + ((n - 1) % 10), '12','45','78','30','56','21','102','77','5','64'),
    ' ',
    ELT(1 + ((n * 3) % 10), 'Nguyễn Trãi','Lê Lợi','Bà Triệu','Hoàng Hoa Thám','Hai Bà Trưng','Đinh Tiên Hoàng','Trần Phú','Lý Thường Kiệt','Nguyễn Huệ','Cách Mạng Tháng 8'),
    ', ',
    ELT(1 + ((n * 5) % 16), 'Hà Nội','TP.HCM','Đà Nẵng','Hải Phòng','Cần Thơ','Huế','Nha Trang','Vũng Tàu','Biên Hòa','Đà Lạt','Quy Nhơn','Thái Nguyên','Nam Định','Vinh','Bắc Ninh','Long An')
  ),
  DATE_ADD('2021-01-05', INTERVAL ((n - 1) * 2) DAY)
FROM (
  WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
  SELECT n FROM seq
) nums;

-- -------------------------------------------------------------------------------
-- 2.3 SPARE_PARTS: 1000 linh kiện trong kho (mã SP-0001..SP-1000)
--     Một số mã cố ý để tồn thấp (min_stock) để demo cảnh báo hết hàng
-- -------------------------------------------------------------------------------
INSERT INTO `spare_parts` (`code`, `name`, `description`, `unit_price`, `quantity_in_stock`, `min_stock`, `created_at`, `updated_at`)
SELECT
  CONCAT('SP-', LPAD(n, 4, '0')),
  ELT(1 + ((n - 1) % 30),
    'Pin MacBook nguyên hộp','Keo tản nhiệt','Bo nguồn Samsung','Màn hình IPS 15.6"',
    'Camera sau iPhone','Bàn phím Lenovo','SSD 256GB NVMe','SSD 512GB NVMe',
    'RAM DDR4 8GB','RAM DDR4 16GB','Cáp sạc USB-C 1m','Bộ sạc nhanh 65W',
    'Module WiFi6','Quạt tản nhiệt laptop','Màn hình Retina','Bản lề laptop',
    'Loa laptop stereo','Micro thu âm','Cảm biến vân tay','Pin sạc dự phòng',
    'Mạch sạc Type-C','Chip CPU laptop','Card đồ họa','Bộ nhớ eMMC 64GB',
    'Màn hình OLED 6.7"','Kính cường lực','Ốp lưng silicon','Tai nghe Bluetooth',
    'Đế sạc không dây','Cáp HDMI 2.1'),
  CONCAT('Linh kiện chính hãng - ',
    ELT(1 + ((n - 1) % 30),
      'Pin','Keo','Bo mạch','Màn hình','Camera','Bàn phím','Ổ cứng','Ổ cứng',
      'RAM','RAM','Cáp','Sạc','Wifi','Quạt','Màn hình','Bản lề',
      'Loa','Micro','Cảm biến','Pin','Mạch','CPU','GPU','ROM',
      'Màn hình','Phụ kiện','Phụ kiện','Tai nghe','Sạc','Cáp'),
    ' - dùng cho thiết bị điện tử'),
  (100000 + ((n * 37) % 190) * 10000) * 1,
  IF(n % 9 = 0, n % 5, 5 + ((n * 3) % 46)),
  2,
  DATE_ADD('2021-01-10', INTERVAL n DAY),
  DATE_ADD('2021-01-10', INTERVAL n DAY)
FROM (
  WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
  SELECT n FROM seq
) nums;

-- -------------------------------------------------------------------------------
-- 2.4 DEVICES: 1000 thiết bị (khách 1..800 mỗi khách 1 thiết bị,
--     khách 1..200 thêm thiết bị thứ 2; khách 801..1000 là khách mới)
-- -------------------------------------------------------------------------------
INSERT INTO `devices` (`serial_number`, `brand`, `model`, `device_type`, `customer_id`, `purchase_date`, `warranty_end_date`)
SELECT
  CONCAT(
    ELT(1 + ((n - 1) % 10), 'APL','SAM','DLN','LEN','HPV','SON','XIA','OPP','ACA','ASU'),
    '-',
    LPAD(20 + ((n - 1) % 6), 2, '0'),
    '-',
    LPAD(n, 6, '0')),
  CASE (n % 25)
    WHEN 0 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 1 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 2 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 3 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 4 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 5 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 6 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 7 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 8 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 9 THEN ELT(1 + ((n * 5) % 7), 'Apple','Samsung','Xiaomi','OPPO','Vivo','Nokia','OnePlus')
    WHEN 10 THEN ELT(1 + ((n * 7) % 5), 'Dell','Lenovo','HP','ASUS','Acer')
    WHEN 11 THEN ELT(1 + ((n * 7) % 5), 'Dell','Lenovo','HP','ASUS','Acer')
    WHEN 12 THEN ELT(1 + ((n * 7) % 5), 'Dell','Lenovo','HP','ASUS','Acer')
    WHEN 13 THEN ELT(1 + ((n * 7) % 5), 'Dell','Lenovo','HP','ASUS','Acer')
    WHEN 14 THEN ELT(1 + ((n * 7) % 5), 'Dell','Lenovo','HP','ASUS','Acer')
    WHEN 15 THEN ELT(1 + ((n * 7) % 5), 'Dell','Lenovo','HP','ASUS','Acer')
    WHEN 16 THEN ELT(1 + ((n * 7) % 5), 'Dell','Lenovo','HP','ASUS','Acer')
    WHEN 17 THEN ELT(1 + ((n * 3) % 3), 'Apple','Samsung','Lenovo')
    WHEN 18 THEN ELT(1 + ((n * 3) % 3), 'Apple','Samsung','Lenovo')
    WHEN 19 THEN ELT(1 + ((n * 3) % 3), 'Apple','Samsung','Lenovo')
    WHEN 20 THEN ELT(1 + ((n * 3) % 3), 'Samsung','LG','Sony')
    WHEN 21 THEN ELT(1 + ((n * 3) % 3), 'Samsung','LG','Sony')
    WHEN 22 THEN ELT(1 + ((n * 3) % 3), 'Dell','HP','Lenovo')
    WHEN 23 THEN ELT(1 + ((n * 3) % 3), 'Dell','HP','Lenovo')
    ELSE      ELT(1 + ((n * 3) % 3), 'Sony','JBL','Bose')
  END,
  CASE (n % 25)
    WHEN 0 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 1 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 2 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 3 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 4 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 5 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 6 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 7 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 8 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 9 THEN ELT(1 + ((n * 5) % 6), 'iPhone 14','iPhone 15','Galaxy S23','Redmi Note 12','OPPO Reno8','Vivo V25')
    WHEN 10 THEN ELT(1 + ((n * 7) % 6), 'IdeaPad 5 Pro','XPS 15 9530','Pavilion 14','ROG Strix','Aspire 5','Vostro 14')
    WHEN 11 THEN ELT(1 + ((n * 7) % 6), 'IdeaPad 5 Pro','XPS 15 9530','Pavilion 14','ROG Strix','Aspire 5','Vostro 14')
    WHEN 12 THEN ELT(1 + ((n * 7) % 6), 'IdeaPad 5 Pro','XPS 15 9530','Pavilion 14','ROG Strix','Aspire 5','Vostro 14')
    WHEN 13 THEN ELT(1 + ((n * 7) % 6), 'IdeaPad 5 Pro','XPS 15 9530','Pavilion 14','ROG Strix','Aspire 5','Vostro 14')
    WHEN 14 THEN ELT(1 + ((n * 7) % 6), 'IdeaPad 5 Pro','XPS 15 9530','Pavilion 14','ROG Strix','Aspire 5','Vostro 14')
    WHEN 15 THEN ELT(1 + ((n * 7) % 6), 'IdeaPad 5 Pro','XPS 15 9530','Pavilion 14','ROG Strix','Aspire 5','Vostro 14')
    WHEN 16 THEN ELT(1 + ((n * 7) % 6), 'IdeaPad 5 Pro','XPS 15 9530','Pavilion 14','ROG Strix','Aspire 5','Vostro 14')
    WHEN 17 THEN ELT(1 + ((n * 3) % 3), 'iPad Air M1','Galaxy Tab S8','Tab P11')
    WHEN 18 THEN ELT(1 + ((n * 3) % 3), 'iPad Air M1','Galaxy Tab S8','Tab P11')
    WHEN 19 THEN ELT(1 + ((n * 3) % 3), 'iPad Air M1','Galaxy Tab S8','Tab P11')
    WHEN 20 THEN ELT(1 + ((n * 3) % 3), 'QN77S90C','C3 55"','Bravia X90')
    WHEN 21 THEN ELT(1 + ((n * 3) % 3), 'QN77S90C','C3 55"','Bravia X90')
    WHEN 22 THEN ELT(1 + ((n * 3) % 3), 'OptiPlex 7010','EliteDesk 600','ThinkCentre M70')
    WHEN 23 THEN ELT(1 + ((n * 3) % 3), 'OptiPlex 7010','EliteDesk 600','ThinkCentre M70')
    ELSE      ELT(1 + ((n * 3) % 3), 'WH-1000XM5','Flip 5','QuietComfort 45')
  END,
  CASE (n % 25)
    WHEN 0 THEN 'PHONE' WHEN 1 THEN 'PHONE' WHEN 2 THEN 'PHONE'
    WHEN 3 THEN 'PHONE' WHEN 4 THEN 'PHONE' WHEN 5 THEN 'PHONE'
    WHEN 6 THEN 'PHONE' WHEN 7 THEN 'PHONE' WHEN 8 THEN 'PHONE' WHEN 9 THEN 'PHONE'
    WHEN 10 THEN 'LAPTOP' WHEN 11 THEN 'LAPTOP' WHEN 12 THEN 'LAPTOP'
    WHEN 13 THEN 'LAPTOP' WHEN 14 THEN 'LAPTOP' WHEN 15 THEN 'LAPTOP' WHEN 16 THEN 'LAPTOP'
    WHEN 17 THEN 'TABLET' WHEN 18 THEN 'TABLET' WHEN 19 THEN 'TABLET'
    WHEN 20 THEN 'TV' WHEN 21 THEN 'TV'
    WHEN 22 THEN 'DESKTOP' WHEN 23 THEN 'DESKTOP'
    ELSE 'AUDIO'
  END,
  IF(n <= 800, n, n - 800),
  DATE_SUB(CURDATE(), INTERVAL (30 + ((n * 17) % 2100)) DAY),
  DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (30 + ((n * 17) % 2100)) DAY),
           INTERVAL (12 + ((n * 7) % 13)) MONTH)
FROM (
  WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
  SELECT n FROM seq
) nums;

-- -------------------------------------------------------------------------------
-- 2.5 REPAIR_ORDERS: 1000 phiếu sửa chữa trải đều 5 năm (01/2021 -> 09/2026)
--     Trung bình 2 ngày 1 phiếu. Phân bố trạng thái theo độ tuổi phiếu:
--       - Phiếu cũ (2021-2022): 95% PAID, 5% CANCELLED
--       - Năm 2023:            ~80% PAID, 10% COMPLETED, 5% WAITING_PARTS, 5% CANCELLED
--       - Năm 2024:            ~70% PAID, 15% COMPLETED, 10% IN_PROGRESS, 5% CANCELLED
--       - Năm 2025:            ~55% PAID, 20% COMPLETED, 15% IN_PROGRESS, 10% CANCELLED
--       - Năm 2026 (9 tháng):  phiếu mới nhiều, chưa thanh toán dứt điểm
-- -------------------------------------------------------------------------------
INSERT INTO `repair_orders` (`code`, `device_id`, `technician_id`, `status`, `issue_description`, `diagnosis`, `product_condition`, `accessories`, `note`, `received_at`, `completed_at`, `paid_at`, `expected_finish_date`, `labor_cost`)
SELECT
  e.code, e.device_id, e.technician_id, e.status,
  e.issue_description, e.diagnosis, e.product_condition, e.accessories, e.note,
  e.received_at,
  CASE WHEN e.status IN ('PAID','COMPLETED') THEN DATE_ADD(e.received_at, INTERVAL (3 + (e.n % 9)) DAY) ELSE NULL END,
  CASE WHEN e.status = 'PAID'                THEN DATE_ADD(e.received_at, INTERVAL (5 + (e.n % 9)) DAY) ELSE NULL END,
  DATE_ADD(DATE(e.received_at), INTERVAL (7 + (e.n % 7)) DAY),
  e.labor_cost
FROM (
  SELECT
    n,
    CONCAT('SR-', YEAR(DATE_ADD('2021-01-01', INTERVAL ((n - 1) * 2) DAY)), '-', LPAD(n, 5, '0')) AS code,
    1 + ((n - 1) % 1000) AS device_id,
    IF(n % 13 = 0, NULL, 2 + ((n * 11) % 998)) AS technician_id,
    CASE
      WHEN YEAR(DATE_ADD('2021-01-01', INTERVAL ((n - 1) * 2) DAY)) <= 2022 THEN
        IF(n % 20 = 0, 'CANCELLED', 'PAID')
      WHEN YEAR(DATE_ADD('2021-01-01', INTERVAL ((n - 1) * 2) DAY)) = 2023 THEN
        CASE WHEN n % 20 = 0 THEN 'CANCELLED'
             WHEN n % 20 = 1 THEN 'WAITING_PARTS'
             WHEN n % 20 IN (2,3) THEN 'COMPLETED'
             ELSE 'PAID' END
      WHEN YEAR(DATE_ADD('2021-01-01', INTERVAL ((n - 1) * 2) DAY)) = 2024 THEN
        CASE WHEN n % 20 = 0 THEN 'CANCELLED'
             WHEN n % 20 IN (1,2) THEN 'IN_PROGRESS'
             WHEN n % 20 IN (3,4,5) THEN 'COMPLETED'
             ELSE 'PAID' END
      WHEN YEAR(DATE_ADD('2021-01-01', INTERVAL ((n - 1) * 2) DAY)) = 2025 THEN
        CASE WHEN n % 20 IN (0,1) THEN 'CANCELLED'
             WHEN n % 20 IN (2,3,4) THEN 'IN_PROGRESS'
             WHEN n % 20 IN (5,6,7) THEN 'COMPLETED'
             ELSE 'PAID' END
      ELSE
        CASE WHEN n % 20 IN (0,1) THEN 'RECEIVED'
             WHEN n % 20 IN (2,3,4,5) THEN 'WAITING_PARTS'
             WHEN n % 20 IN (6,7,8,9) THEN 'IN_PROGRESS'
             WHEN n % 20 IN (10,11) THEN 'COMPLETED'
             ELSE 'PAID' END
    END AS status,
    ELT(1 + (n % 14),
      'Pin chai, nhanh hết pin','Màn hình bị va đập, cảm ứng kém','Không lên nguồn',
      'Bàn phím mất chữ, tiếng quạt ồn','Camera mờ','Không nhận sạc','Loa rè',
      'Màn hình sọc','Ổ cứng kêu lạch cạch','Bản lề gãy','Mất pixel trên màn',
      'Nút nguồn kẹt','Nhiễu bluetooth','Sạc nóng quá mức') AS issue_description,
    IF(n % 5 = 0, NULL, ELT(1 + (n % 12),
      'Thay pin mới','Thay màn hình','Thay bo nguồn','Thay bàn phím','Thay ổ cứng SSD',
      'Vệ sinh quạt - thay dầu','Thay camera sau','Thay cổng sạc','Thay bản lề',
      'Thay loa','Thay nguồn sạc','Hiệu chỉnh cảm ứng')) AS diagnosis,
    ELT(1 + ((n * 7) % 10),
      'Máy nguyên vẹn','Trầy xước nhẹ vỏ','Máy còn mới nguyên','Vỡ kính màn hình',
      'Máy có vết trầy','Bọc case bảo vệ tốt','Máy bị móp góc','Máy cũ, trầy xước nhiều',
      'Máy còn đẹp','Vỏ ngoài ổn định') AS product_condition,
    IF(n % 7 = 0, NULL, ELT(1 + ((n * 3) % 9),
      'Sạc, cáp','Pin cũ','Cáp sạc','Sạc gốc','Ốp lưng, kính cường lực',
      'Tai nghe, sạc','Cáp Lightning','Bộ sạc nhanh','Không có đi kèm')) AS accessories,
    IF(n % 6 = 0, NULL, ELT(1 + ((n * 5) % 8),
      'Bảo hành đã hết hạn - tính phí','Còn bảo hành','Khách yêu cầu nhanh',
      'Linh kiện cần đặt hàng','Khách đồng ý chi phí','Đã liên hệ khách xác nhận',
      'Sửa xong đã bàn giao','Chờ xác nhận giá')) AS note,
    DATE_ADD(DATE_ADD('2021-01-01', INTERVAL ((n - 1) * 2) DAY), INTERVAL (8 + (n % 9)) HOUR) AS received_at,
    (150000 + ((n * 29) % 120) * 10000) * 1 AS labor_cost
  FROM (
    WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
    SELECT n FROM seq
  ) nums
) e;

-- -------------------------------------------------------------------------------
-- 2.6 REPAIR_ITEMS: linh kiện/dịch vụ trong phiếu
--     Mỗi phiếu 1 linh kiện + thêm cho phiếu %3 và %7 -> TỔNG ~1475 dòng
-- -------------------------------------------------------------------------------
INSERT INTO `repair_items` (`repair_order_id`, `spare_part_id`, `name`, `quantity`, `unit_price`)
SELECT
  e.n,
  sp.id,
  sp.name,
  1 + (e.n % 3),
  sp.unit_price
FROM (
  WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
  SELECT n FROM seq
) e
INNER JOIN `spare_parts` sp ON sp.id = 1 + ((e.n - 1) % 1000);

INSERT INTO `repair_items` (`repair_order_id`, `spare_part_id`, `name`, `quantity`, `unit_price`)
SELECT
  e.n * 3,
  sp.id,
  sp.name,
  1,
  sp.unit_price
FROM (
  WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
  SELECT n FROM seq
) e
INNER JOIN `spare_parts` sp ON sp.id = 1 + ((e.n * 5 - 1) % 1000)
WHERE e.n * 3 <= 1000;

INSERT INTO `repair_items` (`repair_order_id`, `spare_part_id`, `name`, `quantity`, `unit_price`)
SELECT
  e.n * 7,
  sp.id,
  sp.name,
  1 + (e.n % 2),
  sp.unit_price
FROM (
  WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
  SELECT n FROM seq
) e
INNER JOIN `spare_parts` sp ON sp.id = 1 + ((e.n * 9 - 1) % 1000)
WHERE e.n * 7 <= 1000;

-- -------------------------------------------------------------------------------
-- 2.7 AUDIT_LOGS: 1000 nhật ký hoạt động trải đều 5 năm
--     Ghi lại CREATE/UPDATE/DELETE trên các thực thể của hệ thống
-- -------------------------------------------------------------------------------
INSERT INTO `audit_logs` (`action`, `entity_name`, `entity_id`, `username`, `details`, `timestamp`)
SELECT
  ELT(1 + (n % 3), 'CREATE', 'UPDATE', 'DELETE'),
  ELT(1 + (n % 5), 'Customer', 'Device', 'RepairOrder', 'SparePart', 'User'),
  IF(n % 5 = 4, 1, 1 + ((n - 1) % 1000)),
  IF(n % 8 = 0, 'admin', CONCAT('staff', LPAD(2 + (n % 997), 4, '0'))),
  CONCAT('Thao tac ',
         ELT(1 + (n % 3), 'tao moi', 'cap nhat', 'xoa'),
         ' ban ghi thu ', 1 + ((n - 1) % 1000),
         ' cua bang ',
         ELT(1 + (n % 5), 'Customer', 'Device', 'RepairOrder', 'SparePart', 'User')),
  DATE_ADD(DATE_ADD('2021-01-02', INTERVAL ((n - 1) * 2) DAY), INTERVAL (9 + (n % 8)) HOUR)
FROM (
  WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
  SELECT n FROM seq
) nums;

-- =====================================================================
-- 3. KIỂM TRA SỐ LƯỢNG BẢN GHI + THỐNG KÊ 5 NĂM
-- =====================================================================
SELECT 'users' AS bang, COUNT(*) AS so_ban_ghi FROM users
UNION ALL SELECT 'customers', COUNT(*) FROM customers
UNION ALL SELECT 'devices', COUNT(*) FROM devices
UNION ALL SELECT 'spare_parts', COUNT(*) FROM spare_parts
UNION ALL SELECT 'repair_orders', COUNT(*) FROM repair_orders
UNION ALL SELECT 'repair_items', COUNT(*) FROM repair_items
UNION ALL SELECT 'audit_logs', COUNT(*) FROM audit_logs;

SELECT YEAR(received_at) AS nam, COUNT(*) AS so_phieu,
       SUM(CASE WHEN status = 'PAID' THEN labor_cost ELSE 0 END) AS doanh_thu
FROM repair_orders
GROUP BY YEAR(received_at)
ORDER BY nam;

SELECT status, COUNT(*) AS so_luong FROM repair_orders GROUP BY status;

SET FOREIGN_KEY_CHECKS = 1;