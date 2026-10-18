package com.example.payledger.service;

import com.example.payledger.model.AuditLog;
import com.example.payledger.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void audit(String entityType, Long entityId, String action, Long performedBy, String details) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> findByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public List<AuditLog> findByUser(Long userId) {
        return auditLogRepository.findByPerformedBy(userId);
    }
}
