package com.example.smartexpensetracker.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.smartexpensetracker.model.Expense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ExpenseRepository {
    private const val DATASTORE_NAME = "expenses_ds"
    private val EXPENSES_KEY = stringPreferencesKey("expenses_json")

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var dataStore: DataStore<Preferences>

    private val _expenses: MutableStateFlow<List<Expense>> = MutableStateFlow(emptyList())
    fun getExpenses(): StateFlow<List<Expense>> = _expenses

    private val _isOnline: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    fun setOnline(online: Boolean) {
        _isOnline.value = online
        // Mock sync: no-op for now
    }

    fun initialize(context: Context) {
        if (::dataStore.isInitialized) return
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = repositoryScope
        ) { context.preferencesDataStoreFile(DATASTORE_NAME) }

        repositoryScope.launch {
            val prefs = try { dataStore.data.first() } catch (e: Exception) { emptyPreferences() }
            val json = prefs[EXPENSES_KEY].orEmpty()
            _expenses.value = decodeExpenses(json)
        }
    }

    fun clear() {
        _expenses.value = emptyList()
        repositoryScope.launch { persist() }
    }

    fun addExpense(expense: Expense): Boolean {
        val current = _expenses.value
        if (isDuplicate(current, expense)) {
            return false
        }
        val updated = current + expense
        _expenses.value = updated
        repositoryScope.launch { persist() }
        return true
    }

    fun deleteExpense(id: UUID): Boolean {
        val current = _expenses.value
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return false
        _expenses.value = updated
        repositoryScope.launch { persist() }
        return true
    }

    private suspend fun persist() {
        val json = encodeExpenses(_expenses.value)
        dataStore.edit { it[EXPENSES_KEY] = json }
    }

    private fun isDuplicate(existing: List<Expense>, candidate: Expense): Boolean {
        val candidateDay = startOfDay(candidate.date)
        return existing.any { e ->
            startOfDay(e.date) == candidateDay &&
            e.title.trim().equals(candidate.title.trim(), ignoreCase = true) &&
            e.category == candidate.category &&
            kotlin.math.abs(e.amount - candidate.amount) < 0.0001 &&
            ((e.receiptUri ?: "") == (candidate.receiptUri ?: ""))
        }
    }

    private fun startOfDay(timeMillis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun encodeExpenses(list: List<Expense>): String {
        val arr = JSONArray()
        list.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id.toString())
            obj.put("title", e.title)
            obj.put("amount", e.amount)
            obj.put("category", e.category)
            obj.put("notes", e.notes)
            obj.put("date", e.date)
            obj.put("receiptUri", e.receiptUri)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeExpenses(json: String): List<Expense> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Expense(
                            id = runCatching { UUID.fromString(o.optString("id")) }.getOrDefault(UUID.randomUUID()),
                            title = o.optString("title"),
                            amount = o.optDouble("amount", 0.0),
                            category = o.optString("category"),
                            notes = o.optString("notes"),
                            date = o.optLong("date", System.currentTimeMillis()),
                            receiptUri = o.optString("receiptUri").ifBlank { null }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}