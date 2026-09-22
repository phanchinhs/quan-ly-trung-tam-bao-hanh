package com.example.btl.advice;

import com.example.btl.entity.UserAccount;
import com.example.btl.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;

@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository userRepository;

    public GlobalModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpServletRequest request) {
        model.addAttribute("today", LocalDate.now());

        String uri = request.getRequestURI();
        if (uri.startsWith("/dashboard")) {
            model.addAttribute("page", "dashboard");
        } else if (uri.startsWith("/customers")) {
            model.addAttribute("page", "customers");
        } else if (uri.startsWith("/devices")) {
            model.addAttribute("page", "devices");
        } else if (uri.startsWith("/orders")) {
            model.addAttribute("page", "orders");
        } else if (uri.startsWith("/parts")) {
            model.addAttribute("page", "parts");
        } else if (uri.startsWith("/reports")) {
            model.addAttribute("page", "reports");
        } else if (uri.startsWith("/audit")) {
            model.addAttribute("page", "audit");
        } else if (uri.startsWith("/users")) {
            model.addAttribute("page", "users");
        } else if (uri.startsWith("/profile")) {
            model.addAttribute("page", "profile");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            UserAccount currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
            model.addAttribute("currentUser", currentUser);
        }
    }
}