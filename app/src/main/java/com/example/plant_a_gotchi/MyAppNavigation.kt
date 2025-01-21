package com.example.plant_a_gotchi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.plant_a_gotchi.pages.FertilizerGame
import com.example.plant_a_gotchi.pages.HomePage
import com.example.plant_a_gotchi.pages.LoginPage
import com.example.plant_a_gotchi.pages.SignupPage
import com.example.plant_a_gotchi.pages.SelectionPage
import com.example.plant_a_gotchi.pages.SunGame
import com.example.plant_a_gotchi.pages.WaterGame


@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login", builder = {
        composable("login"){
            LoginPage(modifier, navController, authViewModel)
        }
        composable("signup"){
            SignupPage(modifier, navController, authViewModel)
        }
        composable("home"){
            HomePage(modifier, navController, authViewModel)
        }
        composable("select"){
            SelectionPage(modifier, navController, authViewModel)
        }
        composable("water"){
            WaterGame(modifier, navController, authViewModel)
        }
        composable("sun"){
            SunGame(modifier, navController, authViewModel)
        }
        composable("fertilizer"){
            FertilizerGame(modifier, navController, authViewModel)
        }

    })

}