package com.example.plant_a_gotchi

data class CurrentLevels(
    var water: Int = 0,
    var sun: Int = 0,
    var fertilizer: Int = 0
)

data class UserPlant (
    var plantType: String = "",
    var plantName: String = "",
    val image: Int = 0,
    var animation: Int = 0,
    var thresholds: Thresholds = Thresholds(),
    var currentLevels: CurrentLevels = CurrentLevels(),
    var plantHappiness: Int = 0,
    var plantLevel: Int = 1,
    val lastUpdate: Long = 0
)

data class Thresholds(
    val waterMin: Int = 0,
    val waterMax: Int = 0,
    val sunMin: Int = 0,
    val sunMax: Int = 0,
    val fertilizerMin: Int = 0,
    val fertilizerMax: Int = 0
)