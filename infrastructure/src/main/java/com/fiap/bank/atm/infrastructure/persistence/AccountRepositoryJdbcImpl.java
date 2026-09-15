package com.fiap.bank.atm.infrastructure.persistence;

import com.fiap.bank.atm.domain.model.Account;
import com.fiap.bank.atm.domain.model.AccountStatus;
import com.fiap.bank.atm.domain.model.Money;
import com.fiap.bank.atm.domain.model.Transaction;
import com.fiap.bank.atm.domain.model.TransactionType;
import com.fiap.bank.atm.domain.repository.AccountRepository;
import com.fiap.bank.atm.infrastructure.config.ConnectionFactory;
import com.fiap.bank.atm.infrastructure.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class AccountRepositoryJdbcImpl implements AccountRepository {

    private static final String FIND_BY_ID = """
            SELECT id, agency, number, pin, balance, daily_withdrawal_limit, failed_attempts, status, created_at, updated_at
            FROM tb_account
            WHERE id = ?
            """;

    private static final String FIND_BY_NUMBER = """
            SELECT id, agency, number, pin, balance, daily_withdrawal_limit, failed_attempts, status, created_at, updated_at
            FROM tb_account
            WHERE number = ?
            """;

    private static final String FIND_ALL = """
            SELECT id, agency, number, pin, balance, daily_withdrawal_limit, failed_attempts, status, created_at, updated_at
            FROM tb_account
            ORDER BY number
            """;

    private static final String FIND_TRANSACTIONS_BY_ACCOUNT = """
            SELECT id, account_id, type, amount, description, created_at
            FROM tb_transaction
            WHERE account_id = ?
            ORDER BY created_at DESC
            """;

    private static final String UPSERT_ACCOUNT = """
            INSERT INTO tb_account (id, agency, number, pin, balance, daily_withdrawal_limit, failed_attempts, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                balance = excluded.balance,
                daily_withdrawal_limit = excluded.daily_withdrawal_limit,
                failed_attempts = excluded.failed_attempts,
                status = excluded.status,
                updated_at = excluded.updated_at
            """;

    private static final String INSERT_TRANSACTION = """
            INSERT INTO tb_transaction (id, account_id, type, amount, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """;

    private static final String DELETE_TRANSACTIONS_BY_ACCOUNT = """
            DELETE FROM tb_transaction
            WHERE account_id = ?
            """;

    private static final String DELETE_ACCOUNT = """
            DELETE FROM tb_account
            WHERE id = ?
            """;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConnectionFactory connectionFactory;

    public AccountRepositoryJdbcImpl(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "Connection factory cannot be null");
    }

    @Override
    public Optional<Account> findById(UUID id) {
        Objects.requireNonNull(id, "Id cannot be null");
        return findOne(FIND_BY_ID, id.toString());
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        Objects.requireNonNull(accountNumber, "Account number cannot be null");
        return findOne(FIND_BY_NUMBER, accountNumber);
    }

    @Override
    public List<Account> findAll() {
        Connection connection = connectionFactory.open();
        try (PreparedStatement statement = connection.prepareStatement(FIND_ALL);
                ResultSet resultSet = statement.executeQuery()) {

            List<Account> accounts = new ArrayList<>();
            while (resultSet.next()) {
                accounts.add(mapAccount(connection, resultSet));
            }
            return List.copyOf(accounts);
        } catch (SQLException exception) {
            throw new DataAccessException("Falha ao consultar as contas cadastradas.", exception);
        } finally {
            connectionFactory.close(connection);
        }
    }

    @Override
    public void save(Account account) {
        Objects.requireNonNull(account, "Account cannot be null");

        Connection connection = connectionFactory.open();
        try {
            connection.setAutoCommit(false);
            saveAccount(connection, account);
            saveTransactions(connection, account);
            connection.commit();
        } catch (SQLException exception) {
            rollback(connection);
            throw new DataAccessException("Falha ao persistir a conta " + account.getAccountNumber() + ".", exception);
        } finally {
            connectionFactory.close(connection);
        }
    }

    @Override
    public void delete(UUID id) {
        Objects.requireNonNull(id, "Id cannot be null");

        Connection connection = connectionFactory.open();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(DELETE_TRANSACTIONS_BY_ACCOUNT)) {
                statement.setString(1, id.toString());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(DELETE_ACCOUNT)) {
                statement.setString(1, id.toString());
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException exception) {
            rollback(connection);
            throw new DataAccessException("Falha ao remover a conta " + id + ".", exception);
        } finally {
            connectionFactory.close(connection);
        }
    }

    private Optional<Account> findOne(String sql, String parameter) {
        Connection connection = connectionFactory.open();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAccount(connection, resultSet));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Falha ao consultar a conta solicitada.", exception);
        } finally {
            connectionFactory.close(connection);
        }
    }

    private void saveAccount(Connection connection, Account account) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_ACCOUNT)) {
            statement.setString(1, account.getId().toString());
            statement.setString(2, account.getAgency());
            statement.setString(3, account.getAccountNumber());
            statement.setString(4, account.getPin());
            statement.setBigDecimal(5, account.getBalance().getAmount());
            statement.setBigDecimal(6, account.getDailyWithdrawalLimit().getAmount());
            statement.setInt(7, account.getFailedAttempts());
            statement.setString(8, account.getStatus().name());
            statement.setString(9, format(account.getCreatedAt()));
            statement.setString(10, format(account.getUpdatedAt()));
            statement.executeUpdate();
        }
    }

    private void saveTransactions(Connection connection, Account account) throws SQLException {
        if (account.getTransactions().isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_TRANSACTION)) {
            for (Transaction transaction : account.getTransactions()) {
                statement.setString(1, transaction.getId().toString());
                statement.setString(2, transaction.getAccountId().toString());
                statement.setString(3, transaction.getType().name());
                statement.setBigDecimal(4, transaction.getAmount().getAmount());
                statement.setString(5, transaction.getDescription());
                statement.setString(6, format(transaction.getTimestamp()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Account mapAccount(Connection connection, ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));

        return Account.restore(
                id,
                resultSet.getString("agency"),
                resultSet.getString("number"),
                resultSet.getString("pin"),
                Money.of(resultSet.getBigDecimal("balance")),
                Money.of(resultSet.getBigDecimal("daily_withdrawal_limit")),
                AccountStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("failed_attempts"),
                parse(resultSet.getString("created_at")),
                parse(resultSet.getString("updated_at")),
                findTransactions(connection, id));
    }

    private List<Transaction> findTransactions(Connection connection, UUID accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_TRANSACTIONS_BY_ACCOUNT)) {
            statement.setString(1, accountId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Transaction> transactions = new ArrayList<>();
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
                return List.copyOf(transactions);
            }
        }
    }

    private Transaction mapTransaction(ResultSet resultSet) throws SQLException {
        return new Transaction(
                UUID.fromString(resultSet.getString("id")),
                UUID.fromString(resultSet.getString("account_id")),
                parse(resultSet.getString("created_at")),
                TransactionType.valueOf(resultSet.getString("type")),
                Money.of(resultSet.getBigDecimal("amount")),
                resultSet.getString("description"));
    }

    private String format(LocalDateTime timestamp) {
        return timestamp.format(TIMESTAMP_FORMATTER);
    }

    private LocalDateTime parse(String timestamp) {
        return LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            throw new DataAccessException("Falha ao desfazer a transação no banco de dados.", exception);
        }
    }
}
