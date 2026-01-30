# Micronaut CRUD Application

A production-ready Micronaut-based CRUD application built with Clean Architecture principles, featuring comprehensive exception handling, separate DTOs for create/update operations, and PostgreSQL integration.

## Project Structure

This project follows Clean Architecture with clear separation of concerns:

```
src/main/java/com/micronaut/crud/
├── domain/                     # Domain Layer (Business Logic)
│   ├── entity/                 # Domain entities
│   │   ├── BaseEntity.java     # Base entity with common fields
│   │   └── User.java           # User entity
│   ├── repository/             # Repository interfaces
│   │   └── UserRepository.java # User repository
│   └── exception/              # Domain exceptions
│       ├── ResourceNotFoundException.java
│       └── DuplicateResourceException.java
│
├── application/                # Application Layer (Use Cases)
│   ├── dto/                    # Data Transfer Objects
│   │   ├── CreateUserRequest.java   # DTO for creating users
│   │   ├── UpdateUserRequest.java   # DTO for updating users
│   │   └── UserDTO.java             # Response DTO
│   ├── usecase/                # Business use cases
│   │   └── UserUseCase.java    # User business logic
│   └── exception/              # Application exceptions
│       └── ValidationException.java
│
├── infrastructure/             # Infrastructure Layer (Technical Details)
│   └── mapper/                 # Entity-DTO mappers
│       └── UserMapper.java
│
└── presentation/               # Presentation Layer (API Controllers)
    ├── controller/
    │   └── UserController.java # REST API endpoints
    ├── dto/
    │   └── ErrorResponse.java  # Standardized error responses
    └── exception/
        └── GlobalExceptionHandler.java # Global exception handling
```

## Technologies

- **Framework**: Micronaut 4.10.7
- **Build Tool**: Gradle
- **Java Version**: 17
- **Database**: PostgreSQL
- **ORM**: Micronaut Data JPA (Hibernate)
- **Migrations**: Flyway
- **Serialization**: Micronaut Serialization (Jackson)
- **Testing**: JUnit 5
- **Validation**: Jakarta Bean Validation

## Features

- ✅ Clean Architecture structure
- ✅ RESTful API with full CRUD operations
- ✅ PostgreSQL database integration
- ✅ **UUID primary keys** for better scalability
- ✅ **Flyway database migrations** for version-controlled schema management
- ✅ JPA/Hibernate ORM with HikariCP connection pooling
- ✅ Separate DTOs for create/update operations
- ✅ Global exception handling with standardized error responses
- ✅ Custom domain and application exceptions
- ✅ Bean validation with constraint violation handling
- ✅ Automated timestamp management
- ✅ Entity-DTO mapping layer
- ✅ JUnit 5 testing support

## Prerequisites

- Java 17 or higher
- PostgreSQL database
- Gradle (wrapper included)

## Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE crudapp_db;
```

2. Update database credentials in `src/main/resources/application.properties`:
```properties
datasources.default.url=jdbc:postgresql://localhost:5432/crudapp_db
datasources.default.username=postgres
datasources.default.password=your_password
datasources.default.driver-class-name=org.postgresql.Driver

# Schema validation (Flyway handles migrations)
jpa.default.properties.hibernate.hbm2ddl.auto=validate
jpa.default.properties.hibernate.show_sql=true

# Enable Flyway migrations
flyway.datasources.default.enabled=true
```

3. Flyway will automatically create the schema on first run

## Running the Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run
```

The application will start on `http://localhost:8080`

## Database Migrations

This project uses **Flyway** for database schema version control.

### How It Works

1. Migration files are in `src/main/resources/db/migration/`
2. On application startup, Flyway automatically runs pending migrations
3. Tracks executed migrations in `flyway_schema_history` table

### Current Migrations

- **V1__create_users_table.sql** - Creates users table with UUID primary keys

### Creating New Migrations

```bash
# Create new migration file
touch src/main/resources/db/migration/V2__your_migration_name.sql

# Example: Add a new field
echo "ALTER TABLE users ADD COLUMN phone VARCHAR(20);" > src/main/resources/db/migration/V2__add_phone_field.sql

# Run application - Flyway executes V2 automatically
./gradlew run
```

