package com.fiap.bank.atm.application.exception;

import com.fiap.bank.atm.domain.exception.DomainException;

public final class DomainExceptionTranslator {

    private DomainExceptionTranslator() {
    }

    public static AtmApplicationException translate(DomainException exception) {
        String message = exception.getMessage();
        return switch (exception.getCode()) {
            case ACCOUNT_BLOCKED -> new AccountBlockedException(message);
            case INVALID_PIN -> new InvalidPinException(message);
            case INSUFFICIENT_FUNDS -> new InsufficientFundsException(message);
            case DAILY_LIMIT_EXCEEDED -> new DailyLimitExceededException(message);
        };
    }
}
