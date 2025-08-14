package me.riddle.fintech.api;

import io.vavr.collection.List;
import io.vavr.control.Try;
import me.riddle.fintech.application.service.TransactionService;
import me.riddle.fintech.domain.model.Transaction;
import me.riddle.fintech.domain.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class FinTechCanary {

    private static final Logger log = LoggerFactory.getLogger(FinTechCanary.class);

    private final TransactionService service;

    public FinTechCanary() {
        // Wire up dependencies - in real app would use DI framework
        var repository = new InMemoryTransactionRepository();
        this.service = new TransactionService(repository);
    }

    /**
     * Run the complete Tinkering demonstration.
     */
    public void run() {
        log.info("Starting Fintech Service - Tinkering Demo");
        log.info("================================================");

        demoSuccessfulTransactions();
        demoBusinessValidation();
        demoBalanceCalculation();
        demoTransactionHistory();
        demoFunctionComposition();

        log.info("Demo complete - Tinkering with Vavr");
    }

    private void demoSuccessfulTransactions() {
        log.info("Demo 1: Processing valid transactions");
        processTransaction("acc-123", "1000.00", "Initial deposit", TransactionType.CREDIT);
        processTransaction("acc-123", "250.00", "Grocery shopping", TransactionType.DEBIT);
        processTransaction("acc-123", "50.00", "ATM withdrawal", TransactionType.DEBIT);
    }

    private void demoBusinessValidation() {
        log.info("Demo 2: Business validation with Either error handling");
        processTransaction("", "100.00", "Invalid account", TransactionType.CREDIT);
        processTransaction("acc-123", "-50.00", "Negative amount", TransactionType.DEBIT);
        processTransaction("acc-123", "15000.00", "Exceeds limit", TransactionType.CREDIT);
    }

    private void demoBalanceCalculation() {
        log.info("Demo 3: Functional balance calculation");
        service.calculateBalance("acc-123").fold(
                error -> {
                    log.error("Balance calculation failed: {}", error);
                    return null;
                },
                balance -> {
                    log.info("Account balance calculated: ${}", balance);
                    return null;
                }
        );
    }

    private void demoTransactionHistory() {
        log.info("Demo 4: Transaction history with immutable collections");
        service.getTransactionHistory("acc-123").fold(
                error -> {
                    log.error("Failed to retrieve transaction history: {}", error);
                    return null;
                },
                transactions -> {
                    log.info("Retrieved {} transactions:", transactions.length());
                    transactions.forEach(t ->
                            log.debug("Transaction: {} ${} - {}", t.type(), t.amount(), t.description())
                    );
                    return null;
                }
        );
    }

    private void demoFunctionComposition() {
        log.info("Demo 5: Function composition");
        var summaryFunction = service.createTransactionSummary();
        summaryFunction.apply("acc-123").fold(
                error -> {
                    log.error("Transaction summary failed: {}", error);
                    return null;
                },
                summary -> {
                    log.info("Account summary: {}", summary);
                    return null;
                }
        );
    }

    /**
     * Helper method to demonstrate Either-based error handling with proper logging.
     */
    private void processTransaction(String accountId, String amount, String description, TransactionType type) {
        var result = service.processTransaction(accountId, new BigDecimal(amount), description, type);

        result.fold(
                error -> {
                    log.warn("Transaction validation failed: {}", error);
                    return null;        // Hate this about Java!
                },
                transaction -> {
                    log.info("Transaction processed: {} ${} - {} [ID: {}]",
                            transaction.type(), transaction.amount(), transaction.description(),
                            transaction.id().substring(0, 8) + "...");
                    return null;
                }
        );
    }

    /**
     * In-memory repository for Tinkering demo.
     */
    private static class InMemoryTransactionRepository implements TransactionService.TransactionRepository {
        private static final Logger repoLog = LoggerFactory.getLogger(InMemoryTransactionRepository.class);
        private List<Transaction> transactions = List.empty();

        @Override
        public Try<Transaction> save(Transaction transaction) {
            transactions = transactions.append(transaction);
            repoLog.debug("Transaction saved: {} [Total: {}]", transaction.id(), transactions.length());
            return Try.success(transaction);
        }

        @Override
        public Try<List<Transaction>> findByAccountId(String accountId) {
            List<Transaction> filtered = transactions.filter(t -> t.accountId().equals(accountId));
            repoLog.debug("Found {} transactions for account: {}", filtered.length(), accountId);
            return Try.success(filtered);
        }
    }
}