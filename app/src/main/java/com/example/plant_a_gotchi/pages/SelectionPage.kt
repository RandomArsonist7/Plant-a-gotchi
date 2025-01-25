package com.example.plant_a_gotchi.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.shape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.plant_a_gotchi.AuthViewModel
import com.example.plant_a_gotchi.CurrentLevels
import com.example.plant_a_gotchi.R
import com.example.plant_a_gotchi.Thresholds
import com.example.plant_a_gotchi.UserPlant
import com.example.plant_a_gotchi.ui.theme.PlantagotchiTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun SelectionPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    // Lista dostępnych roślin
    val plants = listOf(
        UserPlant("sunflower", "Sunny", R.drawable.sunflowericon, R.drawable.foot, Thresholds(30,70,30,70,30,70), CurrentLevels(50,50,50), 1, 1),
        UserPlant("cactus", "Spiky", R.drawable.logo, R.drawable.foot, Thresholds(20,30,0,0,0,0, ), CurrentLevels(), 1, 1),
        UserPlant("aloe", "Vera", R.drawable.logo, R.drawable.foot, Thresholds(0,0,0,0,0,0,), CurrentLevels(), 1, 1),
        UserPlant("monstera", "Missy", R.drawable.logo, R.drawable.foot, Thresholds(0,0,0,0,0,0,), CurrentLevels(), 1, 1),
        UserPlant("rose", "Regina", R.drawable.logo, R.drawable.foot, Thresholds(0,0,0,0,0,0,), CurrentLevels(), 1, 1),
        UserPlant("lilly", "Lillian", R.drawable.logo, R.drawable.foot, Thresholds(0,0,0,0,0,0,), CurrentLevels(), 1, 1),
        UserPlant("sansevieria", "Sansa", R.drawable.logo, R.drawable.foot, Thresholds(0,0,0,0,0,0,), CurrentLevels(), 1, 1),
        UserPlant("snapdragon", "Snappy", R.drawable.logo, R.drawable.foot, Thresholds(0,0,0,0,0,0,), CurrentLevels(), 1, 1)

    )

    var selectedPlant by remember { mutableStateOf<UserPlant?>(null) }
    var selectedPlantIndex by remember { mutableStateOf(-1) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(52.dp))
        // Tytuł ekranu
        Text(text = "Wybierz swoją roślinkę", modifier = Modifier.padding(bottom = 0.dp))

        // Siatka roślin
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
            items(plants.size) { index ->
                val plant = plants[index]
                val isSelected = index == selectedPlantIndex

                CustomPreview(icon = painterResource(id = plant.image), isSelected = isSelected,
                    onClick = {
                        selectedPlantIndex = index
                        selectedPlant = plant
                        Toast.makeText(context, "selected ${plant.plantType}!", Toast.LENGTH_SHORT).show()
                    })
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Przycisk do przejścia do kolejnego ekranu
        CustomRoundedButtonWithDoubleBorder(
            icon = painterResource(R.drawable.baseline_check_circle_outline_24),
            onClick = {
            val userId = authViewModel.getUserUid() // Pobranie UID użytkownika
            if (userId != null) {
                val plant = UserPlant(
                    plantName = selectedPlant!!.plantName,
                    plantType = selectedPlant!!.plantType,
                    image = selectedPlant!!.image,
                    animation = selectedPlant!!.animation,
                    thresholds = selectedPlant!!.thresholds,
                    currentLevels = selectedPlant!!.currentLevels,
                    plantLevel = selectedPlant!!.plantLevel
                )
                val database = FirebaseDatabase.getInstance("https://plant-a-gotchi-default-rtdb.europe-west1.firebasedatabase.app/")
                val plantRef = database.reference.child("users").child(userId).child("plant")
                navController.navigate("home")

                // Zapisujemy dane o roślinie w Firebase
                plantRef.setValue(plant)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Plant data added successfully", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to add plant data", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(context, "User not authenticated", Toast.LENGTH_LONG).show()
            }
        })
    }
}

@Composable
fun CustomPreview(
    icon: Painter,
    isSelected: Boolean,// Ikona w środku przycisku
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(10.dp)
            .size(140.dp)
            .background(
                color = if (isSelected) Color.White else Color.Transparent, // Ramka, jeśli wybrana
                shape = RoundedCornerShape(16.dp)
            )
            .padding(if (isSelected) 4.dp else 0.dp)// Ustaw rozmiar przycisku
            .background(Color(0xFFa67c52), RoundedCornerShape(12.dp)) // Ciemna wewnętrzna ramka
            .padding(10.dp) // Odstęp na jasną ramkę
            .background(Color(0xFF8b5e3c), RoundedCornerShape(11.dp)) // Jasna zewnętrzna ramka
            .padding(8.dp) // Odstęp na tło
            .background(Color(0xFFf1efe7), RoundedCornerShape(10.dp)) // Jasnobeżowe tło
            .clip(RoundedCornerShape(16.dp)) // Wycinanie do zaokrąglonych rogów
            .clickable(onClick = onClick), // Obsługa kliknięcia
        contentAlignment = Alignment.Center // Wyśrodkowanie zawartości
    ) {
        Icon(
            painter = icon,
            contentDescription = "Icon",
            tint = Color.Unspecified,
            modifier = Modifier.size(90.dp) // Rozmiar ikony
        )
    }
}

