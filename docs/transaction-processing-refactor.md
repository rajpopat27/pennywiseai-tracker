# Transaction Processing Architecture Refactor

## Current State Analysis

### Problem Summary
The transaction saving logic is duplicated across 3 different code paths, leading to:
- Inconsistent behavior (cashback only applied in one path)
- Code duplication (~50 lines repeated)
- Maintenance burden (changes need to be made in multiple places)
- Bug-prone architecture (easy to miss updating one path)

---

## Current Architecture

### Three Separate Code Paths:

#### Path 1: Direct Save (OptimizedSmsReaderWorker)
**Location:** `OptimizedSmsReaderWorker.kt:1041-1147`
**Trigger:** When `bypassConfirmationForScans = true` OR `confirmationEnabled = false`

```kotlin
// Current flow:
ParsedTransaction → toEntity() → TransactionEntity
    ↓
Check for deleted duplicate
    ↓
Apply merchant mapping
    ↓
Apply rules (block check + evaluate)
    ↓
Match subscription
    ↓
transactionRepository.insertTransaction()
    ↓
processBalanceUpdate()
```

**Issues:**
- ❌ Does NOT apply cashback
- ❌ Duplicates logic from other paths

---

#### Path 2: Confirm Pending Transaction (PendingTransactionManager)
**Location:** `PendingTransactionManager.kt:173-252`
**Trigger:** User confirms a pending transaction from UI

```kotlin
// Current flow:
PendingTransactionEntity → toTransactionEntity() → TransactionEntity
    ↓
Apply merchant mapping (only if category wasn't changed by user)
    ↓
Apply rules (block check + evaluate)
    ↓
Match subscription
    ↓
applyDefaultCashback() ✅
    ↓
pendingTransactionRepository.confirm()
    ↓
processBalanceUpdate()
```

**Status:** ✅ This is the most complete path - applies cashback

---

#### Path 3: Auto-save Expired Transactions (PendingTransactionManager)
**Location:** `PendingTransactionManager.kt:305-379`
**Trigger:** Pending transactions that expired without user action

```kotlin
// Current flow:
PendingTransactionEntity → toTransactionEntity() → TransactionEntity
    ↓
Apply merchant mapping
    ↓
Apply rules (block check + evaluate)
    ↓
Match subscription
    ↓
transactionRepository.insertTransaction()
    ↓
processBalanceUpdate()
```

**Issues:**
- ❌ Does NOT apply cashback
- ❌ Duplicates logic from Path 1 and Path 2

---

## Comparison Matrix

| Feature | Path 1 (Direct) | Path 2 (Confirm) | Path 3 (Auto-save) |
|---------|-----------------|------------------|-------------------|
| Duplicate check | ✅ Yes | ❌ No (relies on DB) | ❌ No |
| Merchant mapping | ✅ Yes | ✅ Yes (conditional) | ✅ Yes |
| Rule blocking | ✅ Yes | ✅ Yes | ✅ Yes |
| Rule evaluation | ✅ Yes | ✅ Yes | ✅ Yes |
| Subscription matching | ✅ Yes | ✅ Yes | ✅ Yes |
| **Cashback application** | ❌ **No** | ✅ **Yes** | ❌ **No** |
| Balance update | ✅ Yes | ✅ Yes | ✅ Yes |
| Save method | `transactionRepository` | `pendingTransactionRepository` | `transactionRepository` |

---

## Proposed Architecture

### Single Transaction Processing Pipeline

Create a `TransactionProcessor` class that handles ALL transaction saving logic in one place.

