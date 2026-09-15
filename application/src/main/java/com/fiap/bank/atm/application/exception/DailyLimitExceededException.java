package com.fiap.bank.atm.application.exception;

public class DailyLimitExceededException extends AtmApplicationException {

    public DailyLimitExceededException(String message) {
        super(message);
    }
}
