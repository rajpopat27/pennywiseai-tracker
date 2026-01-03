# Transaction Confirmation Popup - Implementation Plan

## Overview
Add a confirmation popup (like AirPods pairing dialog) when the app intercepts and parses bank SMS. Users can review, edit, and confirm before saving to database.

---

## User Requirements (Confirmed)

| Question | Answer |
|----------|--------|
| Expired pending transactions | Auto-save after 24 hours |
| Feature toggle | Yes - add setting to enable/disable |
| Quick Confirm in notification | Yes - saves with merchant mappings/rules applied |
| Multiple pending transactions | Show list screen; clicking item opens bottom sheet dialog; after confirm returns to list |
| Dialog design | Full-screen bottom sheet (like AirPods popup image) |
| When toggle OFF | Show notification (current behavior) |

---

## Architecture

### Data Flow
```
SMS Received → Parse Transaction → Check Feature Toggle
    │
    ├─→ [TOGGLE ON + FOREGROUND] → Save to pending_transactions → Emit to SharedFlow → Show Dialog
    │
    ├─→ [TOGGLE ON + BACKGROUND] → Save to pending_transactions → Show Notification (Review/Quick Confirm)
    │
    └─→ [TOGGLE OFF] → Direct save to transactions (current behavior) → Show Notification
```

### New Components

| Component | Type | Purpose |
|-----------|------|---------|
| `PendingTransactionEntity` | Room Entity | Store pending transactions |
| `PendingTransactionDao` | Room DAO | Database operations |
| `PendingTransactionRepository` | Repository | Data layer abstraction |
| `PendingTransactionManager` | Singleton | Queue management + SharedFlow |
| `PendingTransactionsListScreen` | Compose Screen | List all pending transactions |
| `TransactionConfirmationBottomSheet` | Compose Bottom Sheet | Review/edit single transaction |
| `PendingTransactionsViewModel` | ViewModel | State management |
| `PendingTransactionAutoSaveWorker` | WorkManager Worker | Auto-save expired transactions |

### Files to Modify

| File | Changes |
|------|---------|
| `SmsTransactionProcessor.kt` | Route to pending queue when toggle ON |
| `SmsBroadcastReceiver.kt` | Add Quick Confirm action to notification |
| `PennyWiseDatabase.kt` | Add entity, DAO, migration to v29 |
| `PennyWiseApp.kt` | Observe pending flow, show dialog |
| `AppNavigation.kt` | Add route for pending transactions screen |
| `SettingsScreen.kt` | Add feature toggle |
| `PreferencesManager.kt` | Add preference for toggle |

---

## Database Schema

### pending_transactions table
```sql
CREATE TABLE pending_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    amount TEXT NOT NULL,
    merchant_name TEXT NOT NULL,
    category TEXT NOT NULL,
    transaction_type TEXT NOT NULL,
    date_time TEXT NOT NULL,
    description TEXT,
    sms_body TEXT,
    bank_name TEXT,
    sms_sender TEXT,
    account_number TEXT,
    balance_after TEXT,
    transaction_hash TEXT NOT NULL UNIQUE,
    currency TEXT NOT NULL DEFAULT 'INR',
    from_account TEXT,
    to_account TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL
);

CREATE UNIQUE INDEX index_pending_transactions_transaction_hash
    ON pending_transactions (transaction_hash);
```

### PendingTransactionStatus enum
- `PENDING` - Awaiting user action
- `CONFIRMED` - User confirmed, saved to transactions
- `REJECTED` - User dismissed
- `AUTO_SAVED` - Expired and auto-saved

---

## UI Design

### PendingTransactionsListScreen
- Shows list of all pending transactions
- Each item shows: amount, merchant, bank, time ago
- Swipe to dismiss (reject)
- Tap to open bottom sheet
- "Confirm All" button at bottom

### TransactionConfirmationBottomSheet
Similar to AirPods popup:
- Rounded corners, white card on dimmed background
- Bank icon at top
- Amount (large, editable)
- Merchant name (editable)
- Category dropdown
- Transaction type chips (Income/Expense/Credit)
- Date/time (editable)
- Currency selector
- Expandable "SMS Details" section
- **Confirm** button (primary)
- **X** close button (top right)

---

## Implementation Phases

### Phase 1: Database Layer
- [ ] Create `PendingTransactionEntity`
- [ ] Create `PendingTransactionDao`
- [ ] Create `PendingTransactionRepository`
- [ ] Add migration v28 → v29
- [ ] Update `PennyWiseDatabase`

### Phase 2: Manager & Processor
- [ ] Create `PendingTransactionManager`
- [ ] Add preference for feature toggle
- [ ] Modify `SmsTransactionProcessor`

### Phase 3: UI - List Screen
- [ ] Create `PendingTransactionsListScreen`
- [ ] Create `PendingTransactionsViewModel`
- [ ] Add navigation route

### Phase 4: UI - Bottom Sheet
- [ ] Create `TransactionConfirmationBottomSheet`
- [ ] Add edit functionality for all fields
- [ ] Connect to ViewModel

### Phase 5: Background Flow
- [ ] Modify `SmsBroadcastReceiver` notification
- [ ] Add Quick Confirm broadcast receiver
- [ ] Handle notification tap to open list

### Phase 6: Auto-Save Worker
- [ ] Create `PendingTransactionAutoSaveWorker`
- [ ] Schedule periodic cleanup
- [ ] Apply merchant mappings/rules on auto-save

### Phase 7: Settings & Polish
- [ ] Add toggle in Settings screen
- [ ] Add pending count badge on home
- [ ] Handle edge cases

---

## Todo List (Current Progress)

- [ ] Create PendingTransactionEntity and PendingTransactionDao
- [ ] Create PendingTransactionRepository
- [ ] Add database migration for pending_transactions table
- [ ] Create PendingTransactionManager
- [ ] Modify SmsTransactionProcessor to use pending queue
- [ ] Create PendingTransactionsListScreen UI
- [ ] Create TransactionConfirmationBottomSheet (like AirPods popup)
- [ ] Create PendingTransactionsViewModel
- [ ] Add navigation route for pending transactions screen
- [ ] Modify SmsBroadcastReceiver notification with Quick Confirm
- [ ] Add feature toggle in settings
- [ ] Add auto-save worker for expired transactions

---

## Edge Cases

1. **App killed during review** - Pending persists in DB, shown on next launch
2. **Multiple SMS at once** - All added to pending list
3. **Duplicate SMS** - Hash-based deduplication in pending table
4. **User ignores for 24h** - Auto-saved with mappings/rules applied
5. **Toggle OFF while pending exist** - Show notification to review or auto-save

---

## Testing Checklist

- [ ] Foreground: SMS → Dialog appears
- [ ] Background: SMS → Notification with Review/Quick Confirm
- [ ] Quick Confirm saves correctly with mappings
- [ ] Edit fields in dialog work
- [ ] Confirm saves to transactions table
- [ ] Reject removes from pending
- [ ] Multiple pending shown in list
- [ ] Auto-save after 24h works
- [ ] Toggle ON/OFF works correctly
- [ ] Database migration works
