package com.fiap.bank.atm.domain.model;

import com.fiap.bank.atm.domain.exception.AccountBlockedException;
import com.fiap.bank.atm.domain.exception.DailyLimitExceededException;
import com.fiap.bank.atm.domain.exception.InsufficientFundsException;
import com.fiap.bank.atm.domain.exception.InvalidPinException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Regras de negócio do agregado Account")
class AccountTest {

    private static Account newAccount() {
        return Account.open(UUID.randomUUID(), "0001", "12345", "1234", Money.of(1000.00), Money.of(500.00));
    }

    @Test
    @DisplayName("Depósito credita o saldo e registra a transação")
    void shouldDeposit() {
        Account account = newAccount();

        account.deposit(Money.of(250.00));

        assertEquals(Money.of(1250.00), account.getBalance());
        assertEquals(1, account.getTransactions().size());
        assertEquals(TransactionType.DEPOSIT, account.getTransactions().get(0).getType());
    }

    @Test
    @DisplayName("Saque debita o saldo e consome o limite diário")
    void shouldWithdraw() {
        Account account = newAccount();

        account.withdraw(Money.of(200.00));

        assertEquals(Money.of(800.00), account.getBalance());
        assertEquals(Money.of(200.00), account.getTotalWithdrawnToday());
        assertEquals(Money.of(300.00), account.getAvailableDailyLimit());
    }

    @Test
    @DisplayName("Saque acima do saldo é rejeitado")
    void shouldRejectWithdrawWithoutFunds() {
        Account account = Account.open(UUID.randomUUID(), "0001", "12345", "1234", Money.of(50.00),
                Money.of(500.00));

        assertThrows(InsufficientFundsException.class, () -> account.withdraw(Money.of(100.00)));
        assertEquals(Money.of(50.00), account.getBalance());
    }

    @Test
    @DisplayName("Saque acima do limite diário é rejeitado")
    void shouldRejectWithdrawAboveDailyLimit() {
        Account account = newAccount();

        assertThrows(DailyLimitExceededException.class, () -> account.withdraw(Money.of(600.00)));
        assertEquals(Money.of(1000.00), account.getBalance());
    }

    @Test
    @DisplayName("Valores não positivos são rejeitados")
    void shouldRejectNonPositiveAmounts() {
        Account account = newAccount();

        assertThrows(IllegalArgumentException.class, () -> account.withdraw(Money.ZERO));
        assertThrows(IllegalArgumentException.class, () -> account.deposit(Money.ZERO));
    }

    @Test
    @DisplayName("Três senhas incorretas bloqueiam a conta")
    void shouldBlockAccountAfterThreeFailures() {
        Account account = newAccount();

        assertThrows(InvalidPinException.class, () -> account.authenticate("0000"));
        assertThrows(InvalidPinException.class, () -> account.authenticate("0000"));
        assertThrows(AccountBlockedException.class, () -> account.authenticate("0000"));

        assertTrue(account.isBlocked());
        assertEquals(AccountStatus.BLOCKED, account.getStatus());
        assertThrows(AccountBlockedException.class, () -> account.authenticate("1234"));
    }

    @Test
    @DisplayName("Autenticação bem-sucedida zera as tentativas falhas")
    void shouldResetFailedAttemptsOnSuccess() {
        Account account = newAccount();

        assertThrows(InvalidPinException.class, () -> account.authenticate("0000"));
        account.authenticate("1234");

        assertEquals(0, account.getFailedAttempts());
        assertFalse(account.isBlocked());
    }

    @Test
    @DisplayName("Transferência debita a origem e credita o destino")
    void shouldTransferBetweenAccounts() {
        Account source = newAccount();
        Account target = Account.open(UUID.randomUUID(), "0001", "67890", "5678", Money.of(100.00),
                Money.of(500.00));

        source.transfer(target, Money.of(300.00));

        assertEquals(Money.of(700.00), source.getBalance());
        assertEquals(Money.of(400.00), target.getBalance());
        assertEquals(TransactionType.TRANSFER_OUT, source.getTransactions().get(0).getType());
        assertEquals(TransactionType.TRANSFER_IN, target.getTransactions().get(0).getType());
    }

    @Test
    @DisplayName("Transferência para a própria conta é rejeitada")
    void shouldRejectTransferToSameAccountNumber() {
        Account source = newAccount();
        Account clone = Account.open(UUID.randomUUID(), "0001", "12345", "1234", Money.of(10.00),
                Money.of(500.00));

        assertThrows(IllegalArgumentException.class, () -> source.transfer(clone, Money.of(10.00)));
    }

    @Test
    @DisplayName("Transferência para conta bloqueada é rejeitada")
    void shouldRejectTransferToBlockedAccount() {
        Account source = newAccount();
        Account blocked = Account.restore(UUID.randomUUID(), "0001", "67890", "5678", Money.of(100.00),
                Money.of(500.00), AccountStatus.BLOCKED, 3, source.getCreatedAt(), source.getUpdatedAt(),
                List.of());

        assertThrows(AccountBlockedException.class, () -> source.transfer(blocked, Money.of(10.00)));
    }

    @Test
    @DisplayName("Extrato é devolvido do mais recente para o mais antigo")
    void shouldReturnStatementOrderedByMostRecent() {
        Account account = newAccount();

        account.deposit(Money.of(10.00));
        account.withdraw(Money.of(20.00));

        List<Transaction> statement = account.getStatement();

        assertEquals(2, statement.size());
        assertTrue(statement.get(0).getTimestamp().isAfter(statement.get(1).getTimestamp())
                || statement.get(0).getTimestamp().isEqual(statement.get(1).getTimestamp()));
    }

    @Test
    @DisplayName("A coleção de transações exposta é imutável")
    void shouldExposeImmutableTransactions() {
        Account account = newAccount();
        account.deposit(Money.of(10.00));

        assertThrows(UnsupportedOperationException.class, () -> account.getTransactions().clear());
    }
}
