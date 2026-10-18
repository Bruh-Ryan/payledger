package com.example.payledger.dto.response;

import com.example.payledger.model.Account;

public record AccountResponse(Long id,
                              String accountNumber,
                              Long userId,
                              String balance,
                              String currency,
                              String createdAt,
                              String updatedAt) {

    public AccountResponse(Account account) {
        this(
                account.getId(),
                account.getAccountNumber(),
                account.getUserId(),
                account.getBalance().toString(),
                account.getCurrency(),
                account.getCreatedAt().toString(),
                account.getUpdatedAt().toString()
        );
    }
}