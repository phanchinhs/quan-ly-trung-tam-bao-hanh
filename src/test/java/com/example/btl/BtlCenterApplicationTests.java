package com.example.btl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bộ test luồng chính của ứng dụng:
 * đăng nhập, phân quyền, truy cập trang, thao tác dữ liệu.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BtlCenterApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.example.btl.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private void createBlockedUser() {
        if (userRepository.findByUsername("blocked").isEmpty()) {
            com.example.btl.entity.UserAccount blocked = new com.example.btl.entity.UserAccount();
            blocked.setUsername("blocked");
            blocked.setPassword(passwordEncoder.encode("blocked123"));
            blocked.setFullName("Tài khoản bị khóa");
            blocked.setRole(com.example.btl.entity.Role.STAFF);
            blocked.setActive(false);
            userRepository.save(blocked);
        }
    }

    // ---------- Bảo mật / phân quyền ----------

    @Test
    void unauthenticatedRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithValidCredentialsRedirectsToHome() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "sai-mat-khau"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void lockedAccountCannotLogin() throws Exception {
        createBlockedUser();
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "blocked")
                        .param("password", "blocked123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void loginWithUnknownUserFails() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "khong-ton-tai")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void dashboardAccessibleForLoggedInUser() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void staffCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/users").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessUserManagement() throws Exception {
        mockMvc.perform(get("/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void staffCanAccessReports() throws Exception {
        mockMvc.perform(get("/reports").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
    }

    // ---------- Truy cập các trang chính ----------

    @Test
    void customersPageAccessibleForStaff() throws Exception {
        mockMvc.perform(get("/customers").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void devicesPageAccessibleForStaff() throws Exception {
        mockMvc.perform(get("/devices").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void ordersPageAccessibleForStaff() throws Exception {
        mockMvc.perform(get("/orders").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
    }

    // ---------- Luồng thao tác dữ liệu ----------

    @Test
    void createCustomerRedirectsToList() throws Exception {
        mockMvc.perform(post("/customers/save")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("fullName", "Khách hàng Test")
                        .param("phone", "0999888777")
                        .param("email", "test@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers"));
    }

    @Test
    void createCustomerWithDuplicatePhoneIsRejected() throws Exception {
        // SĐT trùng với dữ liệu mẫu của seeder
        mockMvc.perform(post("/customers/save")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("fullName", "Trùng SĐT")
                        .param("phone", "0901123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/new"));
    }

    @Test
    void createCustomerMissingNameIsRejected() throws Exception {
        mockMvc.perform(post("/customers/save")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("fullName", "")
                        .param("phone", "0900000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/new"));
    }

    @Test
    void createDeviceRedirectsToList() throws Exception {
        mockMvc.perform(post("/devices/save")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("serialNumber", "TEST-SN-0001")
                        .param("brand", "Nokia")
                        .param("model", "3310")
                        .param("customerId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/devices"));
    }

    @Test
    void createOrderRedirectsToDetail() throws Exception {
        // Seeder tạo ít nhất 1 khách hàng + 1 thiết bị (device id=1)
        mockMvc.perform(post("/orders/create")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("deviceId", "1")
                        .param("issueDescription", "Máy không lên nguồn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*"));
    }

    @Test
    void createOrderWithoutIssueIsRejected() throws Exception {
        mockMvc.perform(post("/orders/create")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("deviceId", "1")
                        .param("issueDescription", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/new"));
    }

    @Test
    void changeOrderStatusRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/orders/1/status")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));
    }
}