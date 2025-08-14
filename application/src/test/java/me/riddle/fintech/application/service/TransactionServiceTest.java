package me.riddle.fintech.application.service;

import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Try;
import me.riddle.fintech.domain.model.Transaction;
import me.riddle.fintech.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates functional testing patterns with Vavr.
 * Shows: mocking with functional interfaces, Either testing, functional composition testing.
 */
class TransactionServiceTest {

    private TransactionService service;
    private TestTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TestTransactionRepository();
        service = new TransactionService(repository);
    }

    @Test
    void shouldProcessValidTransaction() {
        // Given
        var accountId = "acc-123";
        var amount = new BigDecimal("100.00");
        var description = "Test transaction";
        var type = TransactionType.CREDIT;

        // When
        Either<String, Transaction> result = service.processTransaction(accountId, amount, description, type);

        // Then - Functional testing with Either
        assertTrue(result.isRight(), "Expected successful transaction");
        result.fold(
                error -> fail("Expected success but got error: " + error),
                transaction -> {
                    assertEquals(accountId, transaction.accountId());
                    assertEquals(amount, transaction.amount());
                    assertEquals(description, transaction.description());
                    assertEquals(type, transaction.type());
                    assertNotNull(transaction.id());
                    assertNotNull(transaction.timestamp());
                    return null;
                }
        );
    }

    @Test
    void shouldRejectBlankAccountId() {
        // When
        Either<String, Transaction> result = service.processTransaction(
                "", new BigDecimal("100.00"), "Test", TransactionType.CREDIT);

        // Then - Testing error cases with Either
        assertTrue(result.isLeft(), "Expected validation error");
        result.fold(
                error -> {
                    assertEquals("Account ID cannot be blank", error);
                    return null;
                },
                transaction -> fail("Expected error but got transaction: " + transaction)
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        Either<String, Transaction> result = service.processTransaction(
                "acc-123", new BigDecimal("-50.00"), "Invalid", TransactionType.DEBIT);

        assertTrue(result.isLeft());
        assertEquals("Amount must be positive", result.getLeft());
    }

    @Test
    void shouldRejectExcessiveAmount() {
        var largeAmount = new BigDecimal("50000.00");

        Either<String, Transaction> result = service.processTransaction(
                "acc-123", largeAmount, "Too much", TransactionType.CREDIT);

        assertTrue(result.isLeft());
        assertTrue(result.getLeft().contains("exceeds maximum limit"));
    }

    @Test
    void shouldCalculateBalanceCorrectly() {
        // Given - Set up transaction history
        var accountId = "acc-123";
        repository.addTransaction(Transaction.create(accountId, new BigDecimal("1000.00"), "Deposit", TransactionType.CREDIT));
        repository.addTransaction(Transaction.create(accountId, new BigDecimal("200.00"), "Purchase", TransactionType.DEBIT));
        repository.addTransaction(Transaction.create(accountId, new BigDecimal("50.00"), "Fee", TransactionType.DEBIT));

        // When
        Either<String, BigDecimal> result = service.calculateBalance(accountId);

        // Then
        assertTrue(result.isRight());
        result.fold(
                error -> fail("Expected balance calculation but got error: " + error),
                balance -> {
                    assertEquals(new BigDecimal("750.00"), balance);
                    return null;
                }
        );
    }

    @Test
    void shouldGetTransactionHistorySortedByTimestamp() {
        // Given
        var accountId = "acc-123";
        repository.addTransaction(Transaction.create(accountId, new BigDecimal("100.00"), "First", TransactionType.CREDIT));
        repository.addTransaction(Transaction.create(accountId, new BigDecimal("200.00"), "Second", TransactionType.CREDIT));

        // When
        Either<String, List<Transaction>> result = service.getTransactionHistory(accountId);

        // Then
        assertTrue(result.isRight());
        result.fold(
                error -> fail("Expected transaction history but got error: " + error),
                transactions -> {
                    assertEquals(2, transactions.length());
                    // Verify sorted by timestamp (newest first)
                    assertTrue(transactions.get(0).timestamp().isAfter(transactions.get(1).timestamp()));
                    return null;
                }
        );
    }

    @Test
    void shouldCreateTransactionSummary() {
        // Given
        var accountId = "acc-123";
        repository.addTransaction(Transaction.create(accountId, new BigDecimal("1000.00"), "Deposit", TransactionType.CREDIT));
        repository.addTransaction(Transaction.create(accountId, new BigDecimal("100.00"), "Purchase", TransactionType.DEBIT));

        // When - Function composition
        var summaryFunction = service.createTransactionSummary();
        Either<String, String> result = summaryFunction.apply(accountId);

        // Then
        assertTrue(result.isRight());
        result.fold(
                error -> fail("Expected summary but got error: " + error),
                summary -> {
                    assertTrue(summary.contains("Balance=900.00"));
                    assertTrue(summary.contains("Transactions=2"));
                    assertTrue(summary.contains(accountId));
                    return null;
                }
        );
    }

    @Test
    void shouldHandleRepositoryFailure() {
        // Given
        repository.setFailureMode(true);

        // When
        Either<String, List<Transaction>> result = service.getTransactionHistory("acc-123");

        // Then - Testing I/O failures
        assertTrue(result.isLeft());
        assertEquals("Failed to retrieve transaction history", result.getLeft());
    }

    /**
     * Test double that implements TransactionRepository interface.
     * Shows functional testing without heavy mocking frameworks.
     */
    private static class TestTransactionRepository implements TransactionService.TransactionRepository {
        private List<Transaction> transactions = List.empty();
        private boolean failureMode = false;

        @Override
        public Try<Transaction> save(Transaction transaction) {
            if (failureMode) {
                return Try.failure(new RuntimeException("Repository failure"));
            }
            transactions = transactions.append(transaction);
            return Try.success(transaction);
        }

        @Override
        public Try<List<Transaction>> findByAccountId(String accountId) {
            if (failureMode) {
                return Try.failure(new RuntimeException("Repository failure"));
            }
            List<Transaction> filtered = transactions.filter(t -> t.accountId().equals(accountId));
            return Try.success(filtered);
        }

        public void addTransaction(Transaction transaction) {
            transactions = transactions.append(transaction);
        }

        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }
    }
}