package com.example.btl.service;

import com.example.btl.entity.AuditLog;
import com.example.btl.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog log(String action, String entityName, Long entityId,
                        String username, String details) {
        AuditLog log = new AuditLog(action, entityName, entityId, username, details);
        return auditLogRepository.save(log);
    }

    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    public Page<AuditLog> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }
        String k = keyword.trim();
        return auditLogRepository
                .findByEntityNameContainingIgnoreCaseOrActionContainingIgnoreCase(k, k, pageable);
    }

    public Page<AuditLog> findByUsername(String username, Pageable pageable) {
        if (username == null || username.isBlank()) {
            return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }
        return auditLogRepository
                .findByUsernameContainingIgnoreCaseOrderByTimestampDesc(username.trim(), pageable);
    }

    public long count() {
        return auditLogRepository.count();
    }
}
