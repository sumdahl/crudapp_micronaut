# Database Migrations in Micronaut CRUD Application

## Overview

This document explains how database migrations work in this Micronaut application and provides best practices for managing schema changes.

## Current Setup: Hibernate Auto-DDL

### How It Works

The application currently uses Hibernate's **automatic schema generation** feature, configured in `application.properties`:

```properties
jpa.default.properties.hibernate.hbm2ddl.auto=update
```

### What `hibernate.hbm2ddl.auto` Does

Hibernate reads your JPA entity classes (like `User.java`) and automatically manages the database schema based on these settings:

| Setting | Behavior | Use Case |
|---------|----------|----------|
| `none` | No automatic schema management | Production (use with proper migration tools) |
| `validate` | Validates schema matches entities, no changes | Production verification |
| `update` | **Current setting** - Updates schema to match entities | Development |
| `create` | Drops and recreates schema on startup | Testing |
| `create-drop` | Creates on startup, drops on shutdown | Integration tests |

### Current Behavior with `update`

When you start the application, Hibernate:

1. **Scans** all `@Entity` classes (e.g., `User`)
2. **Compares** entity definitions with existing database tables
3. **Generates** and **executes** DDL statements to update the schema:
   - Adds new tables if they don't exist
   - Adds new columns to existing tables
   - Updates column types if changed
   - Creates indexes based on annotations

**Example:** When we changed from `Long` to `UUID` for the ID:

```java
// Old:
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// New:
@GeneratedValue(strategy = GenerationType.UUID)
@Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
private UUID id;
```

Hibernate will:
- Alter the `users` table
- Change the `id` column type from `BIGINT` to `UUID`
- Update any foreign key constraints

### Limitations of `hibernate.hbm2ddl.auto=update`

⚠️ **Important Limitations:**

1. **Cannot remove columns** - Old columns remain in the database
2. **Cannot rename columns** - Sees rename as drop + add (doesn't drop old column)
3. **Cannot modify constraints** - Limited constraint modification support
4. **No rollback** - No way to undo schema changes
5. **No versioning** - No history of schema changes
6. **Data loss risks** - Type changes can cause data loss
7. **Production unsafe** - Race conditions in distributed deployments

## Production-Ready Migration Strategy

For production environments, you should use proper migration tools instead of Hibernate auto-DDL.

### Recommended: Flyway Migration Tool

Flyway is the recommended migration tool for Micronaut applications.

#### 1. Add Flyway Dependency

Add to `build.gradle`:

```gradle
dependencies {
    implementation("io.micronaut.flyway:micronaut-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
}
```

#### 2. Configure Flyway

Update `application.properties`:

```properties
# Disable Hibernate auto-DDL for production
jpa.default.properties.hibernate.hbm2ddl.auto=validate

# Enable Flyway
flyway.datasources.default.enabled=true
```

#### 3. Create Migration Files

Create migration files in `src/main/resources/db/migration/`:

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__add_user_names.sql
└── V3__migrate_to_uuid.sql
```

**Example: `V1__create_users_table.sql`**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

**Example: `V3__migrate_to_uuid.sql`**
```sql
-- Migration from BIGINT to UUID
-- WARNING: This is destructive if you have existing data with relationships

-- Option 1: If no data exists or data can be recreated
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Option 2: If you need to preserve data
-- This is more complex and requires mapping old IDs to new UUIDs
-- You would create a temporary table, migrate data with UUID generation,
-- then swap tables
```

#### 4. How Flyway Works

1. **Tracks versions** in `flyway_schema_history` table
2. **Runs migrations** in order (V1, V2, V3...)
3. **Checksums** ensure files haven't changed
4. **Idempotent** - Only runs migrations not yet applied

### Alternative: Liquibase

Another popular option with more features but higher complexity.

```gradle
implementation("io.micronaut.liquibase:micronaut-liquibase")
```

## Migration Strategy Comparison

| Feature | Hibernate Auto-DDL | Flyway | Liquibase |
|---------|-------------------|--------|-----------|
| Development speed | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| Production safe | ❌ | ✅ | ✅ |
| Version control | ❌ | ✅ | ✅ |
| Rollback support | ❌ | ✅ (manual) | ✅ (automatic) |
| Multi-database | ✅ | ✅ | ✅ |
| Learning curve | Low | Low | Medium |
| Team collaboration | Poor | Excellent | Excellent |

## Best Practices

### Development Phase

✅ **Do:**
- Use `hibernate.hbm2ddl.auto=update` for rapid development
- Test schema changes in a local database
- Document major schema changes

❌ **Don't:**
- Use auto-DDL in production
- Rely on auto-DDL for complex migrations
- Forget to create proper migrations before deployment

### Production Phase

✅ **Do:**
- Use Flyway or Liquibase
- Version control all migrations
- Test migrations on staging first
- Create rollback scripts
- Back up data before migrations
- Use `hibernate.hbm2ddl.auto=validate`

❌ **Don't:**
- Modify existing migration files after deployment
- Skip version numbers
- Use auto-DDL in production
- Deploy without testing migrations

## Transition Plan: Development → Production

### Step 1: Generate Initial Schema
```bash
# With current setup, start app to create schema
./gradlew run

# Export schema from database
pg_dump -U postgres -s crudapp_db > initial_schema.sql
```

### Step 2: Add Flyway
```gradle
implementation("io.micronaut.flyway:micronaut-flyway")
```

### Step 3: Create Baseline Migration
Create `V1__baseline.sql` with your exported schema

### Step 4: Update Configuration
```properties
# application-prod.properties
jpa.default.properties.hibernate.hbm2ddl.auto=validate
flyway.datasources.default.enabled=true
flyway.datasources.default.baseline-on-migrate=true
```

### Step 5: Future Changes
All schema changes now done via migration files:
- `V2__add_new_feature.sql`
- `V3__modify_constraints.sql`
- etc.

## UUID Migration Impact

The recent UUID migration affects:

### Database Level
- ID column type: `BIGINT` → `UUID`
- Storage: 8 bytes (BIGINT) → 16 bytes (UUID)
- Generation: Sequential → Random UUID v4

### Application Level
- Better for distributed systems (globally unique)
- No ID sequence conflicts
- Harder to guess/enumerate IDs (security)
- Slightly larger index size

### Migration Consideration
⚠️ **Breaking change!** Existing data with BIGINT IDs needs migration:
- Backup data before migration
- Consider data mapping strategy if relationships exist
- Test thoroughly in development first

## Current Application Status

- **ID Type**: UUID with `GenerationType.UUID`
- **Schema Management**: Hibernate auto-DDL (`update` mode)
- **Environment**: Development
- **Recommendation**: Migrate to Flyway before production deployment

## Resources

- [Micronaut Flyway Documentation](https://micronaut-projects.github.io/micronaut-flyway/latest/guide/)
- [Hibernate Schema Generation](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#schema-generation)
- [Flyway Migration Best Practices](https://flywaydb.org/documentation/)
