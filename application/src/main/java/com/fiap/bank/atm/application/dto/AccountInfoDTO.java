package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountInfoDTO(
        UUID id,
        String agency,
        String accountNumber,
        BigDecimal balance,
        BigDecimal dailyWithdrawalLimit,
        BigDecimal availableDailyLimit,
        String status,
        Boolean blocked) {

    public String formattedBalance() {
        return MoneyFormat.format(balance);
    }

    public String formattedDailyWithdrawalLimit() {
        return MoneyFormat.format(dailyWithdrawalLimit);
    }

    public String formattedAvailableDailyLimit() {
        return MoneyFormat.format(availableDailyLimit);
    }
}
