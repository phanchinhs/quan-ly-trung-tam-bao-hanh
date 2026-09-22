package com.example.btl.service;

import com.example.btl.entity.UserAccount;
import com.example.btl.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        if (!account.isActive()) {
            throw new UsernameNotFoundException("Tài khoản đã bị khóa: " + username);
        }

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + account.getRole().name());
        return new User(account.getUsername(), account.getPassword(), Collections.singletonList(authority));
    }

    public List<UserAccount> findActiveUsers() {
        return userRepository.findByActiveTrue();
    }
}