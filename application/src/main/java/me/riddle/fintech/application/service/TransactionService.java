package me.riddle.fintech.application.service;

import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Try;
import me.riddle.fintech.domain.model.Transaction;
import me.riddle.fintech.domain.model.TransactionType;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Application service demonstrating functional programming with Vavr.
 * Shows error handling, function composition, and immutable operations.
 */
public class TransactionService {

    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("10000.00");
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("50000.00");

    // Repository interface - would be injected in real app
    public interface TransactionRepository {
        Try<Transaction> save(Transaction transaction);
        Try<List<Transaction>> findByAccountId(String accountId);
    }

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    /**
     * Process transaction with full functional error handling.
     * Shows: Either for business validation, Try for I/O operations, function composition.
     */
    public Either<String, Transaction> processTransaction(
            String accountId,
            BigDecimal amount,
            String description,
            TransactionType type) {

        return validateTransaction(accountId, amount, description, type)
                .flatMap(this::checkDailyLimits)
                .flatMap(this::createAndSaveTransaction);
    }

    /**
     * Business validation using Either for explicit error handling.
     * No exceptions - errors are values in the type system.
     */
    private Either<String, TransactionRequest> validateTransaction(
            String accountId, BigDecimal amount, String description, TransactionType type) {

        if (accountId == null || accountId.isBlank()) {
            return Either.left("Account ID cannot be blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Either.left("Amount must be positive");
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            return Either.left("Amount exceeds maximum limit of " + MAX_TRANSACTION_AMOUNT);
        }
        if (description == null || description.isBlank()) {
            return Either.left("Description cannot be blank");
        }
        if (type == null) {
            return Either.left("Transaction type cannot be null");
        }

        return Either.right(new TransactionRequest(accountId, amount, description, type));
    }

    /**
     * Check daily limits using functional composition.
     * Shows: Try for I/O, Vavr collections, functional data processing.
     */
    private Either<String, TransactionRequest> checkDailyLimits(TransactionRequest request) {
        return repository.findByAccountId(request.accountId())
                .toEither("Failed to check daily limits")
                .flatMap(transactions -> {
                    BigDecimal dailyTotal = calculateDailyTotal(transactions, request.type());
                    BigDecimal newTotal = dailyTotal.add(request.amount());

                    if (newTotal.compareTo(DAILY_LIMIT) > 0) {
                        return Either.left("Daily limit exceeded. Current: " + dailyTotal +
                                ", Attempted: " + request.amount() +
                                ", Limit: " + DAILY_LIMIT);
                    }

                    return Either.right(request);
                });
    }

    /**
     * Functional data processing with Vavr collections.
     * Shows: immutable operations, functional filtering and reducing.
     */
    private BigDecimal calculateDailyTotal(List<Transaction> transactions, TransactionType type) {
        return transactions
                .filter(t -> t.type() == type)
                .filter(this::isToday)
                .map(Transaction::amount)
                .foldLeft(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Create and save transaction using Try for I/O error handling.
     * Shows: Try composition, error transformation.
     */
    private Either<String, Transaction> createAndSaveTransaction(TransactionRequest request) {
        return Try.of(() -> Transaction.create(
                        request.accountId(),
                        request.amount(),
                        request.description(),
                        request.type()))
                .flatMap(repository::save)
                .toEither()
                .mapLeft(this::formatError);
    }

    /**
     * Get account transaction history with functional error handling.
     * Shows: Try to Either conversion, functional data transformation.
     */
    public Either<String, List<Transaction>> getTransactionHistory(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return Either.left("Account ID cannot be blank");
        }

        return repository.findByAccountId(accountId)
                .toEither("Failed to retrieve transaction history")
                .map(transactions -> transactions.sortBy(Transaction::timestamp).reverse());
    }

    /**
     * Calculate account balance functionally.
     * Shows: functional composition, Either chaining, business logic.
     */
    public Either<String, BigDecimal> calculateBalance(String accountId) {
        return getTransactionHistory(accountId)
                .map(this::computeBalanceFromTransactions);
    }

    /**
     * Pure functional balance calculation.
     * Shows: Vavr collections, functional data processing, immutable operations.
     */
    private BigDecimal computeBalanceFromTransactions(List<Transaction> transactions) {
        BigDecimal credits = transactions
                .filter(Transaction::isCredit)
                .map(Transaction::amount)
                .foldLeft(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debits = transactions
                .filter(Transaction::isDebit)
                .map(Transaction::amount)
                .foldLeft(BigDecimal.ZERO, BigDecimal::add);

        return credits.subtract(debits);
    }

    /**
     * Function composition example.
     * Shows: higher-order functions, composition, reusable business logic.
     */
    public Function<String, Either<String, String>> createTransactionSummary() {
        return accountId ->
                calculateBalance(accountId)
                        .flatMap(balance -> getTransactionHistory(accountId)
                                .map(transactions -> String.format(
                                        "Account %s: Balance=%.2f, Transactions=%d",
                                        accountId, balance, transactions.length())));
    }

    // Helper methods
    private boolean isToday(Transaction transaction) {
        // Simplified for demo - in real app would use proper date comparison
        return transaction.timestamp().toString().startsWith("2025-08-14");
    }

    private String formatError(Throwable throwable) {
        return "Transaction processing failed: " + throwable.getMessage();
    }

    // Value object for internal use
    private record TransactionRequest(
            String accountId,
            BigDecimal amount,
            String description,
            TransactionType type
    ) {}
}