# PayLedger

A mini core-banking REST API built with Java 21 and Spring Boot 3.2.0.

## Features

- User registration and authentication (JWT)
- Account management (create, deposit, withdraw)
- Money transfers with atomicity, idempotency, and locking for concurrent transfers
- Audit logging for all actions
- Uses Java 21 features: records for DTOs, sealed types for transaction results, virtual threads for concurrency

## Technology Stack

- Java 21
- Spring Boot 3.2.0
- Spring Data JPA
- PostgreSQL
- Spring Security (JWT)
- Flyway (for database migrations)
- Lombok
- Maven

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL

### Setup

1. Clone the repository
2. Create a PostgreSQL database named `payledger`
3. Update the `src/main/resources/application.yml` with your PostgreSQL credentials
4. Run the application:

   ```bash
   mvn spring-boot:run
   ```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login and get JWT token

### Accounts
- `POST /api/accounts` - Create a new account (requires authentication)
- `GET /api/accounts/{accountId}` - Get account by ID
- `GET /api/accounts` - Get all accounts for the authenticated user
- `POST /api/accounts/{accountId}/deposit` - Deposit money
- `POST /api/accounts/{accountId}/withdraw` - Withdraw money

### Transactions
- `POST /api/transactions/transfer` - Transfer money between accounts (requires Idempotency-Key header)

### Audit
- `GET /api/audit/account/{accountId}` - Get audit logs for an account
- `GET /api/audit/user/{userId}` - Get audit logs for a user

## Design Decisions

- **Atomic Transfers**: Uses `@Transactional` with `SERIALIZABLE` isolation and pessimistic locking (`SELECT FOR UPDATE`) to ensure consistency during concurrent transfers.
- **Idempotency**: Clients must provide an `Idempotency-Key` header for transfer requests. The service checks for existing transactions with the same key to prevent duplicate processing.
- **Locking**: To avoid deadlocks, accounts are locked in a consistent order (by account ID).
- **Java 21 Features**:
  - DTOs are implemented as records for immutability and concise syntax.
  - Transaction results use a sealed interface (`TransactionResult`) with implementations for success and failure.
  - Virtual threads are used for the Tomcat executor to handle high concurrency with minimal resource overhead.
- **Audit Trail**: All significant actions (account creation, deposits, withdrawals, transfers, login/logout) are logged to an audit table for compliance and debugging.

## Testing

Run the tests with:

```bash
mvn test
```

## License

This project is licensed under the MIT License.