# Decisions

Anything that changes a rule, an interface or who owns what (Doc 3, section 5). Newest last.

| Date | Decision | Why | Who |
|---|---|---|---|
| 2026-11-03 | C adds A's data contract as A's buddy (Doc 3, section 1): the `Transaction`, `Category` and `IncomeSource` models, their repository interfaces and Firestore implementations, `RepositoryModuleA` and `AddTransaction`, written exactly as Doc 2 sections 5.5 to 5.8 and 6.2 specify. A still owns them and builds Quick Add, Activity, income logging and freelance on top. | Home, Goal and the weekly check-in read `TransactionRepository` (Doc 3, section 4 dependency matrix) and no A branch existed yet. | C (Sunny) |
| 2026-11-03 | A transaction document with an unknown `type` is skipped when read, instead of defaulting to an enum value. | Guessing EXPENSE or INCOME would silently change every total. Other enums keep Doc 2's "safe default" rule. | C (Sunny) |
