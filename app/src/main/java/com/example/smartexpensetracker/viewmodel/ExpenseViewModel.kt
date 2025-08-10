package com.example.smartexpensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpensetracker.model.Expense
import com.example.smartexpensetracker.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class ExpenseViewModel : ViewModel() {
    private val _selectedDate = MutableStateFlow(today())
    val selectedDate: StateFlow<Long> = _selectedDate

    val expenses: StateFlow<List<Expense>> = ExpenseRepository.getExpenses()
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5_000), emptyList())

    val isOnline: StateFlow<Boolean> = ExpenseRepository.isOnline
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5_000), false)

    val todayTotal: StateFlow<Double> = expenses
        .map { list -> list.filter { isSameDay(it.date, today()) }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(), 0.0)

    fun addExpense(expense: Expense, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val added = ExpenseRepository.addExpense(expense)
            onResult(added)
        }
    }

    fun deleteExpense(id: UUID, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val deleted = ExpenseRepository.deleteExpense(id)
            onResult(deleted)
        }
    }

    fun toggleOnline() {
        ExpenseRepository.setOnline(!isOnline.value)
    }

    fun setOnline(online: Boolean) {
        ExpenseRepository.setOnline(online)
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
    }

    fun expensesForDate(date: Long): StateFlow<List<Expense>> =
        expenses.map { list -> list.filter { isSameDay(it.date, date) } }
            .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(), emptyList())

    fun expensesForLast7Days(): StateFlow<List<Expense>> =
        expenses.map { list ->
            val end = endOfToday()
            val weekAgo = startOfDay(end - 6 * 24 * 60 * 60 * 1000)
            list.filter { it.date in weekAgo..end }
        }.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(), emptyList())

    companion object {
        fun today(): Long = startOfDay(System.currentTimeMillis())

        private fun startOfDay(timeMillis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timeMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun endOfToday(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

        fun isSameDay(date1: Long, date2: Long): Boolean {
            val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            return fmt.format(Date(date1)) == fmt.format(Date(date2))
        }
    }
}