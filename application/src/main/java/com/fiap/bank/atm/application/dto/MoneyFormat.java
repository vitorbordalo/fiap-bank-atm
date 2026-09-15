package com.fiap.bank.atm.application.dto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

final class MoneyFormat {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private MoneyFormat() {
    }

    static String format(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }
}
