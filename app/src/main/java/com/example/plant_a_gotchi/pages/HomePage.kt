package com.example.plant_a_gotchi.pages

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.plant_a_gotchi.AuthState
import com.example.plant_a_gotchi.AuthViewModel
import com.example.plant_a_gotchi.CurrentLevels
import com.example.plant_a_gotchi.R
import com.example.plant_a_gotchi.UserPlant
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val authState = authViewModel.authState.observeAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val initialLevels = remember { mutableStateOf(CurrentLevels()) } // Zmienna przechowująca początkowe wartości poziomów

    // Nasłuchiwacz na cykl życia aplikacji
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Przywrócenie początkowych poziomów przy każdym powrocie do aplikacji
                val userId = authViewModel.getUserUid()
                if (userId != null) {
                    val database =
                        FirebaseDatabase.getInstance("https://plant-a-gotchi-default-rtdb.europe-west1.firebasedatabase.app/")
                    val userPlantRef = database.getReference("users/$userId/plant/currentLevels")
                    userPlantRef.get().addOnSuccessListener { snapshot ->
                        val plantLevels = snapshot.getValue(CurrentLevels::class.java)
                        if (plantLevels != null) {
                            initialLevels.value = plantLevels
                        }
                    }
                }
            }

            if (event == Lifecycle.Event.ON_PAUSE) {
                // Zapisujemy aktualne poziomy przed wejściem do minigry
                val currentLevels = CurrentLevels(water = 100, sun = 100, fertilizer = 100) // Przykładowe aktualne poziomy
                // Porównanie zapisanych i obecnych poziomów
                if (initialLevels.value != currentLevels) {
                    // Jeśli poziomy zmieniły się, zaktualizuj Firebase
                    authViewModel.updatePlantLevel(
                        currentLevels.water,
                        currentLevels.sun,
                        currentLevels.fertilizer,
                        initialLevels.value
                    )
                }
            }
        }

        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.applyTimeEffect() // Zastosuj efekt czasu na start
    }

    val userUid = authViewModel.getUserUid()

    if(userUid != null) {
        val firebaseDatabase =
            FirebaseDatabase.getInstance("https://plant-a-gotchi-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference()
        FirebaseUI(LocalContext.current, firebaseDatabase, authViewModel, navController)
    } else {
        Toast.makeText(LocalContext.current, "Failed to get user UID", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun FirebaseUI(context: Context, databaseReference: DatabaseReference, authViewModel: AuthViewModel, navController: NavController) {

    val database =
        FirebaseDatabase.getInstance("https://plant-a-gotchi-default-rtdb.europe-west1.firebasedatabase.app/")
    val plantData = remember { mutableStateOf(UserPlant()) }
    val userId = authViewModel.getUserUid()
    val isDataChanged = remember { mutableStateOf(false) }
    val initialLevels = remember { CurrentLevels() } // Inicjalizacja zmiennej przechowującej początkowe wartości
    val plantImageResId = authViewModel.getPlantImageResource(context, plantData.value.plantType, plantData.value.plantLevel)

    LaunchedEffect(userId) {
        if (userId != null) {
            val userPlantRef = database.getReference("users/$userId/plant")
            userPlantRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val plant = snapshot.getValue(UserPlant::class.java)
                    if (plant != null) {
                        // Sprawdzamy, czy dane w lokalnym stanie są różne od danych w Firebase
                        if (plantData.value != plant) {
                            plantData.value = plant
                            authViewModel.updatePlantLevel(
                                plantData.value.currentLevels.water,
                                plantData.value.currentLevels.sun,
                                plantData.value.currentLevels.fertilizer,
                                initialLevels = initialLevels // Przekazujemy initialLevels
                            )
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to read plant data", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    // Nasłuchuj na zmiany w stanie aplikacji i zapisz dane w Firebase tylko, gdy dane zostały zmienione
    LaunchedEffect(plantData.value) {
        if (userId != null && isDataChanged.value) {
            val userPlantRef = database.getReference("users/$userId/plant")
            userPlantRef.setValue(plantData.value) // Zaktualizuj dane w Firebase tylko wtedy, gdy dane zostały zmienione
                .addOnSuccessListener {
                    Log.d("Firebase", "Plant data updated successfully.")
                }
                .addOnFailureListener {
                    Log.e("Firebase", "Failed to update plant data", it)
                }

            // Po zapisaniu danych ustaw flagę isDataChanged na false
            isDataChanged.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize() // Wypełnia cały dostępny obszar
    ) {

        Image(
            painter = painterResource(id = R.drawable.background), // Zmień na swój plik
            contentDescription = null, // Brak opisu (jeśli nie wymagane)
            contentScale = ContentScale.Crop, // Dopasowanie obrazu
            modifier = Modifier.matchParentSize() // Obraz wypełnia cały kontener
        )



        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.Unspecified),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = plantData.value.plantName,
                    fontFamily = FontFamily(Font(R.font.pacifico)),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 16.dp)
                )
                CustomRoundedButtonWithDoubleBorder(
                    icon = painterResource(R.drawable.exit),
                    onClick = {authViewModel.signout()}
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Plant animation placeholder
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (plantImageResId != 0) {
                    Image(
                        painter = painterResource(id = plantImageResId),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.matchParentSize().background(Color.Transparent)
                    )
                } else {
                    // Jeśli obrazek nie istnieje, ustaw domyślny obrazek
                    Image(
                        painter = painterResource(id = R.drawable.sunflower_10),
                        contentDescription = "Default plant image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.matchParentSize().background(Color.Transparent)
                    )
                    Log.e("PlantImage", "Resource not found for: ${plantData.value.plantType}_${plantData.value.plantLevel}")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))


            // Progress bars
            NeedsProgressBar(
                label = "Water",
                icon = painterResource(R.drawable.waterdrop),
                value = plantData.value.currentLevels.water,
                min = plantData.value.thresholds.waterMin,
                max = plantData.value.thresholds.waterMax,
                onClick = { navController.navigate("water") }
            )
            NeedsProgressBar(
                label = "Sun",
                icon = painterResource(R.drawable.sun),
                value = plantData.value.currentLevels.sun,
                min = plantData.value.thresholds.sunMin,
                max = plantData.value.thresholds.sunMax,
                onClick = { navController.navigate("sun") }
            )
            NeedsProgressBar(
                label = "Fertilizer",
                icon = painterResource(R.drawable.fertilizer),
                value = plantData.value.currentLevels.fertilizer,
                min = plantData.value.thresholds.fertilizerMin,
                max = plantData.value.thresholds.fertilizerMax,
                onClick = { navController.navigate("fertilizer") }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // XP bar
            Text(
                text = "LVL ${plantData.value.plantLevel}",
                fontFamily = FontFamily(Font(R.font.chewy)),
                style = MaterialTheme.typography.displaySmall
            )
            ProgressBarWithLabel(label = "Level", value = plantData.value.plantHappiness / 100f)
            Text(
                text = "${plantData.value.plantHappiness} / 100 HP",
                fontFamily = FontFamily(Font(R.font.chewy)),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CustomRoundedButtonWithDoubleBorder(
    icon: Painter, // Ikona w środku przycisku
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp) // Ustaw rozmiar przycisku
            .background(Color(0xFFa67c52), RoundedCornerShape(12.dp)) // Ciemna wewnętrzna ramka
            .padding(3.5.dp) // Odstęp na jasną ramkę
            .background(Color(0xFF8b5e3c), RoundedCornerShape(11.dp)) // Jasna zewnętrzna ramka
            .padding(2.5.dp) // Odstęp na tło
            .background(Color(0xFFf1efe7), RoundedCornerShape(10.dp)) // Jasnobeżowe tło
            .clip(RoundedCornerShape(16.dp)) // Wycinanie do zaokrąglonych rogów
            .clickable(onClick = onClick), // Obsługa kliknięcia
        contentAlignment = Alignment.Center // Wyśrodkowanie zawartości
    ) {
        Icon(
            painter = icon,
            contentDescription = "Icon",
            tint = Color.Unspecified,
            modifier = Modifier.size(36.dp) // Rozmiar ikony
        )
    }
}




@Composable
fun ProgressBarWithLabel(label: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(Color(0xFFf1efe7))
            .border(BorderStroke(3.5.dp, Color(0xFFa67c52)))
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = value,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                color = Color(0xFFC16252)
            )
        }
    }
}

@Composable
fun NeedsProgressBar(
    label: String,
    icon: Painter,
    value: Int, // Wartość postępu (od 0 do 100)
    min: Int,   // Minimalna wartość dla zakresu żółtego
    max: Int,   // Maksymalna wartość dla zakresu żółtego
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomRoundedButtonWithDoubleBorder(
            icon = icon,
            onClick = onClick,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontFamily = FontFamily(Font(R.font.pacifico)), style = MaterialTheme.typography.bodyMedium)

            // Wskaźnik postępu z kolorami i markerami
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color(0xFFf1efe7))
                    .border(BorderStroke(3.5.dp, Color(0xFFa67c52)))
            ) {
                // Kolor wskaźnika
                val progressColor = when {
                    value <= min -> Color(0xFFd88a7e)
                    value in (min + 1)..max -> Color(0xFFf5d49a)
                    else -> Color(0xFFa9d89e)
                }
                // Wypełniony wskaźnik
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((value / 100f))
                        .background(progressColor)
                )
                // Markery min i max
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val totalWidth = size.width
                    val minPosition = totalWidth * (min.toFloat() / 100)
                    val maxPosition = totalWidth * (max.toFloat() / 100)
                    // Marker dla min
                    drawLine(
                        color = Color(0xFF8b5e3c),
                        start = Offset(x = minPosition, y = 0f),
                        end = Offset(x = minPosition, y = size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                    // Marker dla max
                    drawLine(
                        color = Color(0xFF8b5e3c),
                        start = Offset(x = maxPosition, y = 0f),
                        end = Offset(x = maxPosition, y = size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }
    }


}
