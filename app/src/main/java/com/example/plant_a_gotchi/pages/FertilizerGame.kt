package com.example.plant_a_gotchi.pages

import android.Manifest
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.plant_a_gotchi.AuthViewModel
import com.example.plant_a_gotchi.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun FertilizerGame(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val context = LocalContext.current

    // Zmienna stanu dla liczby kroków (używamy remember, aby stan był zachowany w komponencie)
    val counter = remember { MutableStateFlow(0) }
    val stepImages = remember { mutableStateListOf<Pair<Float, Float>>() }
    var timeLeft by remember { mutableStateOf(10) }
    var gameOver by remember { mutableStateOf(false) } // Flaga zakończenia gry
    var fertilizerToAdd by remember { mutableStateOf(0) }
    val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var lastX = 0f
    var lastY = 0f
    var lastZ = 0f

    val threshold = 10f
    val shakeRotation = remember { androidx.compose.animation.core.Animatable(0f) }

    val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Akceleracja w osi X, Y, Z
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Obliczanie zmiany w przyspieszeniu
                    val deltaX = x - lastX
                    val deltaY = y - lastY
                    val deltaZ = z - lastZ

                    // Obliczanie wartości zmiany przyspieszenia (tzw. "magnitude")
                    val magnitude = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

                    // Jeśli zmiana przyspieszenia jest większa niż próg, uznajemy to za krok
                    if (magnitude > threshold) {
                        counter.value = counter.value + 1  // Aktualizacja liczby kroków
                        // Dodajemy stopę w losowym miejscu
                        stepImages.add(Pair(Random.nextFloat(), Random.nextFloat()))


                    }

                    // Przechowujemy aktualne wartości przyspieszenia
                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }



    // Sprawdzamy uprawnienia
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            )

            if (permissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Rejestrujemy nasłuch na sensorze, jeśli pozwolenie zostało przyznane
                sensorManager.registerListener(
                    sensorEventListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
                )
            } else {
                // Jeśli pozwolenie nie zostało przyznane, zapytaj o nie
                ActivityCompat.requestPermissions(
                    context as android.app.Activity,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    1001
                )
            }
        }
    }

    val stepCount by counter.collectAsState()

    LaunchedEffect(stepCount) {
        shakeRotation.animateTo(
            targetValue = 15f, // Przechylenie w lewo/prawo
            animationSpec = tween(durationMillis = 100)
        )
        shakeRotation.animateTo(
            targetValue = -15f, // Przechylenie w przeciwną stronę
            animationSpec = tween(durationMillis = 100)
        )
        shakeRotation.animateTo(
            targetValue = 0f, // Powrót do początkowej pozycji
            animationSpec = tween(durationMillis = 100)
        )
    }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            kotlinx.coroutines.delay(1000) // Opóźnienie 1 sekundy
            timeLeft--
        } else if (!gameOver) {
            gameOver = true
            fertilizerToAdd = (((stepCount.coerceIn(0, 100).toFloat() / 100) * 50).toInt()) // Mapowanie

            authViewModel.updateFertilizerLevel(addedFertilizer = fertilizerToAdd) // Aktualizacja w ViewModel
            kotlinx.coroutines.delay(2000) // Wyświetlenie gry przez 2 sekundy po zakończeniu
            navController.popBackStack() // Powrót do ekranu głównego
        }
    }

    // Usuwamy nasłuch na sensorze, gdy komponent jest niszczony
    DisposableEffect(Unit) {
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // Zmienna, która będzie przechowywała aktualny stan liczby kroków


    // Wyświetlenie liczby kroków
    Column( verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Time left: $timeLeft s",
            fontFamily = FontFamily(Font(R.font.pacifico)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Kontener z stopami
        Box(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {

            Image(
                painter = painterResource(R.drawable.fertilizer),
                contentDescription = "Fertilizer Bag",
                colorFilter = ColorFilter.tint(Color(0xFFf5d49a)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(150.dp)
                    .graphicsLayer(
                        rotationZ = shakeRotation.value // Animacja potrząsania workiem
                    )
            )

            // Kuleczki nawozu
            stepImages.forEach { position ->
                val xPos = (position.first * 1000).coerceIn(0f, 1000f)
                val yPos = (position.second * 1000).coerceIn(0f, 1000f)


                Box(
                    modifier = Modifier
                        .offset(x = xPos.dp, y = yPos.dp)
                        .size(20.dp) // Rozmiar kulek nawozu
                        .background(
                            color = listOf(
                                Color(0xFFFFAE33), Color(0xFFFFDB2D), Color(0xFF86b77b), Color(0xFFa9d89e)
                            ).random(), // Losowy kolor z listy
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
        if (gameOver) {
            Text(
                text = "Dodano nawóz: $fertilizerToAdd",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
