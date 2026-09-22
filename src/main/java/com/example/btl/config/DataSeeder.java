package com.example.btl.config;

import com.example.btl.entity.Customer;
import com.example.btl.entity.Device;
import com.example.btl.entity.DeviceType;
import com.example.btl.entity.Role;
import com.example.btl.entity.RepairOrder;
import com.example.btl.entity.RepairStatus;
import com.example.btl.entity.SparePart;
import com.example.btl.entity.UserAccount;
import com.example.btl.repository.CustomerRepository;
import com.example.btl.repository.DeviceRepository;
import com.example.btl.repository.RepairOrderRepository;
import com.example.btl.repository.SparePartRepository;
import com.example.btl.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DeviceRepository deviceRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final SparePartRepository sparePartRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      CustomerRepository customerRepository,
                      DeviceRepository deviceRepository,
                      RepairOrderRepository repairOrderRepository,
                      SparePartRepository sparePartRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.deviceRepository = deviceRepository;
        this.repairOrderRepository = repairOrderRepository;
        this.sparePartRepository = sparePartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        if (customerRepository.count() == 0 && repairOrderRepository.count() == 0) {
            seedSampleData();
            log.info("Đã tạo dữ liệu mẫu cho lần chạy đầu tiên.");
        }
        seedSpareParts();
    }

    private void seedUsers() {
        ensureDefaultUser("admin", "admin123", "Quản trị viên hệ thống", Role.ADMIN);
        ensureDefaultUser("staff", "staff123", "Nguyễn Văn Kỹ Thuật", Role.STAFF);
    }

    private void ensureDefaultUser(String username, String rawPassword, String fullName, Role role) {
        Optional<UserAccount> existing = userRepository.findByUsername(username);
        if (existing.isEmpty()) {
            UserAccount account = new UserAccount();
            account.setUsername(username);
            account.setPassword(passwordEncoder.encode(rawPassword));
            account.setFullName(fullName);
            account.setRole(role);
            account.setActive(true);
            if (role == Role.STAFF) {
                account.setPhone("0987654321");
            }
            userRepository.save(account);
            log.info("Đã tạo tài khoản mặc định: {}/{}", username, rawPassword);
            return;
        }
        String stored = existing.get().getPassword();
        if (stored == null || !stored.matches("\\$2[aby]\\$.*") || !passwordEncoder.matches(rawPassword, stored)) {
            existing.get().setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(existing.get());
            log.warn("Phát hiện mật khẩu {} không hợp lệ - đã đặt lại về {} / {}", username, username, rawPassword);
        }
    }

    private void seedSampleData() {
        LocalDate today = LocalDate.now();

        Customer kh1 = customer("Nguyễn Văn An", "0901123456", "an.nguyen@gmail.com", "12 Nguyễn Trãi, Hà Nội");
        Customer kh2 = customer("Trần Thị Bình", "0912233445", "binh.tran@gmail.com", "45 Lê Lợi, TP.HCM");
        Customer kh3 = customer("Lê Hoàng Minh", "0988776655", "minh.le@yahoo.com", "78 Hoàng Hoa Thám, Đà Nẵng");
        customerRepository.saveAll(java.util.List.of(kh1, kh2, kh3));

        Device dt1 = device(kh1, "IPX-2025-000123", "Apple", "iPhone 14", DeviceType.PHONE,
                today.minusMonths(8), today.plusMonths(4));
        Device dt2 = device(kh2, "LEN-2409-88A2K", "Lenovo", "IdeaPad 5 Pro", DeviceType.LAPTOP,
                today.minusMonths(5), today.minusMonths(2));
        Device dt3 = device(kh3, "SAM-TV-77-9001", "Samsung", "QN77S90C", DeviceType.TV,
                today.minusMonths(10), today.plusMonths(2));
        Device dt4 = device(kh1, "XIA-R9-334455", "Xiaomi", "Redmi Note 12", DeviceType.PHONE,
                today.minusMonths(3), today.plusMonths(9));
        Device dt5 = device(kh3, "DLL-XPS-15-021", "Dell", "XPS 15 9530", DeviceType.LAPTOP,
                today.minusWeeks(2), today.plusYears(1));
        deviceRepository.saveAll(java.util.List.of(dt1, dt2, dt3, dt4, dt5));

        UserAccount staff = userRepository.findByUsername("staff").orElse(null);

        RepairOrder o1 = order("SR-" + Year.now().getValue() + "-0001", dt2, staff, RepairStatus.COMPLETED,
                "Pin chai, nhanh hết pin, máy nóng", "Thay pin mới", "Máy nguyên vẹn", "Pin cũ",
                "Bảo hành đã hết hạn - tính phí thay pin", today.minusDays(4), today.minusDays(1), today.minusDays(1),
                new BigDecimal("350000"));
        RepairOrder o2 = order("SR-" + Year.now().getValue() + "-0002", dt1, staff, RepairStatus.IN_PROGRESS,
                "Màn hình bị va đập, cảm ứng kém ở góc", "Đang chờ linh kiện màn hình", "Trầy xước nhẹ vỏ", "Sạc, cáp",
                "Thiết bị còn trong thời hạn bảo hành", today.minusDays(2), today.plusDays(3), null,
                new BigDecimal("200000"));
        RepairOrder o3 = order("SR-" + Year.now().getValue() + "-0003", dt3, staff, RepairStatus.PAID,
                "Không lên nguồn", "Hỏng bo nguồn, đã thay mới", "Vỏ tivi còn mới", null,
                "Sửa xong đã bàn giao", today.minusDays(10), today.minusDays(6), today.minusDays(5),
                new BigDecimal("850000"));
        RepairOrder o4 = order("SR-" + Year.now().getValue() + "-0004", dt5, null, RepairStatus.RECEIVED,
                "Bàn phím mất chữ, tiếng quạt ồn", null, "Máy còn mới nguyên", "Cáp sạc",
                "Khách mới gửi, chờ phân công kỹ thuật", today, today.plusDays(2), null,
                new BigDecimal("0"));
        repairOrderRepository.saveAll(java.util.List.of(o1, o2, o3, o4));
    }

    private void seedSpareParts() {
        if (sparePartRepository.count() > 0) {
            return;
        }
        List<SparePart> parts = new java.util.ArrayList<>();
        parts.add(part("SP-001", "Pin MacBook nguyên hộp", "Pin chính hãng cho MacBook",
                new BigDecimal("850000"), 5, 3));
        parts.add(part("SP-002", "Keo tản nhiệt", "Keo nhiệt CPU/GPU",
                new BigDecimal("50000"), 20, 5));
        parts.add(part("SP-003", "Bo nguồn TV Samsung", "Bo nguồn cho TV Samsung",
                new BigDecimal("1200000"), 2, 2));
        parts.add(part("SP-004", "Bảo trì dàn loa", "Vệ sinh & bảo trì loa",
                new BigDecimal("150000"), 30, 5));
        parts.add(part("SP-005", "Màn hình laptop Dell 15.6\"", "Màn hình IPS cho Dell XPS",
                new BigDecimal("1800000"), 3, 2));
        parts.add(part("SP-006", "Pin Lenovo nguyên hộp", "Pin chính hãng IdeaPad",
                new BigDecimal("900000"), 1, 2));
        sparePartRepository.saveAll(parts);
        log.info("Đã tạo {} linh kiện mẫu cho kho.", parts.size());
    }

    private SparePart part(String code, String name, String desc,
                           BigDecimal price, int stock, int minStock) {
        SparePart p = new SparePart();
        p.setCode(code);
        p.setName(name);
        p.setDescription(desc);
        p.setUnitPrice(price);
        p.setQuantityInStock(stock);
        p.setMinStock(minStock);
        return p;
    }

    private Customer customer(String name, String phone, String email, String address) {
        Customer c = new Customer();
        c.setFullName(name);
        c.setPhone(phone);
        c.setEmail(email);
        c.setAddress(address);
        return c;
    }

    private Device device(Customer customer, String serial, String brand, String model,
                          DeviceType type, LocalDate purchase, LocalDate warrantyEnd) {
        Device d = new Device();
        d.setSerialNumber(serial);
        d.setBrand(brand);
        d.setModel(model);
        d.setDeviceType(type);
        d.setCustomer(customer);
        d.setPurchaseDate(purchase);
        d.setWarrantyEndDate(warrantyEnd);
        return d;
    }

    private RepairOrder order(String code, Device device, UserAccount tech,
                              RepairStatus status, String issue, String diagnosis, String condition,
                              String accessories, String note, LocalDate received, LocalDate expected,
                              LocalDate completed, BigDecimal laborCost) {
        RepairOrder o = new RepairOrder();
        o.setCode(code);
        o.setDevice(device);
        o.setTechnician(tech);
        o.setStatus(status);
        o.setIssueDescription(issue);
        o.setDiagnosis(diagnosis);
        o.setProductCondition(condition);
        o.setAccessories(accessories);
        o.setNote(note);
        o.setReceivedAt(received.atTime(LocalDateTime.now().toLocalTime()));
        o.setExpectedFinishDate(expected);
        o.setCompletedAt(completed != null ? completed.atTime(LocalDateTime.now().toLocalTime()) : null);
        if (status == RepairStatus.PAID && completed != null) {
            o.setPaidAt(completed.atTime(LocalDateTime.now().toLocalTime()));
        }
        o.setLaborCost(laborCost);
        return o;
    }
}