```
┌─────────────────────────────────────────────────────────────────┐
│                    TransactionProcessor                         │
│         (Single class for ALL transaction saving)               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  suspend fun processAndSave(                                    │
│      entity: TransactionEntity,                                 │
│      smsBody: String?,                                          │
│      bankName: String?,                                         │
│      accountLast4: String?,                                     │
│      skipDuplicateCheck: Boolean = false                        │
│  ): ProcessResult                                               │
│                                                                 │
│  Processing steps:                                              │
│     1. Check for duplicates/deleted (optional)                  │
│     2. Apply merchant mapping                                   │
│     3. Apply rules (block check + evaluate)                     │
│     4. Match subscription                                       │
│     5. Apply default cashback                                   │
│     6. Insert transaction                                       │
│     7. Process balance update                                   │
│     8. Save rule applications                                   │
│                                                                 │
│  Returns: ProcessResult (Success/Blocked/Duplicate/Error)       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                              ▲
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   Direct Save          Confirm Pending      Auto-save Expired
   (Worker)             (User action)        (Worker)
        │                     │                     │
        ▼                     ▼                     ▼
  ParsedTransaction   PendingTransaction   PendingTransaction
        │                     │                     │
        └─────────────────────┴─────────────────────┘
                              │
                              ▼
                      TransactionEntity
                              │
                              ▼
              TransactionProcessor.processAndSave()
```

---

## Implementation Details

### New Class: TransactionProcessor

```kotlin
@Singleton
class TransactionProcessor @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository
) {
    sealed class ProcessResult {
        data class Success(val transactionId: Long, val cashbackApplied: BigDecimal?) : ProcessResult()
        data class Blocked(val ruleName: String) : ProcessResult()
        data class Duplicate(val reason: String) : ProcessResult()
        data class Error(val message: String) : ProcessResult()
    }

    suspend fun processAndSave(
        entity: TransactionEntity,
        smsBody: String?,
        pendingEntity: PendingTransactionEntity? = null, // For balance updates
        skipDuplicateCheck: Boolean = false,
        preserveUserCategory: Boolean = false // If user edited category, don't apply mapping
    ): ProcessResult {
        // All processing logic here
    }
}
```

### Updated Flows

#### Direct Save (Worker):
```kotlin
val entity = parsedTransaction.toEntity()
val result = transactionProcessor.processAndSave(
    entity = entity,
    smsBody = sms.body,
    pendingEntity = null
)
```

#### Confirm Pending:
```kotlin
val entity = pending.toTransactionEntity()
val result = transactionProcessor.processAndSave(
    entity = entity,
    smsBody = pending.smsBody,
    pendingEntity = pending,
    skipDuplicateCheck = true,
    preserveUserCategory = (editedCategory != originalCategory)
)
// Then: pendingTransactionRepository.markConfirmed(pending.id)
```

#### Auto-save Expired:
```kotlin
val entity = pending.toTransactionEntity()
val result = transactionProcessor.processAndSave(
    entity = entity,
    smsBody = pending.smsBody,
    pendingEntity = pending,
    skipDuplicateCheck = true
)
// Then: pendingTransactionRepository.markAutoSaved(pending.id)
```

---

## Benefits

1. **Single Source of Truth**: All transaction processing logic in one place
2. **Consistent Behavior**: Cashback, rules, subscriptions applied everywhere
3. **No Code Duplication**: ~100+ lines of duplicated code eliminated
4. **Easy to Maintain**: Changes made once, applied everywhere
5. **Easy to Test**: One class to unit test with clear inputs/outputs
6. **Clear Contract**: `ProcessResult` sealed class makes outcomes explicit

---

## Migration Strategy

1. Create `TransactionProcessor` class with all logic
2. Update `OptimizedSmsReaderWorker` to use it (Path 1)
3. Update `PendingTransactionManager.confirmTransaction()` to use it (Path 2)
4. Update `PendingTransactionManager.autoSaveExpiredTransactions()` to use it (Path 3)
5. Remove duplicated code from all three locations
6. Test all flows to ensure consistent behavior

---

## Files Affected

- `app/src/main/java/com/pennywiseai/tracker/data/processor/TransactionProcessor.kt` (NEW)
- `app/src/main/java/com/pennywiseai/tracker/worker/OptimizedSmsReaderWorker.kt`
- `app/src/main/java/com/pennywiseai/tracker/data/manager/PendingTransactionManager.kt`
- `app/src/main/java/com/pennywiseai/tracker/di/AppModule.kt` (add DI binding)

---

## Related Issues

- Retroactive cashback not applying from Manage Accounts (fixed separately)
- Auto-synced transactions missing cashback (will be fixed by this refactor)
