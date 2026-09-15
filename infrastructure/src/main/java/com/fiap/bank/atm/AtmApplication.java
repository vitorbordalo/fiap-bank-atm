package com.fiap.bank.atm;

import com.fiap.bank.atm.application.service.AtmService;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.config.ConnectionFactory;
import com.fiap.bank.atm.infrastructure.config.DatabaseInitializer;
import com.fiap.bank.atm.infrastructure.persistence.AccountRepositoryJdbcImpl;
import com.fiap.bank.atm.presentation.AtmFrame;

import javax.swing.SwingUtilities;

public class AtmApplication {

    public static void main(String[] args) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        new DatabaseInitializer(connectionFactory).initialize();

        AccountRepository accountRepository = new AccountRepositoryJdbcImpl(connectionFactory);
        AtmService atmService = new AtmService(accountRepository);

        SwingUtilities.invokeLater(() -> {
            AtmFrame mainFrame = new AtmFrame(atmService);
            mainFrame.setVisible(true);
        });
    }
}
