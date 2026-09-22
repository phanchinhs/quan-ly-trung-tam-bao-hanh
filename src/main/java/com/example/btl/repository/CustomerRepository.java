package com.example.btl.repository;

import com.example.btl.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhoneHash(String phoneHash);

    boolean existsByPhoneHash(String phoneHash);

    boolean existsByPhoneHashAndIdNot(String phoneHash, Long id);

    List<Customer> findByFullNameContainingIgnoreCaseOrPhoneHash(String name, String phoneHash);

    Page<Customer> findByFullNameContainingIgnoreCaseOrPhoneHash(String name, String phoneHash, Pageable pageable);
}