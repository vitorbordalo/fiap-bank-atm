# FIAP Bank ATM — Refactoring DDD (Checkpoint 4)

Refatoração do emulador de Caixa Eletrônico do FIAP Bank para uma arquitetura Domain-Driven Design
modularizada em Maven, com persistência relacional em SQLite via JDBC puro.

**Disciplina:** Domain Driven Design - Java
**Professor:** Eduardo dos Santos Ramos
**Turma:** 2ESPH

## Integrantes

| RM | Nome completo |
|----|---------------|
| 561592 | Vitor [SOBRENOME] |

## Arquitetura

O projeto raiz é um agregador (`packaging pom`) com quatro submódulos físicos:

```
fiap-bank-atm
├── domain           entidades, value objects, regras de negócio e contratos de repositório
├── application      casos de uso, DTOs (Java Records) e tradução de erros de domínio
├── infrastructure   persistência JDBC/SQLite, fábrica de conexões e composition root
└── presentation     interface gráfica Java Swing (inalterada visualmente)
```

Dependências declaradas nos POMs:

| Módulo | Depende de |
|--------|------------|
| `domain` | — |
| `application` | `domain` |
| `presentation` | `application` (+ FlatLaf) |
| `infrastructure` | `domain`, `application`, `presentation` (+ driver SQLite) |

A camada de apresentação enxerga **exclusivamente** a camada de aplicação. Nenhuma classe do pacote
`domain` é visível para o Swing em tempo de compilação: o `pom.xml` de `presentation` não possui tag de
dependência para `domain` nem para `infrastructure`, e o código de tela só manipula DTOs, tipos nativos
do Java (`BigDecimal`, `UUID`, `Optional`, `Boolean`, `LocalDateTime`) e exceções de aplicação.

O `main` da aplicação (`com.fiap.bank.atm.AtmApplication`) reside em `infrastructure` porque é o
*composition root*: é o único ponto que conhece simultaneamente a implementação concreta do repositório
e a tela. Colocá-lo em `presentation` obrigaria a camada de apresentação a depender de `infrastructure`,
violando o isolamento exigido.

## Como executar

Requisitos: JDK 21 e Maven 3.9+.

```bash
mvn clean install
mvn -pl infrastructure exec:java
```

No Windows, o script `run.bat` executa os dois comandos acima; no Linux/macOS, `./run.sh`.

O banco de dados é criado automaticamente no primeiro start (arquivo `fiap-bank-atm.db`), junto com o
schema e a carga inicial de contas. Os dados sobrevivem ao reinício da aplicação.

Para apontar para outro arquivo de banco:

```bash
mvn -pl infrastructure exec:java -Datm.database.url="jdbc:sqlite:/caminho/atm.db?foreign_keys=on"
```

## Contas para teste

| Conta | Senha | Saldo inicial | Limite diário de saque |
|-------|-------|---------------|------------------------|
| 12345 | 1234 | R$ 5.000,00 | R$ 1.500,00 |
| 67890 | 5678 | R$ 1.200,00 | R$ 1.000,00 |
| 99999 | 9999 | R$ 50,00 | R$ 500,00 |

Três senhas incorretas consecutivas bloqueiam a conta, e o bloqueio é persistido no banco.

## Testes

```bash
mvn test
```

21 testes unitários cobrem as regras do agregado (`domain/src/test`) e a orquestração do serviço de
aplicação (`application/src/test`). O teste de aplicação usa um duplo de repositório em memória que
implementa `AccountRepository`, evidenciando que a camada de aplicação depende apenas do contrato do
domínio — a troca da implementação JDBC por outra não exige alteração de código no serviço nem na tela.

## Modelo de dados

```
tb_account (id, agency, number, pin, balance, daily_withdrawal_limit,
            failed_attempts, status, created_at, updated_at)

tb_transaction (id, account_id, type, amount, description, created_at)
                FOREIGN KEY (account_id) REFERENCES tb_account (id)
```

