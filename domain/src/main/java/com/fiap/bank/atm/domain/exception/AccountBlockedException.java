package com.fiap.bank.atm.domain.exception;

public class AccountBlockedException extends DomainException {

    public AccountBlockedException(String message) {
        super(DomainErrorCode.ACCOUNT_BLOCKED, message);
    }
}
