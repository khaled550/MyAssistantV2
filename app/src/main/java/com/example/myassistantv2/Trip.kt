package com.example.myassistantv2

import android.content.Context
import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(tableName = "trips_data")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val vehicleType: String,
    val productLine: String,
    val driver: String,
    val startPoint: String,
    val endPoint: String,
    val cost: Double,
    val requester: String,
    val notes: String,
)

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: Trip)

    @Query("SELECT * FROM trips_data ORDER BY id DESC")
    fun getAllData(): Flow<List<Trip>>

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query("DELETE FROM trips_data")
    suspend fun clearAll()

    @Query("SELECT * FROM trips_data WHERE id = :tripId")
    suspend fun getTripById(tripId: Int): Trip?

    @Query("SELECT DISTINCT requester FROM trips_data")
    fun getAllContactDetails(): Flow<List<String>>

    @Query("SELECT productLine FROM trips_data WHERE requester LIKE '%' || :requester || '%'")
    fun getProductLineByRequesterPartialMatch(requester: String): Flow<List<String>>

    @Query("""
        SELECT DISTINCT name 
        FROM drivers 
        WHERE name NOT IN (
            SELECT DISTINCT driver FROM trips_data WHERE date = :selectedDate
        )
    """)
    fun getDriversWithNoTrips(selectedDate: String): Flow<List<String>>

    //*****//

    @Query("""
        SELECT * FROM trips_data 
        WHERE (
        startPoint LIKE '%' || :search || '%' OR 
        endPoint LIKE '%' || :search || '%' OR
        startPoint LIKE '% ' || :search || ' %' OR
        endPoint LIKE '% ' || :search || ' %' OR
        startPoint LIKE :search || '%' OR
        endPoint LIKE :search || '%' OR
        startPoint LIKE '% ' || :search OR
        endPoint LIKE '% ' || :search
    )
        AND vehicleType = :vehicleType
        LIMIT 5
    """)
    fun findSimilarTrips(search: String, vehicleType: String): Flow<List<Trip>>

    @Query("""
        SELECT vehicleType 
        FROM trips_data 
        WHERE driver = :driverName
    """)
    fun getVehicleTypeDriverName(driverName: String): Flow<String?>

}

@Database(entities = [Trip::class, Driver::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun driverDao(): DriverDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trip_database"
                ).addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.driverDao())
                }
            }
        }

        suspend fun populateDatabase(driverDao: DriverDao) {
            val driversMap = mapOf(
                "Select Driver" to "None",
                "Maninder" to "40ft Trailer",
                "Mohamed Tariq" to "Double Cabin",
                "Anwar" to "Double Cabin",
                "Shaban" to "Double Cabin",
                "Saleem" to "Double Cabin",
                "Sanjay" to "Double Cabin",
                "Jan Alam" to "Double Cabin",
                "Gul Habib" to "50ft 6x6 Trailer",
                "Zangi" to "6x6 Trailer",
                "Saif Ur Rehman" to "6x6 Trailer",
                "Nam Dev" to "6x6 Trailer",
                "Durga" to "6x6 Trailer",
                "Maniraj" to "6x6 Trailer",
                "Ahmed Shah" to "6x6 Trailer",
                "Sahib" to "6x6 Trailer",
                "Nek Zali" to "6x6 Trailer",
                "Ghanayia" to "6x6 Trailer",
                "Prabhjith" to "6x6 Trailer",
                "Varinder" to "50ft Trailer",
                "Rashid" to "Hiab",
                "Kuldip" to "7 Ton Pickup"
            )

            val driversList = driversMap.map { (name, type) -> Driver(name = name, vehicleType = type) }
            driverDao.insertAll(driversList)
        }
    }
}

class TripRepository(private val tripDao: TripDao) {
    val allData: Flow<List<Trip>> = tripDao.getAllData()

    suspend fun insert(trip: Trip) {
        tripDao.insert(trip)
    }

    suspend fun update(trip: Trip) {
        tripDao.update(trip)
    }

    suspend fun delete(trip: Trip) {
        tripDao.delete(trip)
    }

    suspend fun clearAll() {
        tripDao.clearAll()
    }

    suspend fun getTripById(tripId: Int): Trip? {
        return tripDao.getTripById(tripId)
    }

    fun getTripsByDate(date: String): Flow<List<String>> {
        return tripDao.getDriversWithNoTrips(date)
    }

    fun getContactDetails(): Flow<List<String>> {
        return tripDao.getAllContactDetails()
    }

    fun getProductLineByRequesterPartialMatch(requester: String): Flow<List<String>> {
        return tripDao.getProductLineByRequesterPartialMatch(requester)
    }

    fun getDriverByName(driverName: String): Flow<String?> {
        return tripDao.getVehicleTypeDriverName(driverName)
    }

    fun getCostByDriver(search: String, vehicleType: String): Flow<List<Trip>> {
        return tripDao.findSimilarTrips(search, vehicleType)
    }

    fun findSimilarTrips(search: String, vehicleType: String): Flow<List<Trip>> {
        return tripDao.findSimilarTrips(search, vehicleType)
    }
}

class TripViewModel(private val repository: TripRepository) : ViewModel() {
    val allData = repository.allData

    //private val costCalculator = TripCostCalculator(repository.tripDao)

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var todayDate = Editable.Factory.getInstance().newEditable(LocalDate.now().format(formatter))

    fun insert(trip: Trip, onSuccess: () -> Unit) = viewModelScope.launch {
        repository.insert(trip)
        onSuccess()
    }

    fun update(trip: Trip, onSuccess: () -> Unit) = viewModelScope.launch {
        repository.update(trip)
    }

    fun delete(trip: Trip) = viewModelScope.launch {
        repository.delete(trip)
    }

    fun getDriversWithNoTrips(date: String): Flow<List<String>> {
        return repository.getTripsByDate(date)
    }

    fun getTripById(tripId: Int, onResult: (Trip?) -> Unit) {
        viewModelScope.launch {
            val trip = repository.getTripById(tripId)
            onResult(trip)
        }
    }

    fun findSimilarTrips(search: String, vehicleType: String): Flow<List<Trip>> = flow {
        val trips = repository.findSimilarTrips(search, vehicleType).firstOrNull() ?: emptyList()
        emit(trips)
    }

    fun getContactDetails(): Flow<List<String>> = flow {
        val contactDetails = repository.getContactDetails().firstOrNull() ?: emptyList()
        emit(contactDetails)
    }

    fun getProductLineByRequesterPartialMatch(requester: String): Flow<List<String>> = flow {
        val productLines = repository.getProductLineByRequesterPartialMatch(requester).firstOrNull() ?: emptyList()
        emit(productLines)
    }

    fun getCostByDriver(startPoint: String,endPoint: String, vehicleType: String): Flow<List<Trip>> {
        val search = if (startPoint.contains("-")) startPoint else endPoint
        return repository.getCostByDriver(search, vehicleType)
    }




class TripViewModelFactory(private val repository: TripRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
}
