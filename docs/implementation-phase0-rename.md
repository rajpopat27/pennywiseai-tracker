# Phase 0: Rename (PennyWise → Fintrace)

## Overview
Rename the entire project from PennyWise to Fintrace before starting any architectural changes.

**Goal:** All code, packages, classes, and resources use the new Fintrace branding.

**Important:** After completing Phase 0, all subsequent phases (1-7) will work with the new package structure (`com.fintrace.app`). Code examples in phase documents use new package names.

---

## Naming Convention

| Type | Old | New |
|------|-----|-----|
| Package | `com.pennywiseai.tracker` | `com.fintrace.app` |
| Application Class | `PennyWiseApplication` | `FintraceApplication` |
| Database | `PennyWiseDatabase` | `FintraceDatabase` |
| UI Components | `PennyWiseCard`, `PennyWiseScaffold` | `FintraceCard`, `FintraceScaffold` |
| App Name | "PennyWise" | "Fintrace" |

**Casing Rule:** Use `Fintrace` (single capital) for all class names.

---

## 0.1 Build Configuration

### 0.1.1 Update settings.gradle.kts
**File:** `settings.gradle.kts`

```kotlin
// BEFORE
rootProject.name = "pennywiseai-tracker"

// AFTER
rootProject.name = "fintrace"
```

- [ ] Update root project name

### 0.1.2 Update app/build.gradle.kts
**File:** `app/build.gradle.kts`

```kotlin
// BEFORE
android {
    namespace = "com.pennywiseai.tracker"
    defaultConfig {
        applicationId = "com.pennywiseai.tracker"
        // ...
    }
}

// AFTER
android {
    namespace = "com.fintrace.app"
    defaultConfig {
        applicationId = "com.fintrace.app"
        // ...
    }
}
```

- [ ] Update namespace
- [ ] Update applicationId

### 0.1.3 Update parser-core/build.gradle.kts
**File:** `parser-core/build.gradle.kts`

```kotlin
// BEFORE
group = "com.pennywiseai.parser"

// AFTER
group = "com.fintrace.parser"
```

- [ ] Update group name

---

## 0.2 Package Directory Structure

### 0.2.1 Rename Main App Directories
**Current:** `app/src/main/java/com/pennywiseai/tracker/`
**New:** `app/src/main/java/com/fintrace/app/`

```bash
# Directory structure to rename
app/src/main/java/com/pennywiseai/tracker/  →  app/src/main/java/com/fintrace/app/
app/src/test/java/com/pennywiseai/tracker/  →  app/src/test/java/com/fintrace/app/
app/src/androidTest/java/com/pennywiseai/tracker/  →  app/src/androidTest/java/com/fintrace/app/
```

- [ ] Rename main source directory
- [ ] Rename test source directory
- [ ] Rename androidTest source directory

### 0.2.2 Rename Parser Core Directories
**Current:** `parser-core/src/main/kotlin/com/pennywiseai/parser/`
**New:** `parser-core/src/main/kotlin/com/fintrace/parser/`

```bash
parser-core/src/main/kotlin/com/pennywiseai/parser/  →  parser-core/src/main/kotlin/com/fintrace/parser/
parser-core/src/test/kotlin/com/pennywiseai/parser/  →  parser-core/src/test/kotlin/com/fintrace/parser/
```

- [ ] Rename parser-core main directory
- [ ] Rename parser-core test directory

---

## 0.3 Package Declarations

### 0.3.1 Update All Package Declarations
**Pattern:** Find and replace in all `.kt` files

```kotlin
// BEFORE
package com.pennywiseai.tracker.data.database

// AFTER
package com.fintrace.app.data.database
```

**Files affected (app module):**
- All files in `data/` (~50+ files)
- All files in `domain/` (~10+ files)
- All files in `di/` (~5 files)
- All files in `ui/` (~40+ files)
- All files in `presentation/` (~20+ files)
- All files in `utils/` (~10+ files)
- All files in `worker/` (~5 files)

**Files affected (parser-core module):**
- All files in `core/` (~5 files)
- All files in `bank/` (~50+ files)

- [ ] Update all package declarations in app module
- [ ] Update all package declarations in parser-core module
- [ ] Update all package declarations in test files

### 0.3.2 Update All Import Statements
**Pattern:** Find and replace in all `.kt` files

```kotlin
// BEFORE
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.parser.core.TransactionType

// AFTER
import com.fintrace.app.data.database.entity.TransactionEntity
import com.fintrace.parser.core.TransactionType
```

- [ ] Update all imports in app module
- [ ] Update all imports in parser-core module
- [ ] Update all imports in test files

---

## 0.4 Class Renames

### 0.4.1 Application Class
**File:** `FintraceApplication.kt` (rename from `PennyWiseApplication.kt`)

```kotlin
// BEFORE
class PennyWiseApplication : Application() { ... }

// AFTER
class FintraceApplication : Application() { ... }
```

- [ ] Rename file `PennyWiseApplication.kt` → `FintraceApplication.kt`
- [ ] Rename class `PennyWiseApplication` → `FintraceApplication`
- [ ] Update all references

### 0.4.2 Database Class
**File:** `FintraceDatabase.kt` (rename from `PennyWiseDatabase.kt`)

```kotlin
// BEFORE
@Database(...)
abstract class PennyWiseDatabase : RoomDatabase() { ... }

// AFTER
@Database(...)
abstract class FintraceDatabase : RoomDatabase() { ... }
```

