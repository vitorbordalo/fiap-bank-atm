package com.fiap.bank.atm.application.exception;

public class AccountBlockedException extends AtmApplicationException {

    public AccountBlockedException(String message) {
        super(message);
    }
}
