package com.example.payledger.model;

import com.example.payledger.model.Transaction;

public record TransactionFailed(String reason, Transaction transaction) implements TransactionResult {}