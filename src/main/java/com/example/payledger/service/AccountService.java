package com.example.payledger.service;

import com.example.payledger.exception.AccountNotFoundException;
import com.example.payledger.exception.InsufficientFundsException;
import com.example.payledger.model.Account;
import com.example.payledger.model.AuditLog;
import com.example.payledger.repository.AccountRepository;
import com.example.payledger.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuditLogRepository auditLogRepository;

    public Account createAccount(Long userId, String currency) {
        String accountNumber = generateAccountNumber(); // TODO: implement proper generation
        Account account = Account.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Account savedAccount = accountRepository.save(account);
        auditLogRepository.save(AuditLog.builder()
                .entityType("Account")
                .entityId(savedAccount.getId())
                .action("CREATE")
                .performedBy(userId)
                .timestamp(LocalDateTime.now())
                .details("Account created with currency: " + currency)
                .build());
        return savedAccount;
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    public Optional<Account> findByIdForUser(Long id, Long userId) {
        return accountRepository.findById(id)
                .filter(account -> account.getUserId().equals(userId));
    }

    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public List<Account> findByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Account deposit(Long accountId, Long userId, BigDecimal amount) {
        Account account = getOwnedAccount(accountId, userId);

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        Account savedAccount = accountRepository.save(account);
        auditLogRepository.save(AuditLog.builder()
                .entityType("Account")
                .entityId(savedAccount.getId())
                .action("DEPOSIT")
                .performedBy(userId)
                .timestamp(LocalDateTime.now())
                .details("Deposited amount: " + amount)
                .build());
        return savedAccount;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Account withdraw(Long accountId, Long userId, BigDecimal amount) {
        Account account = getOwnedAccount(accountId, userId);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        Account savedAccount = accountRepository.save(account);
        auditLogRepository.save(AuditLog.builder()
                .entityType("Account")
                .entityId(savedAccount.getId())
                .action("WITHDRAW")
                .performedBy(userId)
                .timestamp(LocalDateTime.now())
                .details("Withdrew amount: " + amount)
                .build());
        return savedAccount;
    }

    private Account getOwnedAccount(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        if (!account.getUserId().equals(userId)) {
            // Do not reveal that an account owned by someone else exists
            throw new AccountNotFoundException("Account not found: " + accountId);
        }
        return account;
    }

    private String generateAccountNumber() {
        // TODO: implement a proper account number generation (e.g., using sequence or random)
        return "ACC" + System.currentTimeMillis();
    }
}
