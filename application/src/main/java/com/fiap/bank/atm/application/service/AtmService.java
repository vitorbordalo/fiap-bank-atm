package com.fiap.bank.atm.application.service;

import com.fiap.bank.atm.application.dto.AccountInfoDTO;
import com.fiap.bank.atm.application.dto.TransactionDTO;
import com.fiap.bank.atm.application.exception.DomainExceptionTranslator;
import com.fiap.bank.atm.application.exception.InvalidPinException;
import com.fiap.bank.atm.application.mapper.AtmMapper;
import com.fiap.bank.atm.domain.exception.DomainException;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class AtmService {

    private final AccountRepository accountRepository;

    private String currentAccountNumber;

    public AtmService(AccountRepository accountRepository) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "Account repository cannot be null");
    }

    public AccountInfoDTO authenticate(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new InvalidPinException("Conta não encontrada."));

        try {
            account.authenticate(pin);
        } catch (DomainException exception) {
            accountRepository.save(account);
            throw DomainExceptionTranslator.translate(exception);
        }

        accountRepository.save(account);
        currentAccountNumber = account.getAccountNumber();

        return AtmMapper.toAccountInfo(account);
    }

    public AccountInfoDTO withdraw(BigDecimal amount) {
        Account account = requireCurrentAccount();

        return guard(() -> {
            account.withdraw(Money.of(amount));
            accountRepository.save(account);
            return AtmMapper.toAccountInfo(account);
        });
    }

    public AccountInfoDTO deposit(BigDecimal amount) {
        Account account = requireCurrentAccount();

        return guard(() -> {
            account.deposit(Money.of(amount));
            accountRepository.save(account);
            return AtmMapper.toAccountInfo(account);
        });
    }

    public AccountInfoDTO transfer(String targetAccountNumber, BigDecimal amount) {
        Account source = requireCurrentAccount();
        Account target = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada."));

        return guard(() -> {
            source.transfer(target, Money.of(amount));
            accountRepository.save(source);
            accountRepository.save(target);
            return AtmMapper.toAccountInfo(source);
        });
    }

    public Optional<AccountInfoDTO> getCurrentAccount() {
        return Optional.ofNullable(currentAccountNumber)
                .flatMap(accountRepository::findByAccountNumber)
                .map(AtmMapper::toAccountInfo);
    }

    public BigDecimal getBalance() {
        return requireCurrentAccount().getBalance().getAmount();
    }

    public List<TransactionDTO> getStatement() {
        return AtmMapper.toTransactionList(requireCurrentAccount().getStatement());
    }

    public Boolean isAuthenticated() {
        return currentAccountNumber != null;
    }

    public void logout() {
        currentAccountNumber = null;
    }

    private Account requireCurrentAccount() {
        return Optional.ofNullable(currentAccountNumber)
                .flatMap(accountRepository::findByAccountNumber)
                .orElseThrow(() -> new IllegalStateException("Nenhum usuário está autenticado no momento."));
    }

    private <T> T guard(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DomainException exception) {
            throw DomainExceptionTranslator.translate(exception);
        }
    }
}
