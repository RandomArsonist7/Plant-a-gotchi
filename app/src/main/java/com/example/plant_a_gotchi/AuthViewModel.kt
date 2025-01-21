package com.example.plant_a_gotchi

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://plant-a-gotchi-default-rtdb.europe-west1.firebasedatabase.app/")
    private var isUpdating = false
    private val _authState = MutableLiveData<AuthState>()
    val authState : LiveData<AuthState> = _authState


    fun getUserUid(): String? {
        return auth.currentUser?.uid
    }

    val isUserAuthenticated: Boolean
        get() = auth.currentUser != null

    fun checkAuthStatus(){
        if(auth.currentUser==null){
            _authState.value = AuthState.Unauthenticated
        }else{
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email : String, password : String){
        if (email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{task ->
                if (task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                }else{
                    _authState.value = AuthState.Error(task.exception?.message?:"Something went wrong")
                }
            }
    }

    fun signup(email : String, password : String){
        if (email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener{task ->
                if (task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                }else{
                    _authState.value = AuthState.Error(task.exception?.message?:"Something went wrong")
                }
            }
    }

    fun signout(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }


    fun updateWaterLevel(addedWater: Int) {
        val userId = auth.currentUser?.uid ?: return
        val plantRef = database.getReference("users/$userId/plant/currentLevels")

        plantRef.get().addOnSuccessListener { snapshot ->
            val currentLevels = snapshot.getValue(CurrentLevels::class.java)
            if (currentLevels != null) {
                val newWaterLevel = (currentLevels.water + addedWater).coerceAtMost(100) // Max 100
                plantRef.child("water").setValue(newWaterLevel)
            }
        }.addOnSuccessListener {
            Log.d("Firebase", "Water level updated successfully.")
        }.addOnFailureListener {
            Log.e("Firebase", "Failed to update water level", it)
        }
    }

    fun updateSunLevel(addedSun: Int) {
        val userId = auth.currentUser?.uid ?: return
        val plantRef = database.getReference("users/$userId/plant/currentLevels")

        plantRef.get().addOnSuccessListener { snapshot ->
            val currentLevels = snapshot.getValue(CurrentLevels::class.java)
            if (currentLevels != null) {
                val newSunLevel = (currentLevels.sun + addedSun).coerceAtMost(100) // Max 100
                plantRef.child("sun").setValue(newSunLevel)
            }
        }.addOnSuccessListener {
            Log.d("Firebase", "Sun level updated successfully.")
        }.addOnFailureListener {
            Log.e("Firebase", "Failed to update sun level", it)
        }
    }

    fun updateFertilizerLevel(addedFertilizer: Int) {
        val userId = auth.currentUser?.uid ?: return
        val plantRef = database.getReference("users/$userId/plant/currentLevels")

        plantRef.get().addOnSuccessListener { snapshot ->
            val currentLevels = snapshot.getValue(CurrentLevels::class.java)
            if (currentLevels != null) {
                val newFertilizerLevel = (currentLevels.fertilizer + addedFertilizer).coerceAtMost(100) // Max 100
                plantRef.child("fertilizer").setValue(newFertilizerLevel)
            }
        }.addOnSuccessListener {
            Log.d("Firebase", "Fertilizer level updated successfully.")
        }.addOnFailureListener {
            Log.e("Firebase", "Failed to update fertilizer level", it)
        }
    }

    fun applyTimeEffect() {
        val userId = auth.currentUser?.uid ?: return
        val plantRef = database.getReference("users/$userId/plant")

        plantRef.get().addOnSuccessListener { snapshot ->
            val currentLevels = snapshot.child("currentLevels").getValue(CurrentLevels::class.java)
            val lastUpdate = snapshot.child("lastUpdate").getValue(Long::class.java) ?: System.currentTimeMillis()

            val currentTime = System.currentTimeMillis()
            val elapsedTime = (currentTime - lastUpdate) / (1000 * 60) // Liczba godzin

            if (elapsedTime > 0 && currentLevels != null) {
                val waterDecayRate = 1 // np. -1 na godzinę
                val sunDecayRate = 1
                val fertilizerDecayRate = 1
                // Zmniejsz poziomy potrzeb

                val newWaterLevel = (currentLevels.water - (elapsedTime * waterDecayRate)).coerceAtLeast(0).toInt()
                val newSunLevel = (currentLevels.sun - (elapsedTime * sunDecayRate)).coerceAtLeast(0).toInt()
                val newFertilizerLevel = (currentLevels.fertilizer - (elapsedTime * fertilizerDecayRate)).coerceAtLeast(0).toInt()

                // Zaktualizuj Firebase
                plantRef.child("currentLevels").setValue(
                    CurrentLevels(newWaterLevel, newSunLevel, newFertilizerLevel)
                )
                updateLastUpdateTime()
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Failed to fetch or update plant levels", it)
        }
    }

    fun updateLastUpdateTime() {
        val userId = auth.currentUser?.uid ?: return
        val plantRef = database.getReference("users/$userId/plant")
        val currentTime = System.currentTimeMillis()

        // Zaktualizowanie `lastUpdate` w bazie danych
        plantRef.child("lastUpdate").setValue(currentTime)
            .addOnSuccessListener {
                Log.d("Firebase", "Last update time saved successfully.")
            }
            .addOnFailureListener {
                Log.e("Firebase", "Failed to update last update time", it)
            }
    }


    fun updatePlantLevel(waterLevel: Int, sunLevel: Int, fertilizerLevel: Int, initialLevels: CurrentLevels) {
        synchronized(this) {
            if (isUpdating) {
                Log.d("Firebase", "Skipping update as another update is in progress.")
                return
            }
            isUpdating = true
        }
        val userId = auth.currentUser?.uid ?: run {
            isUpdating = false
            return
        }
        val plantRef = database.getReference("users/$userId/plant")

        plantRef.get().addOnSuccessListener { snapshot ->
            val userPlant = snapshot.getValue(UserPlant::class.java)

            if (userPlant != null) {
                val happinessPoints = userPlant.plantHappiness
                var updatedHappinessPoints = happinessPoints
                val plantLevel = userPlant.plantLevel
                var updatedLevel = plantLevel

                // Flaga, która zapisuje, czy potrzeby były już ustawione na 100
                var wasLevel100Before = initialLevels.water == 100 && initialLevels.sun == 100 && initialLevels.fertilizer == 100
                Log.d("Firebase", "Was level 100 before: $wasLevel100Before")

                // Sprawdzamy, czy wszystkie poziomy są równe 100 po raz pierwszy
                if (waterLevel == 100 && sunLevel == 100 && fertilizerLevel == 100 && !wasLevel100Before) {
                    updatedHappinessPoints += 1  // Zwiększ punkty zadowolenia o 1
                    Log.d("Firebase", "All levels are 100 for the first time, increasing happiness points to $updatedHappinessPoints")

                    // Zaktualizuj flagę, żeby nie dodawać więcej punktów zadowolenia
                    initialLevels.water = waterLevel
                    initialLevels.sun = sunLevel
                    initialLevels.fertilizer = fertilizerLevel
                    wasLevel100Before = true
                }

                var wasLevel0Before = initialLevels.water == 0 && initialLevels.sun == 0 && initialLevels.fertilizer == 0
                Log.d("Firebase", "Was level 0 before: $wasLevel0Before")

                // Odejmij 1 punkt, jeśli którakolwiek potrzeba wynosi 0
                if (waterLevel == 0 && sunLevel == 0 && fertilizerLevel == 0 && !wasLevel0Before ) {
                    updatedHappinessPoints -= 1  // Zwiększ punkty zadowolenia o 1
                    Log.d("Firebase", "All levels are 100 for the first time, increasing happiness points to $updatedHappinessPoints")

                    // Zaktualizuj flagę, żeby nie dodawać więcej punktów zadowolenia
                    initialLevels.water = waterLevel
                    initialLevels.sun = sunLevel
                    initialLevels.fertilizer = fertilizerLevel
                    wasLevel100Before = true



                } else if (waterLevel == 0 && sunLevel == 0 && fertilizerLevel == 0 && wasLevel0Before ){
                val lastUpdate = snapshot.child("lastUpdate").getValue(Long::class.java) ?: System.currentTimeMillis()

                val currentTime = System.currentTimeMillis()
                val elapsedTime = (currentTime - lastUpdate) / (1000 * 60) // Liczba godzin

                if (elapsedTime > 0) {
                    val happinessDecayRate = 1
                    // Zmniejsz poziomy potrzeb

                    updatedHappinessPoints = (updatedHappinessPoints - (elapsedTime * happinessDecayRate)).coerceAtLeast(0).toInt()
                    Log.d("Firebase", "All levels are 0 for the first time, decreasing happiness points to $updatedHappinessPoints")
                    wasLevel0Before = false
                }
                }

                if (updatedHappinessPoints >= 100) {
                    if (updatedLevel < 10) {
                        updatedLevel += 1
                        updatedHappinessPoints = 0
                        Log.d("Firebase", "Level increased to $updatedLevel")
                    } else {
                        updatedHappinessPoints = 100
                        updatedLevel = 10
                    }
                } else if (updatedHappinessPoints <= 0) {
                    if (updatedLevel > 1) {
                        updatedLevel -= 1
                        updatedHappinessPoints = 99
                        Log.d("Firebase", "Level decreased to $updatedLevel")
                    } else {
                        updatedLevel = 1
                        updatedHappinessPoints = 0
                    }
                }

                // Przygotowanie mapy do aktualizacji
                val updates = mapOf(
                    "plantHappiness" to updatedHappinessPoints,
                    "plantLevel" to updatedLevel,
                    "lastUpdate" to System.currentTimeMillis()
                )

                // Zaktualizowanie poziomu i punktów w Firebase
                plantRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Plant data updated successfully.")
                    }
                    .addOnFailureListener {
                        Log.e("Firebase", "Failed to update plant data", it)
                    }
                    .addOnCompleteListener{
                        isUpdating = false
                    }
            } else {
                isUpdating = false
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Failed to fetch plant data", it)
            isUpdating = false
        }
    }


    fun getPlantImageResource(context: Context, plantType: String, plantLevel: Int): Int {
        // Zmień nazwę na format "typrośliny_poziom"
        val resourceName = "${plantType.lowercase()}_${plantLevel}"
        return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }

}




sealed class AuthState{
    object Authenticated : AuthState()
    object Unauthenticated: AuthState()
    object Loading : AuthState()
    data class Error(val message : String) : AuthState()

}