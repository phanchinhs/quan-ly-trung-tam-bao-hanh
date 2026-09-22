package com.example.btl.controller;

import com.example.btl.entity.Customer;
import com.example.btl.service.CustomerService;
import com.example.btl.service.DeviceService;
import com.example.btl.util.PageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerController {

    private final CustomerService customerService;
    private final DeviceService deviceService;
    private final PageUtil pageUtil;

    public CustomerController(CustomerService customerService, DeviceService deviceService, PageUtil pageUtil) {
        this.customerService = customerService;
        this.deviceService = deviceService;
        this.pageUtil = pageUtil;
    }

    @GetMapping("/customers")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false, defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "20") int size,
                       Model model) {
        Page<Customer> result = customerService.search(keyword, PageRequest.of(page, size));
        int totalPages = result.getTotalPages();
        int currentPage = pageUtil.clampPage(page, totalPages);
        model.addAttribute("customers", result.getContent());
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", result.getTotalElements());
        model.addAttribute("pages", pageUtil.buildPagination(currentPage, totalPages));
        return "customers/list";
    }

    @GetMapping("/customers/new")
    public String createForm(Model model) {
        model.addAttribute("customer", new Customer());
        model.addAttribute("isEdit", false);
        return "customers/form";
    }

    @GetMapping("/customers/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng: " + id));
        model.addAttribute("customer", customer);
        model.addAttribute("devices", deviceService.findByCustomer(id));
        return "customers/detail";
    }

    @GetMapping("/customers/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng: " + id));
        model.addAttribute("customer", customer);
        model.addAttribute("isEdit", true);
        return "customers/form";
    }

    @PostMapping("/customers/save")
    public String save(@ModelAttribute Customer customer,
                       @RequestParam(required = false) Long id,
                       RedirectAttributes ra) {
        if (id != null) {
            customer.setId(id);
        }
        if (customer.getFullName() == null || customer.getFullName().isBlank()) {
            ra.addFlashAttribute("error", "Họ tên khách hàng không được để trống.");
            return "redirect:/customers" + (id != null ? "/edit/" + id : "/new");
        }
        if (customer.getPhone() == null || customer.getPhone().isBlank()) {
            ra.addFlashAttribute("error", "Số điện thoại không được để trống.");
            return "redirect:/customers" + (id != null ? "/edit/" + id : "/new");
        }

        boolean phoneTaken = (id == null)
                ? customerService.existsByPhone(customer.getPhone())
                : customerService.existsByPhoneExcept(customer.getPhone(), id);
        if (phoneTaken) {
            ra.addFlashAttribute("error", "Số điện thoại '" + customer.getPhone() + "' đã được sử dụng.");
            return "redirect:/customers" + (id != null ? "/edit/" + id : "/new");
        }

        customerService.save(customer);
        ra.addFlashAttribute("success", id != null
                ? "Đã cập nhật thông tin khách hàng."
                : "Đã thêm khách hàng mới.");
        return "redirect:/customers";
    }

    @GetMapping("/customers/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        customerService.findById(id).ifPresent(c -> {
            if (!c.getDevices().isEmpty()) {
                ra.addFlashAttribute("error", "Khách hàng còn thiết bị, không thể xóa.");
                return;
            }
            customerService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa khách hàng.");
        });
        return "redirect:/customers";
    }
}