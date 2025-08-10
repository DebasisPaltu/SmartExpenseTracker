package com.example.smartexpensetracker.screen

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smartexpensetracker.model.Expense
import com.example.smartexpensetracker.viewmodel.ExpenseViewModel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import kotlinx.coroutines.delay
import com.example.smartexpensetracker.ui.components.AppTextField
import com.example.smartexpensetracker.ui.components.PrimaryButton
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.AddCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(vm: ExpenseViewModel, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    BackHandler {
        activity?.finish()
    }

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var notes by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    // Use OpenDocument to allow persistable URI permission so the image remains accessible on restart
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* ignore if already persisted */ }
            receiptUri = uri.toString()
        }
    }

    val categories = listOf("Food", "Staff", "Travel", "Utility")
    val todayTotal by vm.todayTotal.collectAsState()

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1200)
            showSuccess = false
        }
    }

    Column(Modifier.padding(16.dp)) {
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.padding(12.dp)) {
                    Icon(Icons.Filled.Done, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Expense added successfully", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Today's Total Spent", color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "₹" + "%.0f".format(todayTotal),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        ElevatedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(2.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Add New Expense", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                Text("Title *", style = MaterialTheme.typography.labelMedium)
                AppTextField(
                    value = title,
                    onValueChange = { newValue -> title = newValue },
                    placeholderText = "Enter expense title",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Text("Amount (₹) *", style = MaterialTheme.typography.labelMedium)
                AppTextField(
                    value = amount,
                    onValueChange = { newValue -> amount = newValue },
                    placeholderText = "₹ 0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Text("Category", style = MaterialTheme.typography.labelMedium)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    AppTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    category = option
                                    expanded = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text("Notes (Optional)", style = MaterialTheme.typography.labelMedium)
                AppTextField(
                    value = notes,
                    onValueChange = { newValue -> if (newValue.length <= 100) notes = newValue },
                    placeholderText = "Additional notes...",
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${notes.length}/100") }
                )
                Spacer(Modifier.height(12.dp))

                Text("Receipt Image (Optional)", style = MaterialTheme.typography.labelMedium)
                OutlinedButton(onClick = { pickImage.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (receiptUri != null) "Change Receipt" else "Upload Receipt")
                }

                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = "Add Expense",
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (title.isBlank() || amt <= 0) {
                            Toast.makeText(context, "Enter valid title and amount", Toast.LENGTH_SHORT).show()
                        } else {
                            vm.addExpense(
                                Expense(
                                    title = title,
                                    amount = amt,
                                    category = category,
                                    notes = notes,
                                    receiptUri = receiptUri
                                )
                            ) { added ->
                                if (added) {
                                    Toast.makeText(context, "Expense Added!", Toast.LENGTH_SHORT).show()
                                    showSuccess = true
                                    title = ""
                                    amount = ""
                                    notes = ""
                                    receiptUri = null
                                } else {
                                    Toast.makeText(context, "Duplicate expense ignored", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
} 