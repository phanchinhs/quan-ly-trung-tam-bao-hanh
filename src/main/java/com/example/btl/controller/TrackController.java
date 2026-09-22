package com.example.btl.controller;

import com.example.btl.entity.RepairOrder;
import com.example.btl.repository.RepairOrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TrackController {

    private final RepairOrderRepository repairOrderRepository;

    public TrackController(RepairOrderRepository repairOrderRepository) {
        this.repairOrderRepository = repairOrderRepository;
    }

    @GetMapping("/track")
    public String track(@RequestParam(required = false) String code, Model model) {
        if (code != null && !code.isBlank()) {
            model.addAttribute("query", code.trim());
            repairOrderRepository.findByCode(code.trim())
                    .ifPresent(order -> model.addAttribute("order", order));
        }
        return "track";
    }
}