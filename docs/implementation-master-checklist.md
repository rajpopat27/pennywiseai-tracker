# Master Implementation Checklist (Reorganized)

## Implementation Order

**Principle:** Rename First → Architecture → Bug Fixes → Removals → New Features

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 0: RENAME              │ PennyWise → Fintrace             │
├─────────────────────────────────────────────────────────────────┤
│ PHASE 1: DATA LAYER          │ Foundation                       │
│ PHASE 2: DOMAIN LAYER        │ Business Logic                   │
│ PHASE 3: PRESENTATION LAYER  │ State Management                 │
│ PHASE 4: UI LAYER            │ Components                       │
├─────────────────────────────────────────────────────────────────┤
│ PHASE 5: BUG FIXES           │ Fix all known issues             │
├─────────────────────────────────────────────────────────────────┤
│ PHASE 6: REMOVALS            │ Simplify the app                 │
├─────────────────────────────────────────────────────────────────┤
│ PHASE 7: NEW FEATURES        │ Budget, Analytics Tabs           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Phase Documents

| Phase | Document | Focus |
|-------|----------|-------|
| 0 | `implementation-phase0-rename.md` | PennyWise → Fintrace (package, classes, strings) |
| 1 | `implementation-phase1-data-layer.md` | Entities, DAOs, Indexes, Repositories |
| 2 | `implementation-phase2-domain-layer.md` | TransactionProcessor, Use Cases, Services |
| 3 | `implementation-phase3-presentation-layer.md` | ViewModels, FilterStateManager, Delegates |
| 4 | `implementation-phase4-ui-layer.md` | Components, Consolidation, Styling |
| 5 | `implementation-phase5-bug-fixes.md` | Memory Leak, Cashback, Alignment |
| 6 | `implementation-phase6-removals.md` | App Lock, Dev Options, Transaction Types |
| 7 | `implementation-phase7-new-features.md` | Budget, Tabbed Analytics, Charts |

---

## Phase 0: Rename (PennyWise → Fintrace)
**Status:** Not Started

| Task | Status |
|------|--------|
| Update `settings.gradle.kts` root project name | ⬜ |
| Update `app/build.gradle.kts` applicationId to `com.fintrace.app` | ⬜ |
| Rename package directories `com/pennywiseai/tracker` → `com/fintrace/app` | ⬜ |
| Update all package declarations in Kotlin files | ⬜ |
| Update all import statements | ⬜ |
| Rename `PennyWiseCard` → `FintraceCard` | ⬜ |
| Rename `PennyWiseScaffold` → `FintraceScaffold` | ⬜ |
| Rename `PennyWiseApplication` → `FintraceApplication` | ⬜ |
| Rename `PennyWiseDatabase` → `FintraceDatabase` | ⬜ |
| Update `AndroidManifest.xml` references | ⬜ |
| Update string resources (app_name, etc.) | ⬜ |
| Update `CLAUDE.md` references | ⬜ |
| Update all docs references | ⬜ |
| Rename project directory (optional) | ⬜ |
| Verify build succeeds | ⬜ |
| Verify app runs correctly | ⬜ |

---

## Phase 1: Data Layer Architecture
**Status:** Not Started

| Task | Status |
|------|--------|
| Create `Currency` constant | ⬜ |
| Add `updatedAt` to AccountBalanceEntity | ⬜ |
| Add database indexes to TransactionEntity | ⬜ |
| Add database indexes to AccountBalanceEntity | ⬜ |
| Create `LatestAccountBalanceView` database view | ⬜ |
| Standardize DAO return types | ⬜ |
| Add aggregation queries for analytics | ⬜ |
| Extract monthly breakdown helper | ⬜ |
| Delete `CurrencyUtils.kt`, keep `CurrencyFormatter.kt` | ⬜ |
| Create `BudgetEntity` | ⬜ |
| Create `BudgetHistoryEntity` | ⬜ |
| Create `BudgetDao` | ⬜ |
| Create database migration | ⬜ |

---

## Phase 2: Domain Layer Architecture
**Status:** Not Started

| Task | Status |
|------|--------|
| Create `ProcessResult` sealed class | ⬜ |
| Create `TransactionProcessor` | ⬜ |
| Create `CashbackService` | ⬜ |
| Create `BudgetRepository` interface | ⬜ |
| Create `BudgetRepositoryImpl` | ⬜ |
| Create `HiddenAccountsRepository` | ⬜ |
| Create `AppPreferencesRepository` | ⬜ |
| Create `ProcessTransactionUseCase` | ⬜ |
| Create `GetBudgetWithSpendingUseCase` | ⬜ |
| Create `ApplyRetroactiveCashbackUseCase` | ⬜ |
| Update `OptimizedSmsReaderWorker` to use TransactionProcessor | ⬜ |
| Update `PendingTransactionManager` to use TransactionProcessor | ⬜ |

---

## Phase 3: Presentation Layer Architecture
**Status:** Not Started

| Task | Status |
|------|--------|
| Create `FilterStateManager` | ⬜ |
| Create `UndoableDeleteDelegate` | ⬜ |
| Create `UiState` pattern | ⬜ |
| Update `TransactionsViewModel` with FilterStateManager | ⬜ |
| Update `AnalyticsViewModel` with FilterStateManager | ⬜ |
| Simplify `HomeViewModel` | ⬜ |
| Update `ManageAccountsViewModel` with HiddenAccountsRepository | ⬜ |
| Standardize loading/error patterns | ⬜ |

---

## Phase 4: UI Layer Architecture
**Status:** Not Started

