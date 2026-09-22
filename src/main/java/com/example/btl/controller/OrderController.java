package com.example.btl.controller;

import com.example.btl.entity.Device;
import com.example.btl.entity.RepairOrder;
import com.example.btl.entity.RepairStatus;
import com.example.btl.entity.UserAccount;
import com.example.btl.service.DeviceService;
import com.example.btl.service.InvoicePdfService;
import com.example.btl.service.RepairService;
import com.example.btl.service.SparePartService;
import com.example.btl.service.UserService;
import com.example.btl.util.PageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class OrderController {

    private final RepairService repairService;
    private final DeviceService deviceService;
    private final UserService userService;
    private final InvoicePdfService invoicePdfService;
    private final SparePartService sparePartService;
    private final PageUtil pageUtil;

    public OrderController(RepairService repairService,
                           DeviceService deviceService,
                           UserService userService,
                           InvoicePdfService invoicePdfService,
                           SparePartService sparePartService,
                           PageUtil pageUtil) {
        this.repairService = repairService;
        this.deviceService = deviceService;
        this.userService = userService;
        this.invoicePdfService = invoicePdfService;
        this.sparePartService = sparePartService;
        this.pageUtil = pageUtil;
    }

    @GetMapping("/orders")
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false, defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "20") int size,
                       Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
        Page<RepairOrder> result = (status == null || status.isBlank())
                ? repairService.findAll(pageable)
                : repairService.findByStatus(RepairStatus.valueOf(status), pageable);
        int totalPages = result.getTotalPages();
        int currentPage = pageUtil.clampPage(page, totalPages);
        model.addAttribute("orders", result.getContent());
        model.addAttribute("selectedStatus", status == null ? "" : status);
        model.addAttribute("statuses", RepairStatus.values());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", result.getTotalElements());
        model.addAttribute("pages", pageUtil.buildPagination(currentPage, totalPages));
        return "orders/list";
    }

    @GetMapping("/orders/new")
    public String createForm(Model model) {
        model.addAttribute("order", new RepairOrder());
        model.addAttribute("devices", deviceService.findAll());
        model.addAttribute("technicians", userService.findAll().stream()
                .filter(u -> u.isActive() && u.getRole().name().equals("STAFF")).toList());
        model.addAttribute("isEdit", false);
        return "orders/form";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, Model model) {
        RepairOrder order = repairService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu sửa chữa: " + id));
        model.addAttribute("order", order);
        model.addAttribute("parts", sparePartService.findAll());
        return "orders/detail";
    }

    @GetMapping("/orders/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        RepairOrder order = repairService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu sửa chữa: " + id));
        model.addAttribute("order", order);
        model.addAttribute("devices", deviceService.findAll());
        model.addAttribute("technicians", userService.findAll().stream()
                .filter(u -> u.isActive() && u.getRole().name().equals("STAFF")).toList());
        model.addAttribute("isEdit", true);
        return "orders/form";
    }

    @GetMapping("/orders/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable Long id) {
        RepairOrder order = repairService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu sửa chữa: " + id));
        byte[] pdf = invoicePdfService.generateInvoice(order);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=hoa-don-" + order.getCode() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/orders/create")
    public String create(@RequestParam Long deviceId,
                         @RequestParam String issueDescription,
                         @RequestParam(required = false) String productCondition,
                         @RequestParam(required = false) String accessories,
                         @RequestParam(required = false) String note,
                         RedirectAttributes ra) {
        Device device = deviceService.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Chọn thiết bị không hợp lệ"));
        if (issueDescription == null || issueDescription.isBlank()) {
            ra.addFlashAttribute("error", "Mô tả sự cố không được để trống.");
            return "redirect:/orders/new";
        }
        RepairOrder order = new RepairOrder();
        order.setDevice(device);
        order.setIssueDescription(issueDescription);
        order.setProductCondition(productCondition);
        order.setAccessories(accessories);
        order.setNote(note);
        order.setLaborCost(BigDecimal.ZERO);
        repairService.create(order);
        ra.addFlashAttribute("success", "Đã tạo phiếu sửa chữa " + order.getCode() + ".");
        return "redirect:/orders/" + order.getId();
    }

    @PostMapping("/orders/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam(required = false) Long technicianId,
                         @RequestParam(required = false) String issueDescription,
                         @RequestParam(required = false) String diagnosis,
                         @RequestParam(required = false) String productCondition,
                         @RequestParam(required = false) String accessories,
                         @RequestParam(required = false) String note,
                         @RequestParam(required = false) String expectedFinishDate,
                         @RequestParam(required = false) BigDecimal laborCost,
                         RedirectAttributes ra) {
        RepairOrder order = repairService.findById(id).orElseThrow();
        if (technicianId != null) {
            order.setTechnician(userService.findById(technicianId).orElse(null));
        }
        order.setIssueDescription(issueDescription);
        order.setDiagnosis(diagnosis);
        order.setProductCondition(productCondition);
        order.setAccessories(accessories);
        order.setNote(note);
        if (expectedFinishDate != null && !expectedFinishDate.isBlank()) {
            order.setExpectedFinishDate(java.time.LocalDate.parse(expectedFinishDate));
        }
        order.setLaborCost(laborCost == null ? BigDecimal.ZERO : laborCost);
        repairService.updateDetails(order);
        ra.addFlashAttribute("success", "Đã cập nhật thông tin phiếu sửa chữa.");
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam RepairStatus status,
                               RedirectAttributes ra) {
        try {
            repairService.changeStatus(id, status);
            ra.addFlashAttribute("success", "Đã chuyển trạng thái phiếu sang: " + status.getLabel());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/items")
    public String addItem(@PathVariable Long id,
                          @RequestParam(required = false) Long sparePartId,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) Integer quantity,
                          @RequestParam(required = false) BigDecimal unitPrice,
                          RedirectAttributes ra) {
        if (sparePartId == null && (name == null || name.isBlank())) {
            ra.addFlashAttribute("error", "Vui lòng chọn linh kiện từ kho hoặc nhập tên linh kiện.");
            return "redirect:/orders/" + id;
        }
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }
        try {
            repairService.addItem(id, sparePartId, name == null ? null : name.trim(),
                    quantity, unitPrice);
            ra.addFlashAttribute("success", "Đã thêm linh kiện vào phiếu.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @GetMapping("/orders/{id}/items/{itemId}/delete")
    public String removeItem(@PathVariable Long id, @PathVariable Long itemId, RedirectAttributes ra) {
        repairService.removeItem(id, itemId);
        ra.addFlashAttribute("success", "Đã xóa linh kiện.");
        return "redirect:/orders/" + id;
    }

    @GetMapping("/orders/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        RepairOrder order = repairService.findById(id).orElse(null);
        if (order != null && (order.getStatus() == RepairStatus.PAID || order.getStatus() == RepairStatus.CANCELLED)) {
            ra.addFlashAttribute("error", "Không thể xóa phiếu đã thanh toán hoặc đã hủy.");
        } else {
            repairService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa phiếu sửa chữa.");
        }
        return "redirect:/orders";
    }
}