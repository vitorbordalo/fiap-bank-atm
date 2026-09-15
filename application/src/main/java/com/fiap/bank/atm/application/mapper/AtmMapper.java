package com.fiap.bank.atm.application.mapper;

import com.fiap.bank.atm.application.dto.AccountInfoDTO;
import com.fiap.bank.atm.application.dto.TransactionDTO;
import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.Transaction;

import java.util.List;

public final class AtmMapper {

    private AtmMapper() {
    }

    public static AccountInfoDTO toAccountInfo(Account account) {
        return new AccountInfoDTO(
                account.getId(),
                account.getAgency(),
                account.getAccountNumber(),
                account.getBalance().getAmount(),
                account.getDailyWithdrawalLimit().getAmount(),
                account.getAvailableDailyLimit().getAmount(),
                account.getStatus().name(),
                account.isBlocked());
    }

    public static TransactionDTO toTransaction(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getType().name(),
                transaction.getType().getDescription(),
                transaction.getAmount().getAmount(),
                transaction.getDescription(),
                transaction.getTimestamp());
    }

    public static List<TransactionDTO> toTransactionList(List<Transaction> transactions) {
        return transactions.stream()
                .map(AtmMapper::toTransaction)
                .toList();
    }
}
