package com.example.btl.repository;

import com.example.btl.entity.SparePart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SparePartRepository extends JpaRepository<SparePart, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<SparePart> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);

    Page<SparePart> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name, Pageable pageable);

    @Query("SELECT p FROM SparePart p WHERE p.quantityInStock <= p.minStock")
    List<SparePart> findLowStock();

    @Query("SELECT COUNT(p) FROM SparePart p WHERE p.quantityInStock <= p.minStock")
    long countLowStock();
}