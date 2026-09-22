package com.example.btl.controller;

import com.example.btl.entity.UserAccount;
import com.example.btl.repository.UserRepository;
import com.example.btl.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository,
                             UserService userService,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        UserAccount account = userRepository.findByUsername(auth.getName()).orElseThrow();
        model.addAttribute("account", account);
        return "profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(Authentication auth,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        UserAccount account = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            ra.addFlashAttribute("error", "Mật khẩu hiện tại không đúng.");
            return "redirect:/profile";
        }
        if (newPassword == null || newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return "redirect:/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Xác nhận mật khẩu không khớp.");
            return "redirect:/profile";
        }
        userService.changePassword(account.getId(), newPassword);
        ra.addFlashAttribute("success", "Đã đổi mật khẩu thành công.");
        return "redirect:/profile";
    }
}