package com.example.btl.controller;

import com.example.btl.entity.SparePart;
import com.example.btl.service.SparePartService;
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
public class PartController {

    private final SparePartService sparePartService;
    private final PageUtil pageUtil;

    public PartController(SparePartService sparePartService, PageUtil pageUtil) {
        this.sparePartService = sparePartService;
        this.pageUtil = pageUtil;
    }

    @GetMapping("/parts")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false, defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "20") int size,
                       Model model) {
        Page<SparePart> result = sparePartService.search(keyword, PageRequest.of(page, size));
        int totalPages = result.getTotalPages();
        int currentPage = pageUtil.clampPage(page, totalPages);
        model.addAttribute("parts", result.getContent());
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("lowStockCount", sparePartService.countLowStock());
        model.addAttribute("totalParts", sparePartService.count());
        model.addAttribute("lowStockParts", sparePartService.lowStockParts());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", result.getTotalElements());
        model.addAttribute("pages", pageUtil.buildPagination(currentPage, totalPages));
        return "parts/list";
    }

    @GetMapping("/parts/new")
    public String createForm(Model model) {
        model.addAttribute("part", new SparePart());
        model.addAttribute("isEdit", false);
        return "parts/form";
    }

    @GetMapping("/parts/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("part", sparePartService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy linh kiện: " + id)));
        model.addAttribute("isEdit", true);
        return "parts/form";
    }

    @GetMapping("/parts/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("part", sparePartService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy linh kiện: " + id)));
        model.addAttribute("isEdit", true);
        return "parts/form";
    }

    @PostMapping("/parts/save")
    public String save(@ModelAttribute SparePart part,
                       @RequestParam(defaultValue = "false") boolean isEdit,
                       RedirectAttributes ra) {
        if (part.getCode() == null || part.getCode().isBlank()) {
            ra.addFlashAttribute("error", "Mã linh kiện không được để trống.");
            return isEdit ? "redirect:/parts/" + part.getId() + "/edit" : "redirect:/parts/new";
        }
        if (part.getName() == null || part.getName().isBlank()) {
            ra.addFlashAttribute("error", "Tên linh kiện không được để trống.");
            return isEdit ? "redirect:/parts/" + part.getId() + "/edit" : "redirect:/parts/new";
        }
        if (isEdit && part.getId() != null && sparePartService.existsByCodeExcept(part.getCode(), part.getId())) {
            ra.addFlashAttribute("error", "Mã linh kiện đã tồn tại.");
            return "redirect:/parts/" + part.getId() + "/edit";
        }
        if (!isEdit && sparePartService.existsByCode(part.getCode())) {
            ra.addFlashAttribute("error", "Mã linh kiện đã tồn tại.");
            return "redirect:/parts/new";
        }
        sparePartService.save(part);
        ra.addFlashAttribute("success", (isEdit ? "Đã cập nhật" : "Đã thêm") + " linh kiện " + part.getCode());
        return "redirect:/parts";
    }

    @GetMapping("/parts/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        sparePartService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa linh kiện.");
        return "redirect:/parts";
    }

    @PostMapping("/parts/{id}/adjust")
    public String adjustStock(@PathVariable Long id,
                              @RequestParam int quantity,
                              RedirectAttributes ra) {
        try {
            sparePartService.adjustStock(id, quantity);
            String action = quantity > 0 ? "Nhập kho" : "Xuất kho";
            ra.addFlashAttribute("success", "Đã " + action.toLowerCase() + ": " + Math.abs(quantity) + " sản phẩm.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/parts";
    }
}