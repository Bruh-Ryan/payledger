package com.example.payledger.controller;

import com.example.payledger.model.AuditLog;
import com.example.payledger.model.User;
import com.example.payledger.service.AccountService;
import com.example.payledger.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final AccountService accountService;

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsForAccount(@PathVariable Long accountId) {
        Long userId = currentUserId();
        if (accountService.findByIdForUser(accountId, userId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(auditService.findByEntity("Account", accountId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsForUser(@PathVariable Long userId) {
        Long currentUserId = currentUserId();
        // Users may only read their own audit trail
        if (!currentUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(auditService.findByUser(userId));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }
}
