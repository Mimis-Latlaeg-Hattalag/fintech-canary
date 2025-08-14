package me.riddle.fintech.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void shouldCreateValidAccount() {
        var account = new Account(
                "acc-123",
                "cust-456",
                new BigDecimal("1000.00"),
                List.of()
        );

        assertEquals("acc-123", account.id());
        assertEquals("cust-456", account.customerId());
        assertEquals(new BigDecimal("1000.00"), account.balance());
        assertTrue(account.transactions().isEmpty());
    }

    @Test
    void shouldCalculateTotalDebits() {
        var debit1 = Transaction.create("acc-123", new BigDecimal("50.00"), "Withdrawal", TransactionType.DEBIT);
        var debit2 = Transaction.create("acc-123", new BigDecimal("25.00"), "Fee", TransactionType.DEBIT);
        var credit = Transaction.create("acc-123", new BigDecimal("100.00"), "Deposit", TransactionType.CREDIT);

        var account = new Account(
                "acc-123",
                "cust-456",
                new BigDecimal("1000.00"),
                List.of(debit1, debit2, credit)
        );

        assertEquals(new BigDecimal("75.00"), account.calculateTotalDebits());
    }

    @Test
    void shouldCalculateTotalCredits() {
        var credit1 = Transaction.create("acc-123", new BigDecimal("200.00"), "Deposit", TransactionType.CREDIT);
        var credit2 = Transaction.create("acc-123", new BigDecimal("50.00"), "Interest", TransactionType.CREDIT);
        var debit = Transaction.create("acc-123", new BigDecimal("25.00"), "Fee", TransactionType.DEBIT);

        var account = new Account(
                "acc-123",
                "cust-456",
                new BigDecimal("1000.00"),
                List.of(credit1, credit2, debit)
        );

        assertEquals(new BigDecimal("250.00"), account.calculateTotalCredits());
    }

    @Test
    void shouldHandleEmptyTransactionsList() {
        var account = new Account(
                "acc-123",
                "cust-456",
                new BigDecimal("1000.00"),
                List.of()
        );

        assertEquals(BigDecimal.ZERO, account.calculateTotalDebits());
        assertEquals(BigDecimal.ZERO, account.calculateTotalCredits());
    }

    @Test
    void shouldHandleNullTransactionsList() {
        var account = new Account(
                "acc-123",
                "cust-456",
                new BigDecimal("1000.00"),
                null
        );

        assertTrue(account.transactions().isEmpty());
        assertEquals(BigDecimal.ZERO, account.calculateTotalDebits());
        assertEquals(BigDecimal.ZERO, account.calculateTotalCredits());
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(NullPointerException.class, () ->
                new Account(
                        null,
                        "cust-456",
                        new BigDecimal("1000.00"),
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectNullCustomerId() {
        assertThrows(NullPointerException.class, () ->
                new Account(
                        "acc-123",
                        null,
                        new BigDecimal("1000.00"),
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectNullBalance() {
        assertThrows(NullPointerException.class, () ->
                new Account(
                        "acc-123",
                        "cust-456",
                        null,
                        List.of()
                )
        );
    }

    @Test
    void shouldCreateDefensiveCopyOfTransactions() {
        // Use ArrayList to test defensive copying (List.of() is already immutable)
        var mutableTransactions = new java.util.ArrayList<>(List.of(
                Transaction.create("acc-123", new BigDecimal("100.00"), "Deposit", TransactionType.CREDIT)
        ));

        var account = new Account(
                "acc-123",
                "cust-456",
                new BigDecimal("1000.00"),
                mutableTransactions
        );

        // Verify it's a defensive copy, not the same reference
        assertNotSame(mutableTransactions, account.transactions());
        assertEquals(mutableTransactions.size(), account.transactions().size());

        // Verify the copy is immutable by trying to modify original
        var originalSize = account.transactions().size();
        mutableTransactions.add(Transaction.create("acc-123", new BigDecimal("50.00"), "Fee", TransactionType.DEBIT));

        // Account's transactions should be unchanged
        assertEquals(originalSize, account.transactions().size());
    }

    @Test
    void shouldCalculateComplexTransactionMix() {
        // Real-world scenario: multiple transaction types
        var transactions = List.of(
                Transaction.create("acc-123", new BigDecimal("1000.00"), "Initial deposit", TransactionType.CREDIT),
                Transaction.create("acc-123", new BigDecimal("50.00"), "ATM withdrawal", TransactionType.DEBIT),
                Transaction.create("acc-123", new BigDecimal("2.50"), "ATM fee", TransactionType.DEBIT),
                Transaction.create("acc-123", new BigDecimal("500.00"), "Salary", TransactionType.CREDIT),
                Transaction.create("acc-123", new BigDecimal("200.00"), "Rent payment", TransactionType.DEBIT),
                Transaction.create("acc-123", new BigDecimal("1.25"), "Interest earned", TransactionType.CREDIT)
        );

        var account = new Account(
                "acc-123",
                "cust-456",
                new BigDecimal("1000.00"),
                transactions
        );

        // Total debits: 50.00 + 2.50 + 200.00 = 252.50
        assertEquals(new BigDecimal("252.50"), account.calculateTotalDebits());

        // Total credits: 1000.00 + 500.00 + 1.25 = 1501.25
        assertEquals(new BigDecimal("1501.25"), account.calculateTotalCredits());
    }
}