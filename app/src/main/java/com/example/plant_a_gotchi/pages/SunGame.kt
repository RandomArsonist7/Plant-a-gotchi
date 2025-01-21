package com.example.plant_a_gotchi.pages

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.plant_a_gotchi.AuthViewModel
import com.example.plant_a_gotchi.R

@Composable
fun SunGame(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var lightLevel by remember { mutableStateOf(0f) }  // Poziom oświetlenia 0.0 - 1.0

    val cloud1StartOffset = 500f // Początkowa pozycja chmury (im większa, tym bardziej na prawo)
    val cloud1EndOffset = 130f // Końcowa pozycja chmury (im mniejsza, tym bliżej środka)
    val cloud1Offset by animateFloatAsState(
        targetValue = (lightLevel/2) * (cloud1StartOffset - cloud1EndOffset) + cloud1EndOffset,  // Przesunięcie chmur
        animationSpec = tween(durationMillis = 1000) // Czas animacji
    )

    val cloud2StartOffset = -400f  // Chmura zaczyna po lewej stronie
    val cloud2EndOffset = -20f   // Kończy bliżej środka
    val cloud2Offset by animateFloatAsState(
        targetValue = (lightLevel / 2) * (cloud2StartOffset - cloud2EndOffset) + cloud2EndOffset,  // Przesunięcie chmur
        animationSpec = tween(durationMillis = 1000) // Czas animacji
    )

    var timeLeft by remember { mutableStateOf(10) } // 10-sekundowy timer
    var gameOver by remember { mutableStateOf(false) } // Flaga zakończenia gry
    var sunToAdd by remember { mutableStateOf(0) }

    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val maxLightLevel = lightSensor?.maximumRange // Maksymalna wartość czujnika
                    val currentLight = event.values[0] // Aktualny poziom światła
                    val normalizedLight = (currentLight / maxLightLevel!!).coerceIn(0f, 1f) // Normalizacja do 0.0 - 1.0

                    lightLevel = normalizedLight // Aktualizacja poziomu światła
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (lightSensor != null) {
            sensorManager.registerListener(
                sensorListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
            // Obsługa braku czujnika
            lightLevel = 0f
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener) // Usuń nasłuch po zakończeniu
        }
    }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            kotlinx.coroutines.delay(1000) // Opóźnienie 1 sekundy
            timeLeft--
        } else if (!gameOver) {
            gameOver = true
            sunToAdd = ((lightLevel.coerceIn(0f, 5f) / 5f) * 50).toInt() // Mapowanie

            authViewModel.updateSunLevel(addedSun = sunToAdd) // Aktualizacja w ViewModel
            kotlinx.coroutines.delay(2000) // Wyświetlenie gry przez 2 sekundy po zakończeniu
            navController.popBackStack() // Powrót do ekranu głównego
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF87CEEB))) {

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Time left: $timeLeft s",
            fontFamily = FontFamily(Font(R.font.pacifico)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
        )

    Box(modifier = Modifier.fillMaxSize()) {

        // Słońce
        Image(
            painter = painterResource(id = R.drawable.sun),
            contentDescription = "Sun",
            modifier = Modifier
                .align(Alignment.Center)
                .size(300.dp)
        )
        // Chmury
        Image(
            painter = painterResource(id = R.drawable.cloud),
            contentDescription = "Cloud1",
            modifier = Modifier
                .offset(x = cloud1Offset.dp, y = 220.dp)  // Przesunięcie chmur
                .size(300.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.cloud),
            contentDescription = "Cloud2",
            modifier = Modifier
                .offset(x = cloud2Offset.dp, y = 320.dp)  // Przesunięcie drugiej chmury
                .size(300.dp)
        )
        }
    }
}