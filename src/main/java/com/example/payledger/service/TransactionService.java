package com.example.payledger.service;

import com.example.payledger.model.Account;
import com.example.payledger.model.AuditLog;
import com.example.payledger.model.Transaction;
import com.example.payledger.model.Transaction.TransactionStatus;
import com.example.payledger.model.TransactionFailed;
import com.example.payledger.model.TransactionResult;
import com.example.payledger.model.TransactionSuccess;
import com.example.payledger.repository.AccountRepository;
import com.example.payledger.repository.AuditLogRepository;
import com.example.payledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResult transfer(Long fromAccountId, Long toAccountId, Long userId, BigDecimal amount, String currency, String idempotencyKey) {
        // 1. Check for existing transaction with the same idempotency key
        if (Objects.nonNull(idempotencyKey)) {
            Transaction existingTransaction = transactionRepository.findByTransactionId(idempotencyKey)
                    .orElse(null);
            if (existingTransaction != null) {
                if (existingTransaction.getStatus() == TransactionStatus.SUCCESS) {
                    // Audit the duplicate transaction attempt (we already audited the original)
                    auditLogRepository.save(AuditLog.builder()
                            .entityType("Transaction")
                            .entityId(existingTransaction.getId())
                            .action("DUPLICATE_TRANSACTION_ATTEMPT")
                            .performedBy(userId)
                            .timestamp(LocalDateTime.now())
                            .details("Duplicate transaction with idempotency key: " + idempotencyKey)
                            .build());
                    return new TransactionSuccess(existingTransaction);
                } else {
                    // Return a failed result with the existing transaction
                    auditLogRepository.save(AuditLog.builder()
                            .entityType("Transaction")
                            .entityId(existingTransaction.getId())
                            .action("DUPLICATE_TRANSACTION_ATTEMPT_FAILED")
                            .performedBy(userId)
                            .timestamp(LocalDateTime.now())
                            .details("Duplicate transaction attempt with idempotency key: " + idempotencyKey + ", original status: " + existingTransaction.getStatus())
                            .build());
                    return new TransactionFailed("Duplicate transaction attempt", existingTransaction);
                }
            }
        }

        // 2. Create a new transaction record in PENDING state
        Transaction transaction = Transaction.builder()
                .transactionId(idempotencyKey)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // 3. Lock both accounts in a consistent order (by id to avoid deadlock)
            Long firstLockId = Math.min(fromAccountId, toAccountId);
            Long secondLockId = Math.max(fromAccountId, toAccountId);

            Account firstAccount = accountRepository.findAccountForUpdate(firstLockId);
            Account secondAccount = accountRepository.findAccountForUpdate(secondLockId);

            // 4. Check if accounts exist and belong to the same currency (simplified)
            if (firstAccount == null || secondAccount == null) {
                transaction.setStatus(TransactionStatus.FAILED_ACCOUNT_NOT_FOUND);
                transactionRepository.save(transaction);
                auditLogRepository.save(AuditLog.builder()
                        .entityType("Transaction")
                        .entityId(transaction.getId())
                        .action("TRANSFER_FAILED_ACCOUNT_NOT_FOUND")
                        .performedBy(userId)
                        .timestamp(LocalDateTime.now())
                        .details("Transfer failed: account not found. From: " + fromAccountId + ", To: " + toAccountId)
                        .build());
                return new TransactionFailed("Account not found", transaction);
            }

            // 5. Determine which account is the from and which is the to
            Account fromAccount = (fromAccountId.equals(firstAccount.getId())) ? firstAccount : secondAccount;
            Account toAccount = (fromAccountId.equals(firstAccount.getId())) ? secondAccount : firstAccount;

            // 6. Check that the source account belongs to the authenticated user
            if (!fromAccount.getUserId().equals(userId)) {
                transaction.setStatus(TransactionStatus.FAILED_INVALID_ACCOUNT);
                transactionRepository.save(transaction);
                auditLogRepository.save(AuditLog.builder()
                        .entityType("Transaction")
                        .entityId(transaction.getId())
                        .action("TRANSFER_FAILED_UNAUTHORIZED")
                        .performedBy(userId)
                        .timestamp(LocalDateTime.now())
                        .details("Transfer failed: source account " + fromAccountId + " does not belong to user " + userId)
                        .build());
                return new TransactionFailed("Source account does not belong to the authenticated user", transaction);
            }

            // 7. Check sufficient balance
            if (fromAccount.getBalance().compareTo(amount) < 0) {
                transaction.setStatus(TransactionStatus.FAILED_INSUFFICIENT_FUNDS);
                transactionRepository.save(transaction);
                auditLogRepository.save(AuditLog.builder()
                        .entityType("Transaction")
                        .entityId(transaction.getId())
                        .action("TRANSFER_FAILED_INSUFFICIENT_FUNDS")
                        .performedBy(fromAccount.getUserId())
                        .timestamp(LocalDateTime.now())
                        .details("Transfer failed: insufficient funds. From account balance: " + fromAccount.getBalance() + ", amount: " + amount)
                        .build());
                return new TransactionFailed("Insufficient funds", transaction);
            }

            // 8. Perform the transfer
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));

            // 9. Save the updated accounts
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            // 10. Update transaction status to SUCCESS
            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);

            // Audit the successful transfer
            auditLogRepository.save(AuditLog.builder()
                    .entityType("Transaction")
                    .entityId(transaction.getId())
                    .action("TRANSFER_SUCCESS")
                    .performedBy(fromAccount.getUserId())
                    .timestamp(LocalDateTime.now())
                    .details("Transfer successful. From: " + fromAccountId + ", To: " + toAccountId + ", Amount: " + amount + " " + currency)
                    .build());

            return new TransactionSuccess(transaction);
        } catch (Exception e) {
            // In case of any error, mark transaction as failed
            transaction.setStatus(TransactionStatus.FAILED_INVALID_ACCOUNT);
            transactionRepository.save(transaction);
            auditLogRepository.save(AuditLog.builder()
                    .entityType("Transaction")
                    .entityId(transaction.getId())
                    .action("TRANSFER_FAILED_ERROR")
                    .performedBy(userId)
                    .timestamp(LocalDateTime.now())
                    .details("Transfer failed due to: " + e.getMessage())
                    .build());
            return new TransactionFailed("Transaction failed due to: " + e.getMessage(), transaction);
        }
    }
}
