package com.fiap.bank.atm.application.exception;

public class InsufficientFundsException extends AtmApplicationException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
