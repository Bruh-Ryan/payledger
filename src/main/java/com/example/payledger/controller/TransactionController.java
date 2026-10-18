package com.example.payledger.controller;

import com.example.payledger.dto.request.TransferRequest;
import com.example.payledger.dto.response.TransactionResponse;
import com.example.payledger.model.TransactionFailed;
import com.example.payledger.model.TransactionResult;
import com.example.payledger.model.TransactionSuccess;
import com.example.payledger.model.User;
import com.example.payledger.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                        @RequestBody TransferRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        TransactionResult result = transactionService.transfer(
                request.fromAccountId(),
                request.toAccountId(),
                user.getId(),
                request.amount(),
                request.currency(),
                idempotencyKey
        );

        if (result instanceof TransactionSuccess success) {
            return ResponseEntity.ok(new TransactionResponse(success.transaction()));
        } else if (result instanceof TransactionFailed failed) {
            // We can return a bad request or a custom error response
            return ResponseEntity.badRequest().body(new TransactionResponse(failed.transaction(), failed.reason()));
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }
}
