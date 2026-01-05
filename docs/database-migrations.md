# Database Migrations Guide

## Quick Reference

**Files to modify when transitioning to production:**
- `app/src/main/java/com/fintrace/app/data/database/FintraceDatabase.kt`
- `app/src/main/java/com/fintrace/app/di/DatabaseModule.kt`

**Imports needed for migrations (add to FintraceDatabase.kt):**
```kotlin
import androidx.room.AutoMigration
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
```

**Schema files location:** `app/schemas/com.fintrace.app.data.database.FintraceDatabase/`

---

## Current State (Alpha Development)

During alpha development, we use `fallbackToDestructiveMigration()` which **wipes the database when the schema changes**. This simplifies development but causes data loss for users.

**Current setup:**
```kotlin
// In FintraceDatabase.kt and DatabaseModule.kt
Room.databaseBuilder(context, FintraceDatabase::class.java, DATABASE_NAME)
    .fallbackToDestructiveMigration()  // Wipes DB on schema change
    .build()
```

**Database version is reset to 1** - increment as needed during alpha, data will be wiped anyway.

---

## Transitioning to Production

When ready for production release, follow these steps to enable proper migrations:

### Step 1: Remove Destructive Migration

Remove `.fallbackToDestructiveMigration()` from both files:

**FintraceDatabase.kt:**
```kotlin
fun getInstance(context: android.content.Context): FintraceDatabase {
    return INSTANCE ?: synchronized(this) {
        val instance = androidx.room.Room.databaseBuilder(
            context.applicationContext,
            FintraceDatabase::class.java,
            DATABASE_NAME
        )
            // REMOVE: .fallbackToDestructiveMigration()
            .addMigrations(/* add your migrations here */)
            .build()
        INSTANCE = instance
        instance
    }
}
```

**DatabaseModule.kt:**
```kotlin
val database = Room.databaseBuilder(
    context,
    FintraceDatabase::class.java,
    FintraceDatabase.DATABASE_NAME
)
    // REMOVE: .fallbackToDestructiveMigration()
    .addMigrations(/* add your migrations here */)
    .addCallback(DatabaseCallback())  // KEEP THIS - seeds default categories
    .build()
```

### Step 2: Set Your Production Starting Version

Before your first production release, decide on your starting version:
```kotlin
@Database(
    entities = [...],
    version = 1,  // This becomes your production baseline
    exportSchema = true
)
```

### Step 3: Build to Generate Schema

Build the project to generate the schema JSON file:
```bash
./gradlew :app:kspDebugKotlin
```

This creates `app/schemas/com.fintrace.app.data.database.FintraceDatabase/1.json`

### Step 4: Commit Schema Files

Add schema files to version control:
```bash
git add app/schemas/
git commit -m "Add database schema for production v1"
```

---

## Adding Migrations (Post-Production)

After your first production release, every schema change needs a migration.

### Step-by-Step Workflow for Schema Changes

**When you need to modify the database schema after production release:**

#### 1. Make Your Entity Changes
```kotlin
// Example: Add a new field to TransactionEntity
@Entity(tableName = "transactions")
data class TransactionEntity(
    // ... existing fields ...

    @ColumnInfo(name = "notes")
    val notes: String? = null  // NEW FIELD
)
```

#### 2. Increment Database Version
```kotlin
@Database(
    entities = [...],
    version = 2,  // Was 1, now 2
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)  // Add migration
    ]
)
```

#### 3. Build to Generate New Schema
```bash
./gradlew :app:kspDebugKotlin
```

This generates `app/schemas/.../2.json`

#### 4. Test the Migration
- Install the OLD version of the app
- Add some test data
- Install the NEW version over it
- Verify data is preserved

#### 5. Commit Everything
```bash
git add app/schemas/
git commit -m "Add notes field to transactions (migration v1->v2)"
```

### Migration Types

### Option A: Auto-Migration (Simple Changes)

For simple changes like adding nullable columns or new tables:

```kotlin
@Database(
    entities = [...],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
```

### Option B: Auto-Migration with Spec (Renames, Deletes)

For column/table renames or deletions:

```kotlin
@Database(
    entities = [...],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = Migration1To2::class)
    ]
)

// Define the spec class
@RenameColumn(tableName = "transactions", fromColumnName = "old_name", toColumnName = "new_name")
class Migration1To2 : AutoMigrationSpec
```

Available annotations:
- `@RenameColumn` - Rename a column
- `@RenameTable` - Rename a table
- `@DeleteColumn` - Delete a column
- `@DeleteTable` - Delete a table

