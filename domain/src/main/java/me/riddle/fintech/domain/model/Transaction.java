package me.riddle.fintech.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Transaction(
        String id,
        String accountId,
        BigDecimal amount,
        String description,
        Instant timestamp,
        TransactionType type
) {
    public Transaction {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
    }

    public static Transaction create(String accountId, BigDecimal amount, String description, TransactionType type) {
        return new Transaction(
                UUID.randomUUID().toString(),
                accountId,
                amount,
                description,
                Instant.now(),
                type
        );
    }

    public boolean isDebit() {
        return type == TransactionType.DEBIT;
    }

    public boolean isCredit() {
        return type == TransactionType.CREDIT;
    }
}