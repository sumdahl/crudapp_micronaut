# Flyway Migration Guide

## Quick Reference

Your Flyway setup is now active! Here's how to use it:

### Migration Files Location
```
src/main/resources/db/migration/
├── V1__create_users_table.sql      ✅ (Created)
├── V2__your_next_migration.sql     (Future migrations)
└── V3__another_migration.sql       (Future migrations)
```

### Naming Convention
- **Format**: `V{version}__{description}.sql`
- **Version**: Integer (1, 2, 3...) or semantic (1.0, 1.1, 2.0)
- **Separator**: Double underscore `__`
- **Description**: Snake_case or words with underscores

**Valid Examples:**
```
✅ V1__create_users_table.sql
✅ V2__add_roles_table.sql
✅ V3__add_user_role_constraints.sql
✅ V1.1__hotfix_user_email.sql
```

**Invalid Examples:**
```
❌ V1_create_users.sql          (single underscore)
❌ 1__create_users.sql          (missing V prefix)
❌ V1__Create Users Table.sql   (spaces in filename)
```

## How Flyway Works

### First Run (Application Startup)
1. Flyway checks for `flyway_schema_history` table
2. If not exists, creates it
3. Scans `db/migration/` for SQL files
4. Runs all migrations in version order
5. Records each migration in `flyway_schema_history`

### Subsequent Runs
1. Checks `flyway_schema_history` table
2. Only runs **new** migrations not yet recorded
3. Validates checksums of existing migrations

### Migration Tracking Table
```sql
SELECT * FROM flyway_schema_history;
```

| Column | Description |
|--------|-------------|
| `installed_rank` | Execution order |
| `version` | Migration version (e.g., "1") |
| `description` | From filename (e.g., "create users table") |
| `type` | SQL or Java |
| `script` | Filename |
| `checksum` | File hash (detects changes) |
| `installed_on` | Execution timestamp |
| `execution_time` | Duration in ms |
| `success` | TRUE/FALSE |

## Creating New Migrations

### Example: Add Profile Photo Field

**Step 1:** Create migration file
```bash
touch src/main/resources/db/migration/V2__add_user_profile_photo.sql
```

**Step 2:** Write SQL
```sql
-- V2__add_user_profile_photo.sql

-- Add profile photo URL field to users table
ALTER TABLE users 
ADD COLUMN profile_photo_url VARCHAR(500);

-- Add index if frequently queried
CREATE INDEX idx_users_profile_photo ON users(profile_photo_url) 
WHERE profile_photo_url IS NOT NULL;

-- Add comment
COMMENT ON COLUMN users.profile_photo_url IS 'URL to user profile photo';
```

**Step 3:** Restart application
```bash
./gradlew run
```

Flyway will automatically detect and run V2 migration!

### Example: Add New Entity (Department)

**V3__create_departments_table.sql**
```sql
-- Create departments table
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add department_id to users table
ALTER TABLE users 
ADD COLUMN department_id UUID REFERENCES departments(id) ON DELETE SET NULL;

-- Create index for foreign key
CREATE INDEX idx_users_department_id ON users(department_id);

-- Add comments
COMMENT ON TABLE departments IS 'Organizational departments';
COMMENT ON COLUMN users.department_id IS 'User department assignment';
```

## Best Practices

### ✅ DO

1. **Use Transactions** (PostgreSQL auto-wraps each migration)
2. **Version Control** migrations with your code
3. **Test Locally** before deploying
4. **Write Idempotent** SQL when possible:
   ```sql
   -- Good
   CREATE TABLE IF NOT EXISTS users (...);
   
   -- Good  
   ALTER TABLE users 
   ADD COLUMN IF NOT EXISTS new_field VARCHAR(50);
   ```

5. **Create Indexes** for foreign keys and frequently queried fields
6. **Add Comments** for documentation
7. **One Feature** per migration file

### ❌ DON'T

1. **Never Modify** existing migrations after deployment
2. **Don't Skip** version numbers
3. **Avoid Large** data migrations in same file as schema changes
4. **Don't Use** `hibernate.hbm2ddl.auto=update` in production

## Rollback Strategy

Flyway Community Edition doesn't support automatic rollback. Use manual approach:

### Option 1: Undo Migration File
```sql
-- V4__add_column.sql (Applied)
ALTER TABLE users ADD COLUMN temp_field VARCHAR(50);

-- U4__undo_add_column.sql (Manual rollback)
ALTER TABLE users DROP COLUMN temp_field;
```

