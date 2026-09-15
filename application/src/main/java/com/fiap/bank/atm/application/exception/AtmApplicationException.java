package com.fiap.bank.atm.application.exception;

public abstract class AtmApplicationException extends RuntimeException {

    protected AtmApplicationException(String message) {
        super(message);
    }
}
