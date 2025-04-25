package com.example.myassistantv2

class TripCostCalculator(private val tripDao: TripDao) {

    private val dcTripCostMap = mapOf(
        "BB" to 725,
        "BU" to 750,
        "RA" to 650,
        "SA" to 675,
        "Haliba" to 1050,
        "Musaffah" to 100,

    )

    fun calculateCost(
        start: String,
        end: String,
        vehicleType: String
    ) {

    }
}
