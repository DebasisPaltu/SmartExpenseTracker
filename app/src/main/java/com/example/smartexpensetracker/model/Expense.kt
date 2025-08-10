package com.example.smartexpensetracker.model

import java.util.*

data class Expense(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val amount: Double,
    val category: String,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),
    val receiptUri: String? = null
)
data class UiState(
    val expenses: List<Expense> = emptyList(),
    val totalToday: Double = 0.0,
    val loading: Boolean = false
)
