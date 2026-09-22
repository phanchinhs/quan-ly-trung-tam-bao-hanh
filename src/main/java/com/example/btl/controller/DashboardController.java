package com.example.btl.controller;

import com.example.btl.entity.RepairStatus;
import com.example.btl.service.CustomerService;
import com.example.btl.service.DeviceService;
import com.example.btl.service.RepairService;
import com.example.btl.service.SparePartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DashboardController {

    private final CustomerService customerService;
    private final DeviceService deviceService;
    private final RepairService repairService;
    private final SparePartService sparePartService;

    public DashboardController(CustomerService customerService,
                               DeviceService deviceService,
                               RepairService repairService,
                               SparePartService sparePartService) {
        this.customerService = customerService;
        this.deviceService = deviceService;
        this.repairService = repairService;
        this.sparePartService = sparePartService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.now();
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long activeOrders = repairService.countByStatus(RepairStatus.RECEIVED)
                + repairService.countByStatus(RepairStatus.IN_PROGRESS)
                + repairService.countByStatus(RepairStatus.WAITING_PARTS);

        BigDecimal revenueThisMonth = repairService.findPaidBetween(monthStart, now).stream()
                .map(o -> o.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueTotal = repairService.findPaidBetween(
                LocalDateTime.of(2000, 1, 1, 0, 0), now).stream()
                .map(o -> o.getTotalCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalCustomers", customerService.count());
        model.addAttribute("totalDevices", deviceService.count());
        model.addAttribute("totalOrders", repairService.countAll());
        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("revenueThisMonth", revenueThisMonth);
        model.addAttribute("revenueTotal", revenueTotal);
        model.addAttribute("recentOrders", repairService.recent10());
        model.addAttribute("warrantiesExpiring", deviceService.expiringWarranties(today, today.plusDays(30)));
        model.addAttribute("lowStockParts", sparePartService.lowStockParts());
        model.addAttribute("lowStockCount", sparePartService.countLowStock());
        model.addAttribute("today", today);

        model.addAttribute("countReceived", repairService.countByStatus(RepairStatus.RECEIVED));
        model.addAttribute("countInProgress", repairService.countByStatus(RepairStatus.IN_PROGRESS));
        model.addAttribute("countWaitingParts", repairService.countByStatus(RepairStatus.WAITING_PARTS));
        model.addAttribute("countCompleted", repairService.countByStatus(RepairStatus.COMPLETED));
        model.addAttribute("countPaid", repairService.countByStatus(RepairStatus.PAID));
        model.addAttribute("countCancelled", repairService.countByStatus(RepairStatus.CANCELLED));

        model.addAttribute("monthLabels", revenueByLastMonths(6).labels());
        model.addAttribute("monthRevenue", revenueByLastMonths(6).revenue());

        return "dashboard";
    }

    private RevenueSeries revenueByLastMonths(int months) {
        LocalDateTime now = LocalDateTime.now();
        List<String> labels = new ArrayList<>();
        List<Double> revenue = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);
            if (ym.equals(YearMonth.from(now))) {
                end = now;
            }
            labels.add(ym.format(fmt));
            BigDecimal sum = repairService.findPaidBetween(start, end).stream()
                    .map(o -> o.getTotalCost())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenue.add(sum.doubleValue());
        }
        return new RevenueSeries(labels, revenue);
    }

    private record RevenueSeries(List<String> labels, List<Double> revenue) {
    }
}