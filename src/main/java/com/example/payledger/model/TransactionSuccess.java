package com.example.payledger.model;

import com.example.payledger.model.Transaction;

public record TransactionSuccess(Transaction transaction) implements TransactionResult {}