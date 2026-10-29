# Member A plan: Money In and Out

Rewritten on 2026-11-03 against `develop` at `8b713dd`. C already merged A's data contract (PR #15, see `docs/DECISIONS.md`), so this covers only what is left. Rules: Doc 1 section 8. Branch `feature/a-<topic>` off `develop`, one PR each, under 400 lines.

## Reuse, do not rebuild

| Need | Already on `develop` |
|---|---|
| Models, repositories, Firestore | `Transaction`, `Category`, `IncomeSource` with DTOs, mappers, impls and bindings |
| Creating a transaction | `AddTransaction` and `TransactionDraft` (validation, `monthKey`, LKR value) |
| Accounts, profile | `AccountRepository`, `UserProfileRepository` (`setLastFxRate`, `pyfPercent`) |
| Sheet, Undo | `RunwaySheet`, `RunwaySnackbarHost`, `showUndo` |
| Form parts | `AmountField`, `CategoryChip`, `categoryIcon`, `RunwayButton`, `RunwayTopBar` |
| Lists | `TransactionRow`, `SectionHeader`, `EmptyState`, `ErrorState`, `LoadingState` |
| Form pattern to copy | `GoalViewModel`: `SavedStateHandle`, one client id per form, event channel |

## Decisions

| Topic | Decision |
|---|---|
| Labs 04 and 05 | Explained in the TDD as Compose equivalents: hoisted form state with inline validation for Data Binding, `LazyColumn` with the Firestore offline cache for RecyclerView and Room. No Room code. |
| Quick Add | `RunwaySheet` shown by `RunwayShell`. Its ViewModel is activity scoped, so it outlives the sheet for Undo. |
| Income, freelance | Activity top bar has "Log income" plus an overflow menu with Transfer and Freelance. |
| Refund | Started from an expense's detail screen. Sets `relatedTxId`. |
| Transfer and ATM | Started from Activity. ATM is a transfer with the Cash account preselected. |
| Crypto | C built it in PR #24. Off this list unless the team says otherwise. |

## PRs, in order

Cut from the bottom when time runs out.

### 1. `a-quick-add` (F-07)
- `SuggestFavourites`: top 6 categories by use in the last 60 days, filled from `sortOrder` (BR-27), with a test.
- `ui/feature/quickadd`: sheet and `QuickAddViewModel`. Amount, category chip, Save. "More" shows the other categories. "More options" holds note, date and account.
- `RunwayShell`: open the sheet from the raised button, use `RunwaySnackbarHost`, show Undo.
- AT-02, AT-03, AT-04, AT-05, AT-06, AT-21.

### 2. `a-activity` (F-08)
- Month list grouped by day with sticky headers. Previous and next month via `MonthKey`. Client-side category filter.
- `Transaction.withAmount(minor, now)`: an edit keeps the original rate (BR-05), with a test for AT-22.
- Detail screen: edit, archive, and refund (amount no larger than the original). AT-16.
- `activityScreens(nav)` replaces the placeholder in `AppNavHost`.

### 3. `a-income` (F-09, F-15)
- Fix first: `TransactionDraft.fxRate` defaults to 1.0, so USD with no rate saves as 1 USD = 1 LKR. `AddTransaction` must reject it.
- Log income: pick a source, LKR or USD, enter the rate or the LKR received. Store the last rate with `setLastFxRate`.
- Salary turns on PYF: income plus a transfer to the goal fund, one `addAll` batch at `pyfPercent`.
- Transfer and ATM screen (BR-13).
- AT-07, AT-08, AT-15.

### 4. `a-freelance` (F-14)
- Nothing exists yet. Add `FreelanceProject`, `Milestone` (+ `isOverdue(today)`), DTOs, mappers, `FreelanceRepository` with a `markPaid` batch, binding in `RepositoryModuleA`.
- Project list, project detail, PLANNED → INVOICED → PAID. OVERDUE is derived, never stored.
- Overdue card on Home (BR-53), agreed with C because `HomeCards.kt` is C's.
- AT-13, AT-14.

### Docs (alongside the code)
- `docs/AI_USAGE.md` does not exist yet. Add it with the first PR, then one entry per PR.
- TDD chapters 2 (intro and scope), 3 (problem analysis) and 4 (SRS + use-case diagram A1), from Doc 1 sections 4 and 6.
- Eraser diagrams 06 and 07: remove Room and the outbox. Show repository → Firestore cache → listener re-emits.

## Skipped (add if time allows)
- UI for adding custom categories (FR-09). Only the seed for now.
- Snooze on the overdue card (BR-53). It needs a new field.
- Currency in Quick Add. Expenses are LKR. USD goes through Log income.

## Open
- Undo time: `showUndo` uses 6 s from the design system, BR-17 says 5 s. Reusing `showUndo` until the team picks one.
