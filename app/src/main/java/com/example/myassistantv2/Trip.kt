package com.example.myassistantv2

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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
    val notes: String
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

    @Query("""
        SELECT DISTINCT driver 
        FROM trips_data 
        WHERE driver NOT IN (
            SELECT DISTINCT driver FROM trips_data WHERE date = :selectedDate
        )
    """)
    fun getDriversWithNoTrips(selectedDate: String): Flow<List<String>>
}

@Database(entities = [Trip::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trip_database"
                ).build()
                INSTANCE = instance
                instance
            }
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
}

class TripViewModel(private val repository: TripRepository) : ViewModel() {
    val allData = repository.allData

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

    fun clearAll() = viewModelScope.launch {
        repository.clearAll()
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
