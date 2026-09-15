package com.fiap.bank.atm.infrastructure.config;

import com.fiap.bank.atm.infrastructure.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class ConnectionFactory {

    private static final String DATABASE_URL_PROPERTY = "atm.database.url";
    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:fiap-bank-atm.db?foreign_keys=on";

    private final String databaseUrl;

    public ConnectionFactory() {
        this(System.getProperty(DATABASE_URL_PROPERTY, DEFAULT_DATABASE_URL));
    }

    public ConnectionFactory(String databaseUrl) {
        this.databaseUrl = Objects.requireNonNull(databaseUrl, "Database url cannot be null");
    }

    public Connection open() {
        try {
            Connection connection = DriverManager.getConnection(databaseUrl);
            connection.setAutoCommit(true);
            return connection;
        } catch (SQLException exception) {
            throw new DataAccessException("Falha ao abrir conexão com o banco de dados do ATM.", exception);
        }
    }

    public void close(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Falha ao encerrar a conexão com o banco de dados do ATM.", exception);
        }
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }
}