Also update DatabaseModule.kt:
```kotlin
// BEFORE
.databaseBuilder(context, PennyWiseDatabase::class.java, "pennywise_database")

// AFTER
.databaseBuilder(context, FintraceDatabase::class.java, "fintrace_database")
```

**Note:** Database name change will require migration or fresh install.

- [ ] Rename file `PennyWiseDatabase.kt` → `FintraceDatabase.kt`
- [ ] Rename class `PennyWiseDatabase` → `FintraceDatabase`
- [ ] Update database name in DatabaseModule
- [ ] Update all references to database class

### 0.4.3 UI Components
**Files to rename:**

| Old File | New File |
|----------|----------|
| `PennyWiseCard.kt` | `FintraceCard.kt` |
| `PennyWiseScaffold.kt` | `FintraceScaffold.kt` |

```kotlin
// BEFORE
@Composable
fun PennyWiseCard(...) { ... }

@Composable
fun PennyWiseScaffold(...) { ... }

// AFTER
@Composable
fun FintraceCard(...) { ... }

@Composable
fun FintraceScaffold(...) { ... }
```

- [ ] Rename `PennyWiseCard.kt` → `FintraceCard.kt`
- [ ] Rename `PennyWiseScaffold.kt` → `FintraceScaffold.kt`
- [ ] Update all usages of `PennyWiseCard` → `FintraceCard`
- [ ] Update all usages of `PennyWiseScaffold` → `FintraceScaffold`

### 0.4.4 Theme Components
**Check for any theme-related classes with PennyWise prefix:**

- [ ] Search for `PennyWise` in theme files
- [ ] Rename any found classes/functions

---

## 0.5 Android Manifest

### 0.5.1 Update AndroidManifest.xml
**File:** `app/src/main/AndroidManifest.xml`

```xml
<!-- BEFORE -->
<application
    android:name=".PennyWiseApplication"
    android:label="@string/app_name"
    ...>

<!-- AFTER -->
<application
    android:name=".FintraceApplication"
    android:label="@string/app_name"
    ...>
```

- [ ] Update application name reference
- [ ] Verify all activity/service references are correct

---

## 0.6 String Resources

### 0.6.1 Update strings.xml
**File:** `app/src/main/res/values/strings.xml`

```xml
<!-- BEFORE -->
<string name="app_name">PennyWise</string>

<!-- AFTER -->
<string name="app_name">Fintrace</string>
```

- [ ] Update app_name
- [ ] Search for any other "PennyWise" strings
- [ ] Update all found strings

---

## 0.7 Documentation

### 0.7.1 Update CLAUDE.md
**File:** `CLAUDE.md`

```markdown
<!-- BEFORE -->
# PennyWise Project Context

## Project Overview
PennyWise is a minimalist, AI-powered expense tracker...

<!-- AFTER -->
# Fintrace Project Context

## Project Overview
Fintrace is a minimalist, AI-powered expense tracker...
```

- [ ] Update project name throughout
- [ ] Update all package references
- [ ] Update component names (PennyWiseCard → FintraceCard, etc.)

### 0.7.2 Update Other Docs
**Files to update:**
- `docs/architecture.md`
- `docs/design.md`
- `docs/features-roadmap.md`
- `docs/implementation-*.md` (all phase docs)
- `prd.md`
- `README.md` (if exists)

- [ ] Update all documentation files

---

## 0.8 Proguard/R8 Rules

### 0.8.1 Update proguard-rules.pro
**File:** `app/proguard-rules.pro`

Check for any package-specific rules:
```proguard
# BEFORE
-keep class com.pennywiseai.tracker.** { *; }

# AFTER
-keep class com.fintrace.app.** { *; }
```

- [ ] Update any package references in proguard rules

---

## 0.9 Verification

### 0.9.1 Build Verification
```bash
./gradlew clean
./gradlew :app:assembleDebug
./gradlew :parser-core:build
```

- [ ] Clean build succeeds
- [ ] No compilation errors
- [ ] No unresolved references

### 0.9.2 Runtime Verification
- [ ] App launches successfully
- [ ] Database initializes (new or migrated)
- [ ] All screens load without crashes
- [ ] SMS parsing works
- [ ] Settings persist correctly

### 0.9.3 Test Verification
```bash
./gradlew test
./gradlew :parser-core:test
```

- [ ] All unit tests pass
- [ ] All parser tests pass

---

## Search Patterns for Verification

After completing all renames, search for remaining old references:

```bash
# Search for old package references
grep -r "pennywiseai" --include="*.kt" --include="*.xml" --include="*.gradle*" --include="*.md"

# Search for old class names
grep -r "PennyWise" --include="*.kt" --include="*.xml" --include="*.md"
```

**Expected:** Zero results (except possibly git history references)

---

## Rollback Plan

If issues are encountered:
1. Git revert to pre-rename commit
2. Fix issues identified
3. Re-attempt rename

**Recommendation:** Create a git commit before starting Phase 0, and commit after successful verification.

---

## Files Renamed Summary

| Old Name | New Name |
|----------|----------|
| `PennyWiseApplication.kt` | `FintraceApplication.kt` |
| `PennyWiseDatabase.kt` | `FintraceDatabase.kt` |
| `PennyWiseCard.kt` | `FintraceCard.kt` |
| `PennyWiseScaffold.kt` | `FintraceScaffold.kt` |
| `com/pennywiseai/tracker/` | `com/fintrace/app/` |
| `com/pennywiseai/parser/` | `com/fintrace/parser/` |

---

## Next Phase
→ Proceed to `implementation-phase1-data-layer.md`
