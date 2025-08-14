package me.riddle.fintech.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void shouldCreateValidTransaction() {
        var transaction = Transaction.create(
                "acc-123",
                new BigDecimal("100.00"),
                "Test transaction",
                TransactionType.CREDIT
        );

        assertNotNull(transaction.id());
        assertEquals("acc-123", transaction.accountId());
        assertTrue(transaction.isCredit());
        assertFalse(transaction.isDebit());
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                Transaction.create(
                        "acc-123",
                        new BigDecimal("-100.00"),
                        "Invalid transaction",
                        TransactionType.DEBIT
                )
        );
    }

    @Test
    void shouldRejectBlankDescription() {
        assertThrows(IllegalArgumentException.class, () ->
                Transaction.create(
                        "acc-123",
                        new BigDecimal("100.00"),
                        "",
                        TransactionType.CREDIT
                )
        );
    }
}