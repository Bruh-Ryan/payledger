package com.example.payledger.model;

public sealed interface TransactionResult permits TransactionSuccess, TransactionFailed {}