| Task | Status |
|------|--------|
| Create unified `TransactionListItem` | ⬜ |
| Create `EmptyStateCard` | ⬜ |
| Create generic `FilterDialog` | ⬜ |
| Replace raw `Card` with `FintraceCard` | ⬜ |
| Create `Spacing` and `Dimensions` constants | ⬜ |
| Extract HomeScreen components | ⬜ |
| Remove dead code from HomeScreen | ⬜ |
| Fix filter card consistent padding | ⬜ |

---

## Phase 5: Bug Fixes
**Status:** Not Started

| Task | Status |
|------|--------|
| Fix memory leak in HomeViewModel | ⬜ |
| Fix custom cashback retroactive bug | ⬜ |
| Fix Income/Expense/Net alignment | ⬜ |
| Center align balance on Home screen | ⬜ |
| Add sync/settings to Pending screen | ⬜ |
| Add category line separators | ⬜ |
| Show cashback in transaction details | ⬜ |
| Show remaining limit for credit accounts | ⬜ |

---

## Phase 6: Feature Removals
**Status:** Not Started

| Task | Status |
|------|--------|
| Simplify TransactionType (remove CREDIT, INVESTMENT) | ⬜ |
| Update all bank parsers | ⬜ |
| Remove App Lock feature | ⬜ |
| Remove Developer Options | ⬜ |
| Remove Support & Community | ⬜ |
| Delete `SmsReaderWorker.kt` | ⬜ |
| Update SMS scan period options | ⬜ |
| Clean up dead code | ⬜ |

---

## Phase 7: New Features
**Status:** Not Started

| Task | Status |
|------|--------|
| Add Vico chart library | ⬜ |
| Create `SpendingTrendChart` | ⬜ |
| Create `BudgetComparisonChart` | ⬜ |
| Create `DayOfWeekChart` | ⬜ |
| Add budget to Settings | ⬜ |
| Create `BudgetInputDialog` | ⬜ |
| Create `BudgetProgressCard` | ⬜ |
| Update AnalyticsViewModel for tabs | ⬜ |
| Create `OverviewTab` | ⬜ |
| Create `BudgetTab` | ⬜ |
| Create `InsightsTab` | ⬜ |
| Update AnalyticsScreen with TabRow | ⬜ |

---

## Files Created Summary

### Phase 1
- `data/database/entity/Currency.kt`
- `data/database/view/LatestAccountBalanceView.kt`
- `data/database/dao/result/TransactionAggregateResults.kt`
- `data/database/entity/BudgetEntity.kt`
- `data/database/entity/BudgetHistoryEntity.kt`
- `data/database/dao/BudgetDao.kt`

### Phase 2
- `data/processor/ProcessResult.kt`
- `data/processor/TransactionProcessor.kt`
- `domain/service/CashbackService.kt`
- `data/repository/BudgetRepository.kt`
- `data/repository/BudgetRepositoryImpl.kt`
- `data/repository/HiddenAccountsRepository.kt`
- `data/repository/AppPreferencesRepository.kt`
- `domain/usecase/transaction/ProcessTransactionUseCase.kt`
- `domain/usecase/budget/GetBudgetWithSpendingUseCase.kt`
- `domain/usecase/cashback/ApplyRetroactiveCashbackUseCase.kt`

### Phase 3
- `presentation/common/FilterStateManager.kt`
- `presentation/common/UndoableDeleteDelegate.kt`
- `presentation/common/UiState.kt`

### Phase 4
- `ui/components/items/TransactionListItem.kt`
- `ui/components/states/EmptyStateCard.kt`
- `ui/components/dialogs/FilterDialog.kt`
- `ui/theme/Dimensions.kt`
- `presentation/home/components/AccountBalanceCard.kt`
- `presentation/home/components/TransactionSummaryCard.kt`

### Phase 7
- `ui/components/charts/SpendingTrendChart.kt`
- `ui/components/charts/BudgetComparisonChart.kt`
- `ui/components/charts/DayOfWeekChart.kt`
- `ui/components/dialogs/BudgetInputDialog.kt`
- `ui/components/budget/BudgetProgressCard.kt`
- `presentation/analytics/tabs/OverviewTab.kt`
- `presentation/analytics/tabs/BudgetTab.kt`
- `presentation/analytics/tabs/InsightsTab.kt`

---

## Files Deleted Summary

| File | Phase |
|------|-------|
| `utils/CurrencyUtils.kt` | 1 |
| `worker/SmsReaderWorker.kt` | 6 |
| Old transaction item composables | 4 |
| Old empty state composables | 4 |
| Old filter dialog composables | 4 |
| App lock code | 6 |

---

## Estimated Impact

| Metric | Before | After |
|--------|--------|-------|
| Duplicated code | ~1500-2000 lines | ~200-300 lines |
| Transaction item implementations | 4 | 1 |
| Transaction processing paths | 3 | 1 |
| Workers | 2 | 1 |
| HomeScreen lines | 2104 | ~800-1000 |
| HomeViewModel lines | 535 | ~200-250 |
| TransactionType values | 5 | 3 |

---

## Version Planning

After completing ALL phases:

**Version 2.2.0** includes:
- Complete architecture refactor
- Budget feature
- Tabbed Analytics with charts
- Simplified transaction types
- Removed: App lock, Developer options, Support/Community

---

## Reference Documents

| Document | Purpose |
|----------|---------|
| `CLAUDE.md` | Development principles |
| `architecture-refactor-analysis.md` | Technical debt analysis |
| `features-roadmap.md` | Feature requirements |
| `transaction-processing-refactor.md` | Transaction flow architecture |
