package com.example.btl.repository;

import com.example.btl.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityNameContainingIgnoreCaseOrActionContainingIgnoreCase(
            String entityName, String action, Pageable pageable);

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByUsernameContainingIgnoreCaseOrderByTimestampDesc(String username, Pageable pageable);
}