**Naming Convention**: `V{version}__{description}.sql`

For detailed migration guide, see [docs/FLYWAY_GUIDE.md](docs/FLYWAY_GUIDE.md)

## API Endpoints

### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create a new user |
| GET | `/api/users` | Get all users |
| GET | `/api/users/{id}` | Get user by ID |
| GET | `/api/users/username/{username}` | Get user by username |
| PUT | `/api/users/{id}` | Update user (password optional) |
| DELETE | `/api/users/{id}` | Delete user |

### Example Requests

**Create User:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Update User (password optional):**
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john.updated@example.com",
    "firstName": "John",
    "lastName": "Smith"
  }'
```

**Get User by ID:**
```bash
curl http://localhost:8080/api/users/1
```

**Get All Users:**
```bash
curl http://localhost:8080/api/users
```

**Delete User:**
```bash
curl -X DELETE http://localhost:8080/api/users/1
```

## Exception Handling

The application features comprehensive exception handling with standardized error responses:

### Error Response Format

```json
{
  "timestamp": "2026-01-30T16:03:45",
  "status": 409,
  "error": "Conflict",
  "message": "User already exists with username: 'johndoe'",
  "path": "/api/users",
  "validationErrors": []
}
```

### Validation Error Response

```json
{
  "timestamp": "2026-01-30T16:03:45",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users",
  "validationErrors": [
    {
      "field": "username",
      "message": "Username must be between 3 and 50 characters"
    },
    {
      "field": "email",
      "message": "Email should be valid"
    }
  ]
}
```

### Exception Types

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `ResourceNotFoundException` | 404 | Resource not found |
| `DuplicateResourceException` | 409 | Resource already exists |
| `ValidationException` | 400 | Custom validation failed |
| `ConstraintViolationException` | 400 | Bean validation failed |
| `IllegalArgumentException` | 400 | Invalid arguments |
| Generic exceptions | 500 | Internal server error |

## Testing

Run tests with:
```bash
./gradlew test
```

## Clean Architecture Layers

### 1. Domain Layer
- **Entities**: Core business objects (`User`, `BaseEntity`)
- **Repositories**: Interfaces defining data access contracts
- **Exceptions**: Domain-specific exceptions (`ResourceNotFoundException`, `DuplicateResourceException`)
- **Independence**: No dependencies on outer layers

### 2. Application Layer
- **DTOs**: Data transfer objects for API communication
  - `CreateUserRequest`: For user creation (password required)
  - `UpdateUserRequest`: For user updates (password optional)
  - `UserDTO`: Response DTO
- **Use Cases**: Business logic and orchestration (`UserUseCase`)
- **Exceptions**: Application-specific exceptions (`ValidationException`)
- **Depends on**: Domain layer only

### 3. Infrastructure Layer
- **Mappers**: Convert between entities and DTOs
- **Implementations**: Database implementations (auto-generated by Micronaut Data)
- **Depends on**: Domain and Application layers

### 4. Presentation Layer
- **Controllers**: REST API endpoints
- **DTOs**: Error response models
- **Exception Handlers**: Global exception handling
- **HTTP Handling**: Request/response management
- **Depends on**: Application layer (via Use Cases)

## Development

### Adding New Entities

1. Create entity in `domain/entity/`
2. Create repository interface in `domain/repository/`
3. Create DTOs in `application/dto/`
4. Create use case in `application/usecase/`
5. Create mapper in `infrastructure/mapper/`
6. Create controller in `presentation/controller/`

### Key Design Decisions

- **Separate Create/Update DTOs**: Different validation rules for create vs update operations
- **Password handling**: Password required for creation, optional for updates
- **Global exception handling**: Centralized error handling with consistent responses
- **Layered exceptions**: Domain and application exceptions for semantic clarity
- **Automated timestamps**: BaseEntity provides `createdAt` and `updatedAt` fields

### Dependencies

All dependencies are managed in `build.gradle`:
- Micronaut Data JPA
- PostgreSQL JDBC driver
- Jakarta Annotation API
- Micronaut Serialization (Jackson)
- HikariCP connection pooling
- Jakarta Bean Validation

## License

This project is open source and available under the MIT License.