Run manually: `psql -U postgres -d crudapp_db -f U4__undo_add_column.sql`

### Option 2: Forward Fix
```sql
-- V5__remove_temp_column.sql (Better approach)
ALTER TABLE users DROP COLUMN IF EXISTS temp_field;
```

## Common Scenarios

### Scenario 1: Add Nullable Field
```sql
-- V2__add_phone_number.sql
ALTER TABLE users ADD COLUMN phone_number VARCHAR(20);
```

### Scenario 2: Add Non-Nullable Field with Default
```sql
-- V3__add_status.sql
ALTER TABLE users 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
```

### Scenario 3: Rename Column (Safe Way)
```sql
-- V4__rename_first_name.sql
-- Step 1: Add new column
ALTER TABLE users ADD COLUMN given_name VARCHAR(50);

-- Step 2: Copy data
UPDATE users SET given_name = first_name;

-- Step 3: Drop old column (or keep for backward compatibility)
ALTER TABLE users DROP COLUMN first_name;
```

### Scenario 4: Large Data Migration
```sql
-- V5__migrate_legacy_data.sql
-- Use batching for large datasets
WITH batch AS (
    SELECT id FROM users 
    WHERE migrated = false 
    LIMIT 1000
)
UPDATE users 
SET new_field = transform_function(old_field),
    migrated = true
WHERE id IN (SELECT id FROM batch);
```

## Configuration Reference

### Current Settings (application.properties)
```properties
# Disable Hibernate auto-DDL
jpa.default.properties.hibernate.hbm2ddl.auto=validate

# Enable Flyway
flyway.datasources.default.enabled=true
```

### Additional Options (Optional)
```properties
# Baseline existing database
flyway.datasources.default.baseline-on-migrate=true
flyway.datasources.default.baseline-version=0

# Custom migration location
flyway.datasources.default.locations=classpath:db/migration

# Clean database (DANGEROUS - dev only)
flyway.datasources.default.clean-disabled=false
```

## Troubleshooting

### Problem: Migration Failed
```
ERROR: Migration V2__add_field.sql failed
```

**Solution:**
1. Check `flyway_schema_history` table
2. Fix the migration file
3. Delete failed entry from history table:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '2';
   ```
4. Restart application

### Problem: Checksum Mismatch
```
ERROR: Checksum mismatch for migration V1__create_users_table.sql
```

**Solution:**
- **Never modify** existing migrations in production!
- For dev: 
  ```sql
  DELETE FROM flyway_schema_history;
  DROP TABLE users; -- Be careful!
  -- Restart app to re-run all migrations
  ```

### Problem: Out of Order Migration
```
WARN: Detected applied migration not resolved locally: 3
```

**Solution:**
```properties
flyway.datasources.default.out-of-order=true
```

## Migration Workflow

### Development
```bash
# 1. Create migration
touch src/main/resources/db/migration/V2__add_feature.sql

# 2. Write SQL
vi src/main/resources/db/migration/V2__add_feature.sql

# 3. Build and run
./gradlew clean build
./gradlew run

# 4. Verify
psql -U postgres -d crudapp_db -c "SELECT * FROM flyway_schema_history;"
```

### Production Deployment
```bash
# 1. Backup database
pg_dump -U postgres crudapp_db > backup_$(date +%Y%m%d).sql

# 2. Deploy application (includes new migrations)
./gradlew build
java -jar build/libs/crudapp-*.jar

# 3. Flyway runs automatically on startup

# 4. Verify
psql -U postgres -d crudapp_db -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

## Current Migration Status

### V1: Users Table (Baseline)
- ✅ Created users table with UUID primary key
- ✅ Added username, email, password fields
- ✅ Added firstName, lastName, active fields
- ✅ Created indexes on username, email, active
- ✅ Enabled UUID extension

### Next Steps
Create V2 migration when you need to:
- Add new fields to users table
- Create new tables
- Add constraints or indexes
- Modify data

## Resources

- **Flyway Documentation**: https://flywaydb.org/documentation/
- **Micronaut Flyway**: https://micronaut-projects.github.io/micronaut-flyway/latest/guide/
- **PostgreSQL UUID**: https://www.postgresql.org/docs/current/datatype-uuid.html
