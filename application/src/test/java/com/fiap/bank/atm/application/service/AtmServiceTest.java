package com.fiap.bank.atm.application.service;

import com.fiap.bank.atm.application.dto.AccountInfoDTO;
import com.fiap.bank.atm.application.dto.TransactionDTO;
import com.fiap.bank.atm.application.exception.AccountBlockedException;
import com.fiap.bank.atm.application.exception.DailyLimitExceededException;
import com.fiap.bank.atm.application.exception.InsufficientFundsException;
import com.fiap.bank.atm.application.exception.InvalidPinException;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Orquestração do AtmService sobre contratos do domínio")
class AtmServiceTest {

    /**
     * Duplo de teste em memória: prova que o serviço depende apenas do contrato
     * {@code AccountRepository} do domínio, e não da implementação JDBC.
     */
    private static final class InMemoryAccountRepository implements AccountRepository {

        private final Map<UUID, Account> accounts = new LinkedHashMap<>();

        @Override
        public Optional<Account> findById(UUID id) {
            return Optional.ofNullable(accounts.get(id));
        }

        @Override
        public Optional<Account> findByAccountNumber(String accountNumber) {
            return accounts.values().stream()
                    .filter(account -> account.getAccountNumber().equals(accountNumber))
                    .findFirst();
        }

        @Override
        public List<Account> findAll() {
            return List.copyOf(new ArrayList<>(accounts.values()));
        }

        @Override
        public void save(Account account) {
            accounts.put(account.getId(), account);
        }

        @Override
        public void delete(UUID id) {
            accounts.remove(id);
        }
    }

    private static AtmService newService() {
        InMemoryAccountRepository repository = new InMemoryAccountRepository();
        repository.save(Account.open(UUID.randomUUID(), "0001", "12345", "1234", Money.of(1000.00),
                Money.of(500.00)));
        repository.save(Account.open(UUID.randomUUID(), "0002", "67890", "5678", Money.of(200.00),
                Money.of(500.00)));
        return new AtmService(repository);
    }

    @Test
    @DisplayName("Autenticação devolve DTO, nunca a entidade de domínio")
    void shouldAuthenticateAndReturnDto() {
        AtmService service = newService();

        AccountInfoDTO account = service.authenticate("12345", "1234");

        assertEquals("12345", account.accountNumber());
        assertEquals("0001", account.agency());
        assertEquals(new BigDecimal("1000.00"), account.balance());
        assertEquals(Boolean.FALSE, account.blocked());
        assertTrue(service.isAuthenticated());
    }

    @Test
    @DisplayName("Conta inexistente não retorna null")
    void shouldRejectUnknownAccount() {
        AtmService service = newService();

        assertThrows(InvalidPinException.class, () -> service.authenticate("00000", "1234"));
        assertFalse(service.isAuthenticated());
        assertTrue(service.getCurrentAccount().isEmpty());
    }

    @Test
    @DisplayName("Saque e depósito atualizam o saldo exposto pelo DTO")
    void shouldWithdrawAndDeposit() {
        AtmService service = newService();
        service.authenticate("12345", "1234");

        assertEquals(new BigDecimal("900.00"), service.withdraw(BigDecimal.valueOf(100)).balance());
        assertEquals(new BigDecimal("950.00"), service.deposit(BigDecimal.valueOf(50)).balance());
        assertEquals(new BigDecimal("950.00"), service.getBalance());
        assertEquals(new BigDecimal("400.00"), service.getCurrentAccount().orElseThrow().availableDailyLimit());
    }

    @Test
    @DisplayName("Transferência credita a conta de destino")
    void shouldTransfer() {
        AtmService service = newService();
        service.authenticate("12345", "1234");

        service.transfer("67890", BigDecimal.valueOf(100));

        assertEquals(new BigDecimal("900.00"), service.getBalance());

        service.logout();
        assertEquals(new BigDecimal("300.00"), service.authenticate("67890", "5678").balance());
    }

    @Test
    @DisplayName("Erros de domínio chegam à tela como exceções de aplicação")
    void shouldTranslateDomainExceptions() {
        AtmService service = newService();
        service.authenticate("67890", "5678");

        assertThrows(InsufficientFundsException.class, () -> service.withdraw(BigDecimal.valueOf(300)));
        assertThrows(IllegalArgumentException.class, () -> service.transfer("00000", BigDecimal.valueOf(10)));

        service.logout();
        service.authenticate("12345", "1234");

        assertThrows(DailyLimitExceededException.class, () -> service.withdraw(BigDecimal.valueOf(600)));
    }

    @Test
    @DisplayName("Bloqueio por senha incorreta é propagado como erro de aplicação")
    void shouldTranslateBlockedAccount() {
        AtmService service = newService();

        assertThrows(InvalidPinException.class, () -> service.authenticate("12345", "0000"));
        assertThrows(InvalidPinException.class, () -> service.authenticate("12345", "0000"));
        assertThrows(AccountBlockedException.class, () -> service.authenticate("12345", "0000"));
        assertThrows(AccountBlockedException.class, () -> service.authenticate("12345", "1234"));
    }

    @Test
    @DisplayName("Extrato é devolvido como lista de TransactionDTO")
    void shouldReturnStatementAsDtoList() {
        AtmService service = newService();
        service.authenticate("12345", "1234");
        service.deposit(BigDecimal.valueOf(10));

        List<TransactionDTO> statement = service.getStatement();

        assertEquals(1, statement.size());
        assertEquals("DEPOSIT", statement.get(0).type());
        assertEquals("Depósito", statement.get(0).typeDescription());
        assertEquals(new BigDecimal("10.00"), statement.get(0).amount());
    }

    @Test
    @DisplayName("Operações exigem sessão autenticada")
    void shouldRequireAuthenticatedSession() {
        AtmService service = newService();

        assertThrows(IllegalStateException.class, () -> service.withdraw(BigDecimal.valueOf(10)));
        assertThrows(IllegalStateException.class, () -> service.getStatement());
        assertThrows(IllegalStateException.class, () -> service.getBalance());
    }

    @Test
    @DisplayName("Logout encerra a sessão e devolve Optional vazio")
    void shouldLogout() {
        AtmService service = newService();
        service.authenticate("12345", "1234");

        service.logout();

        assertFalse(service.isAuthenticated());
        assertTrue(service.getCurrentAccount().isEmpty());
    }
}
