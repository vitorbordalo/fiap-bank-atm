package com.fiap.bank.atm.infrastructure.config;

import com.fiap.bank.atm.infrastructure.exception.DataAccessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DatabaseInitializer {

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";
    private static final String STATEMENT_SEPARATOR = ";";
    private static final String COMMENT_PREFIX = "--";

    private final ConnectionFactory connectionFactory;

    public DatabaseInitializer(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "Connection factory cannot be null");
    }

    public void initialize() {
        Connection connection = connectionFactory.open();
        try {
            connection.setAutoCommit(false);
            for (String statement : readStatements()) {
                execute(connection, statement);
            }
            connection.commit();
        } catch (SQLException exception) {
            rollback(connection);
            throw new DataAccessException("Falha ao inicializar o schema do banco de dados.", exception);
        } finally {
            connectionFactory.close(connection);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private List<String> readStatements() {
        return Arrays.stream(readScript().split(STATEMENT_SEPARATOR))
                .map(String::trim)
                .filter(statement -> !statement.isEmpty())
                .toList();
    }

    private String readScript() {
        try (InputStream input = DatabaseInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (input == null) {
                throw new DataAccessException("Script de schema não encontrado em " + SCHEMA_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .filter(line -> !line.trim().startsWith(COMMENT_PREFIX))
                        .collect(Collectors.joining("\n"));
            }
        } catch (IOException exception) {
            throw new DataAccessException("Falha ao ler o script de schema do banco de dados.", exception);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            throw new DataAccessException("Falha ao desfazer a inicialização do banco de dados.", exception);
        }
    }
}
