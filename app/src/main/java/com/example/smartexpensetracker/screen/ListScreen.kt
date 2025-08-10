package com.example.smartexpensetracker.screen

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.smartexpensetracker.model.Expense
import com.example.smartexpensetracker.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement as ComposeArrangement
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import android.widget.Toast
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class GroupBy { None, Category, Time }

@Composable
fun ListScreen(vm: ExpenseViewModel, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val selectedDate by vm.selectedDate.collectAsState()
    val baseExpenses by vm.expensesForDate(selectedDate).collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var groupBy by remember { mutableStateOf(GroupBy.None) }

    val filtered = remember(baseExpenses, searchQuery) {
        if (searchQuery.isBlank()) baseExpenses else baseExpenses.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalCount = filtered.size
    val totalAmount = filtered.sumOf { it.amount }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = ComposeArrangement.spacedBy(12.dp)) {
            StatCard(modifier = Modifier.weight(1f), title = "Total Expenses", value = totalCount.toString())
            StatCard(modifier = Modifier.weight(1f), title = "Total Amount", value = "₹" + "%.0f".format(totalAmount), primary = true)
        }

        Spacer(Modifier.height(16.dp))

        ElevatedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(2.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Filters", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                Text("Select Date", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = formatDate(selectedDate),
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val c = Calendar.getInstance()
                            c.set(Calendar.YEAR, y)
                            c.set(Calendar.MONTH, m)
                            c.set(Calendar.DAY_OF_MONTH, d)
                            c.set(Calendar.HOUR_OF_DAY, 0)
                            c.set(Calendar.MINUTE, 0)
                            c.set(Calendar.SECOND, 0)
                            c.set(Calendar.MILLISECOND, 0)
                            vm.setSelectedDate(c.timeInMillis)
                        },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) { Text("Pick Date") }

                Spacer(Modifier.height(12.dp))
                Text("Search", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title or category...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text("Group By", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = ComposeArrangement.spacedBy(8.dp)) {
                    Chip("None", selected = groupBy == GroupBy.None) { groupBy = GroupBy.None }
                    Chip("Category", selected = groupBy == GroupBy.Category) { groupBy = GroupBy.Category }
                    Chip("Time", selected = groupBy == GroupBy.Time) { groupBy = GroupBy.Time }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (filtered.isEmpty()) {
            Text("No expenses found.", style = MaterialTheme.typography.headlineLarge)
        } else {
            when (groupBy) {
                GroupBy.None -> Box(Modifier.weight(1f)) { ExpenseList(items = filtered, onDelete = { id ->
                    vm.deleteExpense(id) { ok ->
                        Toast.makeText(context, if (ok) "Deleted" else "Not found", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxSize()) }
                GroupBy.Category -> Box(Modifier.weight(1f)) { GroupedByCategoryList(items = filtered, onDelete = { id ->
                    vm.deleteExpense(id) { ok ->
                        Toast.makeText(context, if (ok) "Deleted" else "Not found", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxSize()) }
                GroupBy.Time -> Box(Modifier.weight(1f)) { GroupedByTimeList(items = filtered, onDelete = { id ->
                    vm.deleteExpense(id) { ok ->
                        Toast.makeText(context, if (ok) "Deleted" else "Not found", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.fillMaxSize()) }
            }
        }

        // Removed bottom "View Report" button as requested
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, title: String, value: String, primary: Boolean = false) {
    Card(
        modifier = modifier,
        colors = if (primary) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        else CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpenseList(items: List<Expense>, onDelete: (UUID) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = ComposeArrangement.spacedBy(8.dp)) {
        items(items, key = { it.id }) { exp -> ExpenseItem(exp, onDelete) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedByCategoryList(items: List<Expense>, onDelete: (UUID) -> Unit, modifier: Modifier = Modifier) {
    val grouped = items.groupBy { it.category }
    LazyColumn(modifier = modifier, verticalArrangement = ComposeArrangement.spacedBy(8.dp)) {
        grouped.forEach { (cat, list) ->
            item { Text(cat, style = MaterialTheme.typography.titleMedium) }
            items(list, key = { it.id }) { exp -> ExpenseItem(exp, onDelete) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedByTimeList(items: List<Expense>, onDelete: (UUID) -> Unit, modifier: Modifier = Modifier) {
    val grouped = items.groupBy { SimpleDateFormat("hh a", Locale.getDefault()).format(Date(it.date)) }
    LazyColumn(modifier = modifier, verticalArrangement = ComposeArrangement.spacedBy(8.dp)) {
        grouped.forEach { (time, list) ->
            item { Text(time, style = MaterialTheme.typography.titleMedium) }
            items(list, key = { it.id }) { exp -> ExpenseItem(exp, onDelete) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onDelete: (UUID) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (expense.receiptUri != null) {
                ReceiptImage(
                    uriString = expense.receiptUri,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("${expense.title} - ₹%.2f".format(expense.amount), style = MaterialTheme.typography.titleMedium)
                Text("Category: ${expense.category}", style = MaterialTheme.typography.bodyMedium)
                if (expense.notes.isNotBlank()) Text("Notes: ${expense.notes}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Time: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(expense.date))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { onDelete(expense.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun ReceiptImage(uriString: String, contentScale: ContentScale, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uriString) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = false
                    }
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Receipt image",
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis)) 