### Option C: Manual Migration (Complex Changes)

For complex changes like changing column types or data transformations:

```kotlin
// Add to FintraceDatabase companion object:
companion object {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Example: Add a new non-nullable column with default
            db.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        }
    }
}

// Register in DatabaseModule.kt:
Room.databaseBuilder(...)
    .addMigrations(FintraceDatabase.MIGRATION_1_2)
    .build()
```

---

## Common Migration Patterns

### Adding a Nullable Column
```kotlin
// Auto-migration handles this automatically
// Just add the field to your entity and increment version
```

### Adding a Non-Nullable Column
```kotlin
db.execSQL("ALTER TABLE transactions ADD COLUMN new_column TEXT NOT NULL DEFAULT ''")
```

### Changing Column Type (Requires Table Rebuild)
```kotlin
override fun migrate(db: SupportSQLiteDatabase) {
    // 1. Create new table with correct schema
    db.execSQL("""
        CREATE TABLE transactions_new (
            id TEXT PRIMARY KEY NOT NULL,
            amount INTEGER NOT NULL,  -- Changed from TEXT to INTEGER
            ...
        )
    """)

    // 2. Copy data with conversion
    db.execSQL("""
        INSERT INTO transactions_new (id, amount, ...)
        SELECT id, CAST(amount AS INTEGER), ...
        FROM transactions
    """)

    // 3. Drop old table
    db.execSQL("DROP TABLE transactions")

    // 4. Rename new table
    db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

    // 5. Recreate indices
    db.execSQL("CREATE INDEX index_transactions_date ON transactions(date_time)")
}
```

### Deleting a Column
SQLite doesn't support DROP COLUMN before version 3.35.0. Use table rebuild (same pattern as above).

---

## Testing Migrations

Always test migrations before release:

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FintraceDatabase::class.java
    )

    @Test
    fun migrate1To2() {
        // Create database with version 1
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO transactions (id, ...) VALUES (...)")
            close()
        }

        // Run migration and validate
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Verify data integrity
        val db = helper.openDatabase(TEST_DB, 2)
        val cursor = db.query("SELECT * FROM transactions")
        // Assert data is preserved correctly
    }
}
```

---

## Production Migration Checklist

Before each production release with schema changes:

- [ ] Remove `fallbackToDestructiveMigration()` (first time only)
- [ ] Increment database version number
- [ ] Add appropriate migration (auto or manual)
- [ ] Build project to generate new schema JSON
- [ ] Commit schema files to version control
- [ ] Write migration tests
- [ ] Test migration on device with existing data
- [ ] Test fresh install (no migration needed)
- [ ] Test upgrade from oldest supported version

---

## Handling Version Skips

Users may skip app updates. For example, a user on v1 might update directly to v3.

### Auto-migrations handle this automatically
Room chains auto-migrations: `1 → 2 → 3`

### For manual migrations, ensure all paths work:
```kotlin
// In DatabaseModule.kt
.addMigrations(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_1_3  // Optional: direct path for efficiency
)
```

### Best Practice
Always test upgrading from the **oldest production version** to the **newest**.

---

## Troubleshooting

### "Schema 'X.json' required for migration was not found"
- Ensure `exportSchema = true` in `@Database` annotation
- Build the project to generate schema files
- Check that schema files exist in `app/schemas/`
- Commit schema files to version control

### "No migration path from version X to Y"
- Add the missing migration(s)
- Ensure migrations are registered with `.addMigrations()`

### "Room cannot verify the data integrity"
- Schema mismatch between expected and actual
- Run migration or clear app data

### Auto-migration fails
- Change may be too complex for auto-migration
- Use manual migration instead
- Check for missing annotations (`@DeleteColumn`, etc.)

---

## Reference: Previous Migrations

The following migrations were used during alpha development and are preserved here for reference. They are no longer active in the codebase.

<details>
<summary>Click to expand historical migrations</summary>

### MIGRATION_13_14
Added `sms_sender` column to transactions, `is_deleted` column to unrecognized_sms with unique constraint.

### MIGRATION_20_21
Made `next_payment_date` nullable in subscriptions table.

### MIGRATION_21_22 / MIGRATION_22_23
Added `transaction_rules` and `rule_applications` tables for the rule engine.

### MIGRATION_27_28
Added `account_type` column to `account_balances` table.

### MIGRATION_29_30
Dropped `chat_messages` table when removing AI chat features.

</details>
