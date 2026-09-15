package com.fiap.bank.atm.domain.exception;

public class InvalidPinException extends DomainException {

    public InvalidPinException(String message) {
        super(DomainErrorCode.INVALID_PIN, message);
    }
}
