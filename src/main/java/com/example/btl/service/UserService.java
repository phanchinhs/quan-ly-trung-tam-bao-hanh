package com.example.btl.service;

import com.example.btl.entity.UserAccount;
import com.example.btl.config.Auditable;
import com.example.btl.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAccount> findAll() {
        return userRepository.findAll();
    }

    public Page<UserAccount> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Optional<UserAccount> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    @Auditable(action = "CREATE", entity = "Tài khoản", description = "Tạo tài khoản {name}")
    public UserAccount create(UserAccount account, String rawPassword) {
        account.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(account);
    }

    @Transactional
    @Auditable(action = "UPDATE", entity = "Tài khoản", description = "Cập nhật tài khoản #{id}")
    public UserAccount update(UserAccount account) {
        return userRepository.save(account);
    }

    @Transactional
    @Auditable(action = "UPDATE", entity = "Tài khoản", description = "Đặt lại mật khẩu tài khoản #{id}")
    public void resetPassword(Long id, String rawPassword) {
        UserAccount account = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        account.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(account);
    }

    @Transactional
    public void changePassword(Long id, String rawPassword) {
        resetPassword(id, rawPassword);
    }

    @Transactional
    @Auditable(action = "UPDATE", entity = "Tài khoản", description = "Khóa/Mở khóa tài khoản #{id}")
    public void toggleActive(Long id) {
        UserAccount account = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        account.setActive(!account.isActive());
        userRepository.save(account);
    }

    @Transactional
    @Auditable(action = "DELETE", entity = "Tài khoản", description = "Xóa tài khoản #{id}")
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}