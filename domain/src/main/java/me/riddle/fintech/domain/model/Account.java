package me.riddle.fintech.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record Account(
        String id,
        String customerId,
        BigDecimal balance,
        List<Transaction> transactions
) {
    public Account {
        Objects.requireNonNull(id, "Account ID cannot be null");
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(balance, "Balance cannot be null");
        transactions = transactions != null ? List.copyOf(transactions) : List.of();
    }

    public BigDecimal calculateTotalDebits() {
        return transactions.stream()
                .filter(Transaction::isDebit)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalCredits() {
        return transactions.stream()
                .filter(Transaction::isCredit)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}