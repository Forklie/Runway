package com.example.runway.domain.model

/** BR-10. A transfer is never income or expense (BR-13). A refund links to its expense (BR-14). */
enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    REFUND,
}
