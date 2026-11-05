package com.example.runway.domain.model

enum class IncomeSourceKind { SALARY, FREELANCE, ADSENSE, CRYPTO, OTHER }

/** RELIABLE (salary base) is planned on before it arrives; VARIABLE only once received (BR-40, BR-41). */
enum class Reliability { RELIABLE, VARIABLE }

/**
 * users/{uid}/incomeSources/{id}. Income has four attributes (source, currency, frequency, certainty),
 * so each source is modelled on its own instead of one flat "monthly income" field.
 */
data class IncomeSource(
    val id: String,
    val name: String,
    val kind: IncomeSourceKind,
    val currency: CurrencyCode,
    val reliability: Reliability,
    val expectedAmountMinor: Long,       // salary base (BR-40); 0 when unknown
    val payDay: Int?,                    // 25 for salary
    val ownMonthlyEstimateMinor: Long?,  // his own guess, shown against reality (BR-44)
)
