package com.example.btl.controller;

import com.example.btl.entity.Role;
import com.example.btl.entity.UserAccount;
import com.example.btl.service.UserService;
import com.example.btl.util.PageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private final UserService userService;
    private final PageUtil pageUtil;

    public UserController(UserService userService, PageUtil pageUtil) {
        this.userService = userService;
        this.pageUtil = pageUtil;
    }

    @GetMapping("/users")
    public String list(@RequestParam(required = false, defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "20") int size,
                       Model model) {
        Page<UserAccount> result = userService.findAll(PageRequest.of(page, size, Sort.by("id")));
        int totalPages = result.getTotalPages();
        int currentPage = pageUtil.clampPage(page, totalPages);
        model.addAttribute("users", result.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", result.getTotalElements());
        model.addAttribute("pages", pageUtil.buildPagination(currentPage, totalPages));
        return "users/list";
    }

    @GetMapping("/users/new")
    public String createForm(Model model) {
        model.addAttribute("account", new UserAccount());
        model.addAttribute("roles", Role.values());
        model.addAttribute("isEdit", false);
        return "users/form";
    }

    @GetMapping("/users/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        UserAccount account = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản: " + id));
        model.addAttribute("account", account);
        model.addAttribute("roles", Role.values());
        model.addAttribute("isEdit", true);
        return "users/form";
    }

    @PostMapping("/users/create")
    public String create(@ModelAttribute UserAccount account,
                         @RequestParam String password,
                         RedirectAttributes ra) {
        if (account.getUsername() == null || account.getUsername().isBlank()) {
            ra.addFlashAttribute("error", "Tên đăng nhập không được để trống.");
            return "redirect:/users/new";
        }
        if (password == null || password.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
            return "redirect:/users/new";
        }
        if (userService.existsByUsername(account.getUsername())) {
            ra.addFlashAttribute("error", "Tên đăng nhập '" + account.getUsername() + "' đã tồn tại.");
            return "redirect:/users/new";
        }
        if (account.getRole() == null) {
            account.setRole(Role.STAFF);
        }
        userService.create(account, password);
        ra.addFlashAttribute("success", "Đã tạo tài khoản " + account.getUsername() + ".");
        return "redirect:/users";
    }

    @PostMapping("/users/update")
    public String update(@ModelAttribute UserAccount account,
                         @RequestParam Long id,
                         RedirectAttributes ra) {
        account.setId(id);
        if (account.getRole() == null) {
            account.setRole(Role.STAFF);
        }
        userService.update(account);
        ra.addFlashAttribute("success", "Đã cập nhật tài khoản.");
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String password,
                                RedirectAttributes ra) {
        if (password == null || password.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
            return "redirect:/users";
        }
        userService.resetPassword(id, password);
        ra.addFlashAttribute("success", "Đã đặt lại mật khẩu.");
        return "redirect:/users";
    }

    @GetMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (auth.getName().equals(userService.findById(id).map(UserAccount::getUsername).orElse(""))) {
            ra.addFlashAttribute("error", "Không thể khóa tài khoản của chính mình.");
            return "redirect:/users";
        }
        userService.toggleActive(id);
        ra.addFlashAttribute("success", "Đã thay đổi trạng thái tài khoản.");
        return "redirect:/users";
    }

    @GetMapping("/users/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (auth.getName().equals(userService.findById(id).map(UserAccount::getUsername).orElse(""))) {
            ra.addFlashAttribute("error", "Không thể xóa tài khoản của chính mình.");
            return "redirect:/users";
        }
        userService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa tài khoản.");
        return "redirect:/users";
    }
}