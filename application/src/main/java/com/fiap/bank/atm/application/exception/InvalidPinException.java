package com.fiap.bank.atm.application.exception;

public class InvalidPinException extends AtmApplicationException {

    public InvalidPinException(String message) {
        super(message);
    }
}
