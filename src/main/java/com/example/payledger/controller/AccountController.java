package com.example.payledger.controller;

import com.example.payledger.dto.request.CreateAccountRequest;
import com.example.payledger.dto.response.AccountResponse;
import com.example.payledger.model.Account;
import com.example.payledger.model.User;
import com.example.payledger.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        Long userId = currentUserId();
        Account account = accountService.createAccount(userId, request.currency());
        return ResponseEntity.ok(new AccountResponse(account));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long accountId) {
        Long userId = currentUserId();
        return accountService.findByIdForUser(accountId, userId)
                .map(account -> ResponseEntity.ok(new AccountResponse(account)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccountsByUser() {
        Long userId = currentUserId();
        List<Account> accounts = accountService.findByUserId(userId);
        List<AccountResponse> accountResponses = accounts.stream()
                .map(AccountResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(accountResponses);
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<AccountResponse> deposit(@PathVariable Long accountId,
                                                   @RequestParam BigDecimal amount) {
        Long userId = currentUserId();
        Account account = accountService.deposit(accountId, userId, amount);
        return ResponseEntity.ok(new AccountResponse(account));
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(@PathVariable Long accountId,
                                                    @RequestParam BigDecimal amount) {
        Long userId = currentUserId();
        Account account = accountService.withdraw(accountId, userId, amount);
        return ResponseEntity.ok(new AccountResponse(account));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }
}
