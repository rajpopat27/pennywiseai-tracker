# Transaction Processing Refactor - TODO

## Overview
Refactor transaction saving logic to use a single `TransactionProcessor` class instead of 3 duplicated code paths.

**Reference:** See `docs/transaction-processing-refactor.md` for full architecture documentation.

---

## Phase 1: Create TransactionProcessor

### Task 1.1: Create TransactionProcessor class
- [ ] Create new file: `app/src/main/java/com/pennywiseai/tracker/data/processor/TransactionProcessor.kt`
- [ ] Define `ProcessResult` sealed class with Success, Blocked, Duplicate, Error variants
- [ ] Inject all required dependencies:
  - TransactionRepository
  - MerchantMappingRepository
  - RuleRepository
  - RuleEngine
  - SubscriptionRepository
  - AccountBalanceRepository
  - CardRepository

### Task 1.2: Implement processAndSave() method
- [ ] Step 1: Duplicate check (optional, controlled by parameter)
- [ ] Step 2: Apply merchant mapping (with option to preserve user-edited category)
- [ ] Step 3: Apply rules - block check
- [ ] Step 4: Apply rules - evaluate and transform
- [ ] Step 5: Match subscription
- [ ] Step 6: Apply default cashback (from account settings)
- [ ] Step 7: Insert transaction
- [ ] Step 8: Save rule applications
- [ ] Step 9: Return ProcessResult

### Task 1.3: Implement processBalanceUpdate() method
- [ ] Move balance update logic from PendingTransactionManager
- [ ] Handle card vs account detection
- [ ] Handle linked debit cards
- [ ] Create/update account balance records

### Task 1.4: Add Hilt DI binding
- [ ] Add @Singleton annotation to TransactionProcessor
- [ ] Ensure all dependencies are injectable

---

## Phase 2: Update OptimizedSmsReaderWorker (Path 1)

### Task 2.1: Inject TransactionProcessor
- [ ] Add TransactionProcessor to worker dependencies

### Task 2.2: Replace direct save logic
- [ ] Replace lines 1041-1147 with call to `transactionProcessor.processAndSave()`
- [ ] Handle ProcessResult appropriately
- [ ] Remove duplicated code:
  - Duplicate check logic
  - Merchant mapping logic
  - Rule evaluation logic
  - Subscription matching logic
  - Balance update logic

### Task 2.3: Test direct save flow
- [ ] Test with bypassConfirmationForScans = true
- [ ] Test with confirmationEnabled = false
- [ ] Verify cashback is now applied
- [ ] Verify balance updates work

---

## Phase 3: Update PendingTransactionManager.confirmTransaction() (Path 2)

### Task 3.1: Inject TransactionProcessor
- [ ] Add TransactionProcessor to PendingTransactionManager dependencies

### Task 3.2: Replace confirmTransaction logic
- [ ] Replace lines 173-252 with call to `transactionProcessor.processAndSave()`
- [ ] Pass `preserveUserCategory = true` when user edited the category
- [ ] Pass `skipDuplicateCheck = true` (pending already validated)
- [ ] Keep pending status update: `pendingTransactionRepository.confirm()`

### Task 3.3: Test confirm flow
- [ ] Test confirming pending transaction from UI
- [ ] Test with user-edited category (should preserve)
- [ ] Test with default category (should apply mapping)
- [ ] Verify cashback is applied
- [ ] Verify balance updates work

---

## Phase 4: Update PendingTransactionManager.autoSaveExpiredTransactions() (Path 3)

### Task 4.1: Replace auto-save logic
- [ ] Replace lines 305-379 with call to `transactionProcessor.processAndSave()`
- [ ] Pass `skipDuplicateCheck = true`
- [ ] Keep pending status update: `pendingTransactionRepository.markAutoSaved()`

### Task 4.2: Test auto-save flow
- [ ] Test expired pending transactions are auto-saved
- [ ] Verify cashback is now applied (was missing before!)
- [ ] Verify balance updates work
- [ ] Verify rules are applied

---

## Phase 5: Cleanup

### Task 5.1: Remove duplicated private methods
- [ ] Remove `applyDefaultCashback()` from PendingTransactionManager (moved to TransactionProcessor)
- [ ] Remove `processBalanceUpdate()` from PendingTransactionManager (moved to TransactionProcessor)
- [ ] Remove `processBalanceUpdate()` from OptimizedSmsReaderWorker (moved to TransactionProcessor)

### Task 5.2: Update imports and dependencies
- [ ] Remove unused imports from modified files
- [ ] Update any tests that mock the old methods

---

## Phase 6: Testing

### Task 6.1: Unit tests
- [ ] Write unit tests for TransactionProcessor.processAndSave()
- [ ] Test all ProcessResult variants
- [ ] Test cashback calculation
- [ ] Test rule blocking
- [ ] Test subscription matching

### Task 6.2: Integration tests
- [ ] Test full SMS → Transaction flow with confirmation disabled
- [ ] Test full SMS → Pending → Confirm flow
- [ ] Test full SMS → Pending → Auto-save (expired) flow
- [ ] Verify all three flows produce consistent results

### Task 6.3: Manual testing
- [ ] Test on device with confirmation enabled
- [ ] Test on device with confirmation disabled
- [ ] Verify cashback shows in Analytics for all transaction types
- [ ] Verify retroactive cashback still works from Manage Accounts

---

## Success Criteria

- [ ] All transaction saving goes through TransactionProcessor
- [ ] Cashback is applied in ALL flows (not just confirm)
- [ ] No duplicated processing logic in codebase
- [ ] All existing tests pass
- [ ] Manual testing confirms consistent behavior

---

## Estimated Impact

**Lines of code removed:** ~100-150 (duplicated logic)
**Lines of code added:** ~150-200 (TransactionProcessor class)
**Net change:** Slight increase, but much better organized

**Risk level:** Medium - core transaction flow changes
**Rollback plan:** Revert commits, no database changes needed
