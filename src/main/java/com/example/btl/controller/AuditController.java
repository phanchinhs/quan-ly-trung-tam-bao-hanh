package com.example.btl.controller;

import com.example.btl.service.AuditLogService;
import com.example.btl.util.PageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditController {

    private final AuditLogService auditLogService;
    private final PageUtil pageUtil;

    public AuditController(AuditLogService auditLogService, PageUtil pageUtil) {
        this.auditLogService = auditLogService;
        this.pageUtil = pageUtil;
    }

    @GetMapping("/audit")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false, defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "20") int size,
                       Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<?> logs;
        if (keyword != null && !keyword.isBlank()) {
            logs = auditLogService.search(keyword, pageable);
        } else {
            logs = auditLogService.findAll(pageable);
        }
        int totalPages = logs.getTotalPages();
        int currentPage = pageUtil.clampPage(page, totalPages);
        model.addAttribute("logs", logs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", logs.getTotalElements());
        model.addAttribute("pages", pageUtil.buildPagination(currentPage, totalPages));
        return "audit/list";
    }
}
