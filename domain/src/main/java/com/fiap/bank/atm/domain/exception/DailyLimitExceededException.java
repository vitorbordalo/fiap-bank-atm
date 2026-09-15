package com.fiap.bank.atm.domain.exception;

public class DailyLimitExceededException extends DomainException {

    public DailyLimitExceededException(String message) {
        super(DomainErrorCode.DAILY_LIMIT_EXCEEDED, message);
    }
}
