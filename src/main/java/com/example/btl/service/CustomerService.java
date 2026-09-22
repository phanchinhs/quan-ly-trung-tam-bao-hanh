package com.example.btl.service;

import com.example.btl.entity.Customer;
import com.example.btl.config.Auditable;
import com.example.btl.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAll();
        }
        String k = keyword.trim();
        String hash = CryptoService.get() != null ? CryptoService.get().sha256Hex(k) : null;
        return customerRepository.findByFullNameContainingIgnoreCaseOrPhoneHash(k, hash);
    }

    public Page<Customer> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAll(pageable);
        }
        String k = keyword.trim();
        String hash = CryptoService.get() != null ? CryptoService.get().sha256Hex(k) : null;
        return customerRepository.findByFullNameContainingIgnoreCaseOrPhoneHash(k, hash, pageable);
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public boolean existsByPhone(String phone) {
        String hash = CryptoService.get() != null ? CryptoService.get().sha256Hex(phone) : null;
        return customerRepository.existsByPhoneHash(hash);
    }

    public boolean existsByPhoneExcept(String phone, Long id) {
        String hash = CryptoService.get() != null ? CryptoService.get().sha256Hex(phone) : null;
        return customerRepository.existsByPhoneHashAndIdNot(hash, id);
    }

    @Transactional
    @Auditable(action = "SAVE", entity = "Khách hàng", description = "Thêm/sửa khách hàng {name}")
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    @Auditable(action = "DELETE", entity = "Khách hàng", description = "Xóa khách hàng #{id}")
    public void delete(Long id) {
        customerRepository.deleteById(id);
    }

    public long count() {
        return customerRepository.count();
    }
}