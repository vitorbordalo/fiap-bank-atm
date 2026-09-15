package com.fiap.bank.atm.domain.exception;

public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException(String message) {
        super(DomainErrorCode.INSUFFICIENT_FUNDS, message);
    }
}
