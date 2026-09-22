package com.example.btl.controller;

import com.example.btl.entity.Customer;
import com.example.btl.entity.Device;
import com.example.btl.entity.DeviceType;
import com.example.btl.service.CustomerService;
import com.example.btl.service.DeviceService;
import com.example.btl.util.PageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DeviceController {

    private final DeviceService deviceService;
    private final CustomerService customerService;
    private final PageUtil pageUtil;

    public DeviceController(DeviceService deviceService, CustomerService customerService, PageUtil pageUtil) {
        this.deviceService = deviceService;
        this.customerService = customerService;
        this.pageUtil = pageUtil;
    }

    @GetMapping("/devices")
    public String list(@RequestParam(required = false, defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "20") int size,
                       Model model) {
        Page<Device> result = deviceService.findAll(PageRequest.of(page, size, Sort.by("id")));
        int totalPages = result.getTotalPages();
        int currentPage = pageUtil.clampPage(page, totalPages);
        model.addAttribute("devices", result.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", result.getTotalElements());
        model.addAttribute("pages", pageUtil.buildPagination(currentPage, totalPages));
        return "devices/list";
    }

    @GetMapping("/devices/new")
    public String createForm(@RequestParam(required = false) Long customerId, Model model) {
        model.addAttribute("device", new Device());
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("deviceTypes", DeviceType.values());
        model.addAttribute("isEdit", false);
        return "devices/form";
    }

    @GetMapping("/devices/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Device device = deviceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiết bị: " + id));
        model.addAttribute("device", device);
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("deviceTypes", DeviceType.values());
        model.addAttribute("isEdit", true);
        return "devices/form";
    }

    @GetMapping("/devices/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        deviceService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa thiết bị.");
        return "redirect:/devices";
    }

    @PostMapping("/devices/save")
    public String save(@ModelAttribute Device device,
                       @RequestParam(required = false) Long id,
                       @RequestParam Long customerId,
                       RedirectAttributes ra) {
        if (id != null) {
            device.setId(id);
        }
        if (device.getSerialNumber() == null || device.getSerialNumber().isBlank()) {
            ra.addFlashAttribute("error", "Số serial không được để trống.");
            return "redirect:/devices" + (id != null ? "/edit/" + id : "/new");
        }

        boolean serialTaken = (id == null)
                ? deviceService.existsBySerialNumber(device.getSerialNumber())
                : deviceService.existsBySerialNumberExcept(device.getSerialNumber(), id);
        if (serialTaken) {
            ra.addFlashAttribute("error", "Số serial '" + device.getSerialNumber() + "' đã tồn tại.");
            return "redirect:/devices" + (id != null ? "/edit/" + id : "/new");
        }

        Customer customer = customerService.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Chọn khách hàng không hợp lệ"));
        if (device.getDeviceType() == null) {
            device.setDeviceType(DeviceType.OTHER);
        }
        device.setCustomer(customer);
        deviceService.save(device);
        ra.addFlashAttribute("success", id != null ? "Đã cập nhật thiết bị." : "Đã thêm thiết bị mới.");
        return "redirect:/devices";
    }
}