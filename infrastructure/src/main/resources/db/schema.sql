CREATE TABLE IF NOT EXISTS tb_account (
    id VARCHAR(36) PRIMARY KEY,
    agency VARCHAR(10) NOT NULL,
    number VARCHAR(20) NOT NULL UNIQUE,
    pin VARCHAR(4) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL,
    daily_withdrawal_limit DECIMAL(15, 2) NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS tb_transaction (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    description VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (account_id) REFERENCES tb_account(id)
);

CREATE INDEX IF NOT EXISTS ix_transaction_account ON tb_transaction (account_id, created_at);

INSERT INTO tb_account (id, agency, number, pin, balance, daily_withdrawal_limit, failed_attempts, status, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440000', '0001', '12345', '1234', 5000.00, 1500.00, 0, 'ACTIVE',
        datetime('now', 'localtime', '-30 days'), datetime('now', 'localtime'))
ON CONFLICT DO NOTHING;

INSERT INTO tb_account (id, agency, number, pin, balance, daily_withdrawal_limit, failed_attempts, status, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440001', '0001', '67890', '5678', 1200.00, 1000.00, 0, 'ACTIVE',
        datetime('now', 'localtime', '-30 days'), datetime('now', 'localtime'))
ON CONFLICT DO NOTHING;

INSERT INTO tb_account (id, agency, number, pin, balance, daily_withdrawal_limit, failed_attempts, status, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440002', '0002', '99999', '9999', 50.00, 500.00, 0, 'ACTIVE',
        datetime('now', 'localtime', '-30 days'), datetime('now', 'localtime'))
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, description, created_at)
VALUES ('7c9e6679-7425-40de-944b-e07fc1f90a01', '550e8400-e29b-41d4-a716-446655440000', 'DEPOSIT', 2000.00,
        'Depósito em dinheiro', datetime('now', 'localtime', '-3 days'))
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, description, created_at)
VALUES ('7c9e6679-7425-40de-944b-e07fc1f90a02', '550e8400-e29b-41d4-a716-446655440000', 'TRANSFER_IN', 500.00,
        'Transf. de Conta 67890', datetime('now', 'localtime', '-2 days'))
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, description, created_at)
VALUES ('7c9e6679-7425-40de-944b-e07fc1f90a03', '550e8400-e29b-41d4-a716-446655440000', 'WITHDRAWAL', 100.00,
        'Saque eletrônico', datetime('now', 'localtime', '-1 days'))
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, description, created_at)
VALUES ('7c9e6679-7425-40de-944b-e07fc1f90a04', '550e8400-e29b-41d4-a716-446655440001', 'DEPOSIT', 1500.00,
        'Depósito inicial', datetime('now', 'localtime', '-5 days'))
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, description, created_at)
VALUES ('7c9e6679-7425-40de-944b-e07fc1f90a05', '550e8400-e29b-41d4-a716-446655440001', 'TRANSFER_OUT', 500.00,
        'Transf. para Conta 12345', datetime('now', 'localtime', '-2 days'))
ON CONFLICT DO NOTHING;

INSERT INTO tb_transaction (id, account_id, type, amount, description, created_at)
VALUES ('7c9e6679-7425-40de-944b-e07fc1f90a06', '550e8400-e29b-41d4-a716-446655440002', 'DEPOSIT', 50.00,
        'Abertura de conta', datetime('now', 'localtime', '-10 days'))
ON CONFLICT DO NOTHING;
