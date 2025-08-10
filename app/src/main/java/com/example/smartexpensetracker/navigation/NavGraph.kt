package com.example.smartexpensetracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.smartexpensetracker.screen.EntryScreen
import com.example.smartexpensetracker.screen.ListScreen
import com.example.smartexpensetracker.screen.ReportScreen
import com.example.smartexpensetracker.viewmodel.ExpenseViewModel

@Composable
fun NavGraph(navController: NavHostController, vm: ExpenseViewModel) {
    NavHost(navController = navController, startDestination = "entry") {
        composable("entry") {
            EntryScreen(
                vm,
                onNavigate = { route -> navController.navigate(route) })
        }
        composable("list") {
            ListScreen(
                vm,
                onNavigate = { route -> navController.navigate(route) })
        }
        composable("report") {
            ReportScreen(
                vm,
                onNavigate = { route -> navController.navigate(route) })
        }
    }
}