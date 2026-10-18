package com.example.payledger.dto.response;

import com.example.payledger.model.Transaction;

public record TransactionResponse(Long id,
                                  String transactionId,
                                  Long fromAccountId,
                                  Long toAccountId,
                                  String amount,
                                  String currency,
                                  String status,
                                  String createdAt,
                                  String errorMessage) {

    public TransactionResponse(Transaction transaction) {
        this(
                transaction.getId(),
                transaction.getTransactionId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount().toString(),
                transaction.getCurrency(),
                transaction.getStatus().name(),
                transaction.getCreatedAt().toString(),
                null
        );
    }

    public TransactionResponse(Transaction transaction, String errorMessage) {
        this(
                transaction.getId(),
                transaction.getTransactionId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount().toString(),
                transaction.getCurrency(),
                transaction.getStatus().name(),
                transaction.getCreatedAt().toString(),
                errorMessage
        );
    }
}