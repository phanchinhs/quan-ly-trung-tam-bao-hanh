package com.example.btl.controller;

import com.example.btl.entity.DeviceType;
import com.example.btl.entity.RepairItem;
import com.example.btl.entity.RepairOrder;
import com.example.btl.entity.RepairStatus;
import com.example.btl.service.DeviceService;
import com.example.btl.service.RepairService;
import com.example.btl.service.ReportExcelService;
import com.example.btl.service.ReportPdfService;
import com.example.btl.service.SparePartService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ReportController {

    private final RepairService repairService;
    private final DeviceService deviceService;
    private final SparePartService sparePartService;
    private final ReportPdfService reportPdfService;
    private final ReportExcelService reportExcelService;

    public ReportController(RepairService repairService,
                            DeviceService deviceService,
                            SparePartService sparePartService,
                            ReportPdfService reportPdfService,
                            ReportExcelService reportExcelService) {
        this.repairService = repairService;
        this.deviceService = deviceService;
        this.sparePartService = sparePartService;
        this.reportPdfService = reportPdfService;
        this.reportExcelService = reportExcelService;
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        LocalDate today = LocalDate.now();

        // Revenue by month, last 12 months
        List<MonthStat> monthStats = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime from = ym.atDay(1).atStartOfDay();
            LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);
            BigDecimal revenue = repairService.findPaidBetween(from, to).stream()
                    .map(o -> o.getTotalCost())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthStats.add(new MonthStat(ym, revenue));
        }
        model.addAttribute("monthStats", monthStats);

        List<String> labels = new ArrayList<>();
        List<Double> revenueList = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        for (MonthStat ms : monthStats) {
            labels.add(ms.month().format(fmt));
            revenueList.add(ms.revenue().doubleValue());
        }
        model.addAttribute("labels", labels);
        model.addAttribute("revenue", revenueList);

        BigDecimal totalRevenue = repairService.findPaidBetween(
                LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now()).stream()
                .map(o -> o.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalRevenue", totalRevenue);

        model.addAttribute("countReceived", repairService.countByStatus(RepairStatus.RECEIVED));
        model.addAttribute("countInProgress", repairService.countByStatus(RepairStatus.IN_PROGRESS));
        model.addAttribute("countWaitingParts", repairService.countByStatus(RepairStatus.WAITING_PARTS));
        model.addAttribute("countCompleted", repairService.countByStatus(RepairStatus.COMPLETED));
        model.addAttribute("countPaid", repairService.countByStatus(RepairStatus.PAID));
        model.addAttribute("countCancelled", repairService.countByStatus(RepairStatus.CANCELLED));

        model.addAttribute("warrantiesExpiring", deviceService.expiringWarranties(today, today.plusDays(30)));
        model.addAttribute("warrantiesExpired", 0L);

        model.addAttribute("lowStockParts", sparePartService.lowStockParts());
        model.addAttribute("lowStockCount", sparePartService.countLowStock());
        model.addAttribute("totalParts", sparePartService.count());

        addAdvancedStats(model);
        return "reports/index";
    }

    private void addAdvancedStats(Model model) {
        List<RepairOrder> orders = repairService.findAll();
        List<RepairOrder> paid = orders.stream()
                .filter(o -> o.getStatus() == RepairStatus.PAID)
                .toList();

        // Doanh thu theo loại thiết bị
        Map<DeviceType, BigDecimal> byType = new LinkedHashMap<>();
        for (RepairOrder o : paid) {
            DeviceType dt = o.getDevice() != null ? o.getDevice().getDeviceType() : DeviceType.OTHER;
            byType.merge(dt, o.getTotalCost(), BigDecimal::add);
        }
        model.addAttribute("typeLabels", byType.keySet().stream().map(DeviceType::getLabel).toList());
        model.addAttribute("typeRevenue", byType.values().stream().map(BigDecimal::doubleValue).toList());

        // Linh kiện / dịch vụ được sử dụng nhiều nhất
        Map<String, Integer> partCounts = new HashMap<>();
        for (RepairOrder o : orders) {
            for (RepairItem it : o.getItems()) {
                partCounts.merge(it.getName(), it.getQuantity(), Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> topParts = partCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .collect(Collectors.toList());
        model.addAttribute("partLabels", topParts.stream().map(Map.Entry::getKey).toList());
        model.addAttribute("partCounts", topParts.stream().map(Map.Entry::getValue).map(Integer::doubleValue).toList());
        model.addAttribute("topPartsList", topParts.stream()
                .map(e -> new TopPart(e.getKey(), e.getValue())).toList());

        // Thời gian sửa trung bình (giờ) các đơn đã hoàn thành / thanh toán
        List<Long> repairHours = orders.stream()
                .filter(o -> o.getCompletedAt() != null && o.getReceivedAt() != null)
                .filter(o -> o.getStatus() == RepairStatus.PAID || o.getStatus() == RepairStatus.COMPLETED)
                .map(o -> java.time.Duration.between(o.getReceivedAt(), o.getCompletedAt()).toHours())
                .toList();
        double avgHours = repairHours.stream().mapToLong(Long::longValue).average().orElse(0);
        model.addAttribute("avgRepairHours", Math.round(avgHours * 10) / 10.0);

        // Tỉ lệ hoàn thành (đã thanh toán / tổng không hủy)
        long finished = paid.size();
        long notCancelled = repairService.countAll() - repairService.countByStatus(RepairStatus.CANCELLED);
        model.addAttribute("completionRate", notCancelled == 0 ? 0 : Math.round(finished * 100.0 / notCancelled));
    }

    @GetMapping("/reports/export-pdf")
    public ResponseEntity<byte[]> exportPdf() {
        byte[] data = reportPdfService.generateProgressReport();
        String fileName = "bao-cao-tien-do-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/reports/export-excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] data = reportExcelService.generateProgressReport();
        String fileName = "bao-cao-tien-do-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    public record MonthStat(YearMonth month, BigDecimal revenue) {
    }

    public record TopPart(String name, int count) {
    }
}