O dicionário de dados do Anexo 7.2 foi preservado (nomes de tabelas, chaves e colunas `id`, `agency`,
`number`, `balance`, `status`, `account_id`, `type`, `amount`, `created_at`) e estendido com as colunas
necessárias para as regras já existentes no emulador: `pin`, `daily_withdrawal_limit`, `failed_attempts`,
`description` e trilha de auditoria (`created_at` / `updated_at`).

O total sacado no dia não é armazenado: é derivado das transações via Streams API
(`Account.getTotalWithdrawnToday`), o que mantém o limite diário correto mesmo após reinícios.

## Mapeamento dos requisitos

| Requisito | Onde está implementado |
|-----------|------------------------|
| POM raiz agregador | `pom.xml` (`<packaging>pom</packaging>` + `<modules>`) |
| Quatro submódulos físicos | `domain/`, `application/`, `infrastructure/`, `presentation/` |
| Isolamento físico das camadas | `presentation/pom.xml` declara apenas `application` |
| DTOs em Java Records | `application/dto/AccountInfoDTO.java`, `application/dto/TransactionDTO.java` |
| Serviço expondo apenas DTOs e tipos Java | `application/service/AtmService.java` |
| Interface genérica `ATMRepository<T extends BaseEntity>` | `domain/repository/ATMRepository.java` |
| `AccountRepository` estendendo a abstração genérica | `domain/repository/AccountRepository.java` |
| Erradicação do `null` com `Optional<T>` | `ATMRepository`, `AccountRepository`, `AtmService`, `AtmFrame` |
| Streams API no lugar de laços imperativos | `Account.getTotalWithdrawnToday`, `Account.getStatement`, `AtmMapper.toTransactionList`, `AtmFrame.showVirtualReceipt` |
| Driver SQLite no módulo de infraestrutura | `infrastructure/pom.xml` (`org.xerial:sqlite-jdbc`) |
| Fábrica de conexões | `infrastructure/config/ConnectionFactory.java` |
| `AccountRepositoryJdbcImpl` respeitando o contrato do domínio | `infrastructure/persistence/AccountRepositoryJdbcImpl.java` |
| `PreparedStatement` e `ResultSet` em todas as rotinas | `AccountRepositoryJdbcImpl`, `DatabaseInitializer` |
| Frontend Swing preservado | `presentation/AtmFrame.java` e `AtmFrame.form` |

## Segurança e proteção do domínio

- Todas as instruções SQL são constantes fixas com parâmetros `?` preenchidos por métodos `set` do
  `PreparedStatement`. Não existe concatenação de strings para montagem de SQL nem uso de `Statement`.
- Escritas de conta e transações ocorrem na mesma transação JDBC (`setAutoCommit(false)` com
  `commit` / `rollback`).
- Exceções de domínio (`DomainException`) são traduzidas para exceções de aplicação em
  `application/exception/DomainExceptionTranslator.java`, de modo que a tela trate falhas de negócio sem
  conhecer o domínio.
- `SQLException` é encapsulada em `DataAccessException`, mantendo a assinatura dos contratos do domínio
  livre de detalhes de infraestrutura.
- As entidades continuam protegidas: coleções devolvidas são imutáveis, o estado só muda pelos métodos
  de negócio (`authenticate`, `withdraw`, `deposit`, `transfer`) e nenhuma entidade atravessa a fronteira
  da aplicação.

## Alterações na camada de apresentação

O comportamento e o layout das telas permanecem idênticos. Após o isolamento físico, o código Swing
precisou apenas ser religado aos novos contratos:

- imports de `domain.model` / `domain.exception` substituídos por `application.dto` /
  `application.exception`;
- `Account` / `Transaction` substituídos por `AccountInfoDTO` / `TransactionDTO`;
- valores enviados ao serviço convertidos para `BigDecimal`;
- verificações de `null` substituídas pelo tratamento de `Optional`;
- laço `for` do comprovante substituído por Streams API.

Nenhum componente visual, texto de tela, botão, animação ou fluxo de navegação foi modificado.
