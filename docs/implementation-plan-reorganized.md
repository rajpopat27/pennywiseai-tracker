# Implementation Plan - Reorganized

## Correct Order of Implementation

**Principle:** Architecture and code quality FIRST, then bug fixes, then removals, then new features.

```
┌─────────────────────────────────────────────────────────────────┐
│                    PHASE 1: DATA LAYER                          │
│  Entities, DAOs, Repositories, Database Indexes, Views          │
│  (Foundation that everything else builds on)                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PHASE 2: DOMAIN LAYER                        │
│  Use Cases, TransactionProcessor, CashbackService               │
│  (Business logic centralization)                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  PHASE 3: PRESENTATION LAYER                    │
│  ViewModels, FilterStateManager, UndoableDeleteDelegate         │
│  (State management patterns)                                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     PHASE 4: UI LAYER                           │
│  Consolidated components, EmptyStateCard, TransactionListItem   │
│  (Reusable UI components)                                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     PHASE 5: BUG FIXES                          │
│  Memory leak, cashback bug, alignment issues                    │
│  (Fix issues with clean architecture)                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PHASE 6: FEATURE REMOVALS                     │
│  App lock, developer options, support, transaction types        │
│  (Simplify the app)                                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PHASE 7: NEW FEATURES                        │
│  Budget feature, Tabbed Analytics, Charts                       │
│  (Build on clean architecture)                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Refactoring Items Coverage

All items from `architecture-refactor-analysis.md` are now covered:

| Issue | Phase | Status |
|-------|-------|--------|
| **CRITICAL** | | |
| Memory leak HomeViewModel | Phase 5 | ✅ |
| Transaction processing duplication | Phase 2 | ✅ |
| Duplicate workers | Phase 6 | ✅ |
| **DATA LAYER** | | |
| Duplicate DAO queries | Phase 1 | ✅ |
| Inconsistent Flow vs Suspend | Phase 1 | ✅ |
| Missing database indexes | Phase 1 | ✅ |
| Duplicate monthly breakdown | Phase 1 | ✅ |
| Cashback fragmentation | Phase 2 | ✅ |
| Missing FK constraints | Phase 1 | ✅ (optional) |
| **VIEWMODEL LAYER** | | |
| Filter state duplication | Phase 3 | ✅ |
| HomeViewModel too large | Phase 3 | ✅ |
| SharedPreferences in ViewModels | Phase 3 | ✅ |
| Business logic in ViewModels | Phase 2 | ✅ |
| Delete with undo duplication | Phase 3 | ✅ |
| Inconsistent loading/error | Phase 3 | ✅ |
| **UI LAYER** | | |
| Transaction item x4 | Phase 4 | ✅ |
| Empty state x3 | Phase 4 | ✅ |
| Filter dialogs x4 | Phase 4 | ✅ |
| Inconsistent Card usage | Phase 4 | ✅ |
| Hardcoded dimensions | Phase 4 | ✅ |
| HomeScreen too large | Phase 4 | ✅ |
| **DOMAIN LAYER** | | |
| Missing use cases | Phase 2 | ✅ |
| ViewModels calling repos directly | Phase 2 | ✅ |
| **UTILITIES** | | |
| Duplicate currency utils | Phase 1 | ✅ |
| Currency sorting duplication | Phase 1 | ✅ |
| **ENTITIES** | | |
| Currency default duplication | Phase 1 | ✅ |
| Timestamp inconsistency | Phase 1 | ✅ |

---

## Feature Requests Considered

While refactoring, these upcoming features inform architectural decisions:

| Feature | Impact on Architecture |
|---------|----------------------|
| Transaction types (3 only) | Affects TransactionType enum, parsers |
| Budget feature | Need BudgetEntity, BudgetRepository in Phase 1-2 |
| Tabbed Analytics | FilterStateManager shared across tabs |
| Spending trend chart | Need aggregation queries in DAO/Repository |
| Cashback display | CashbackService handles all calculations |
| Day-of-week analytics | Need aggregation queries |
| Budget history | Need BudgetHistoryEntity |

---

## Phase Documents

| Phase | Document |
|-------|----------|
| 1 | `implementation-phase1-data-layer.md` |
| 2 | `implementation-phase2-domain-layer.md` |
| 3 | `implementation-phase3-presentation-layer.md` |
| 4 | `implementation-phase4-ui-layer.md` |
| 5 | `implementation-phase5-bug-fixes.md` |
| 6 | `implementation-phase6-removals.md` |
| 7 | `implementation-phase7-new-features.md` |

---

## Why This Order?

1. **Data Layer First**: Entities and DAOs are the foundation. Adding indexes and proper queries here makes everything faster. Creating proper repository patterns sets up for domain layer.

2. **Domain Layer Second**: Business logic should be in use cases, not ViewModels. TransactionProcessor unifies all transaction flows. This is the "brain" of the app.

3. **Presentation Layer Third**: With clean domain layer, ViewModels become thin. Shared state patterns (FilterStateManager) reduce duplication.

4. **UI Layer Fourth**: With clean presentation layer, we can create reusable components that work with proper state.

5. **Bug Fixes Fifth**: With clean architecture, bugs are easier to fix and less likely to reintroduce.

6. **Removals Sixth**: Remove unused features from the now-clean codebase.

7. **New Features Last**: Build new features on top of the clean, tested architecture.
