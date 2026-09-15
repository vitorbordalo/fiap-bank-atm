package com.fiap.bank.atm.domain.model;

public enum AccountStatus {

    ACTIVE("Ativa"),
    BLOCKED("Bloqueada");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
