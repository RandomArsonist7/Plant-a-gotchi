package com.example.plant_a_gotchi.pages

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.plant_a_gotchi.AuthViewModel
import android.app.Application
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.times
import com.example.plant_a_gotchi.R
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun WaterGame(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel ) {
    val context = LocalContext.current
    val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var xTilt by remember { mutableFloatStateOf(0f) }
    var timeLeft by remember { mutableStateOf(10) } // 10-sekundowy timer
    var gameOver by remember { mutableStateOf(false) } // Flaga zakończenia gry
    var waterToAdd by remember { mutableStateOf(0) }

    val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                xTilt = event.values[0]
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            kotlinx.coroutines.delay(1000) // Opóźnienie 1 sekundy
            timeLeft--
        } else if (!gameOver) {
            gameOver = true
            waterToAdd = ((xTilt.coerceIn(0f, 4.5f) / 4.5f) * 50).toInt() // Mapowanie

            authViewModel.updateWaterLevel(addedWater = waterToAdd) // Aktualizacja w ViewModel
            kotlinx.coroutines.delay(2000) // Wyświetlenie gry przez 2 sekundy po zakończeniu
            navController.popBackStack() // Powrót do ekranu głównego
        }
    }

    LaunchedEffect(Unit) {
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    DisposableEffect(Unit) {
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            // Timer na dole ekranu
            Text(
                text = "Time left: $timeLeft s",
                fontFamily = FontFamily(Font(R.font.pacifico)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(20.dp))

            TiltedWateringCan(
                modifier = Modifier
                .height(300.dp)
                .fillMaxWidth(),
                tilt = xTilt)

            Spacer(modifier = Modifier.height(40.dp))

        }
    }
}

@Composable
fun TiltedWateringCan(
    tilt: Float, // Przechylenie telefonu w osi X
    modifier: Modifier = Modifier
) {
    val rotationZ by animateFloatAsState(
        targetValue = (-tilt.coerceIn(0f, 4.5f) / 4.5f) * 45f, // Ograniczenie kąta przechylenia do -45° i 45°
        animationSpec = tween(durationMillis = 300), label = "" // Płynna animacja przechylenia
    )

    // Lista pozycji kropli wody
    val drops = remember { mutableStateListOf<Pair<Float, Float>>() }

    // Generowanie kropli wody w zależności od kąta nachylenia
    LaunchedEffect(tilt) {
        if (tilt > 0.5f) { // Tworzenie kropli przy odpowiednim nachyleniu
            val count = ((tilt / 4.5f) * 10).toInt() // Liczba kropli zależna od nachylenia
            repeat(count) {
                drops.add(Pair(0.8f, 0.2f)) // Pozycja "dzióbka" w procentach szerokości i wysokości
            }
        }
    }

    // Aktualizacja pozycji kropli
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(16) // 60 FPS
            drops.replaceAll { (x, y) ->
                Pair(x, y + 0.02f) // Krople spadają w dół
            }
            drops.removeAll { (_, y) -> y > 1f } // Usuwanie kropli, które spadły poza ekran
        }
    }

    val xOffset by remember(rotationZ) {
        mutableStateOf(
            -160.dp + (rotationZ / 45f * 10.dp // Zakres od -170 do -160
        ))
    }
    val yOffset by remember(rotationZ) {
        mutableStateOf(
            -150.dp + (rotationZ / 45f * -120.dp // Zakres od -140 do -20
        ))
    }

    Box(
        modifier = modifier
            .size(200.dp), // Rozmiar obszaru konewki
        contentAlignment = Alignment.Center // Wycentrowanie obrazu konewki
    ) {
        // Konewka
        Image(
            painter = painterResource(id = R.drawable.wateringcan), // Zasób graficzny konewki
            contentDescription = "Konewka",
            modifier = Modifier.fillMaxSize().graphicsLayer(
                rotationZ = rotationZ // Obrót konewki zgodnie z przechyleniem telefonu
            ) // Dopasowanie konewki do rozmiaru obszaru
        )

        // Krople wody
        drops.forEach { (relativeX, relativeY) ->
            Canvas(
                modifier = Modifier
                    .offset(
                        x = with(LocalDensity.current) {
                            relativeX.dp + xOffset // Przesunięcie X na podstawie "dzióbka"
                        },
                        y = with(LocalDensity.current) {
                            relativeY * 300.dp + yOffset // Przesunięcie Y na podstawie "dzióbka"
                        }
                    )
                    .size(10.dp) // Rozmiar kropli
            ) {
                drawCircle(
                    color = Color(0xFF64B5F6), // Kolor kropli
                    radius = size.minDimension / 2 // Promień kropli
                )
            }
        }
    }
}
