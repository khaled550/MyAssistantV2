package com.example.myassistantv2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Entity(tableName = "drivers")
data class Driver(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var name: String,
    var vehicleType: String
)

@Dao
interface DriverDao {
    @Query("SELECT * FROM drivers ORDER BY name ASC")
    fun getAllDrivers(): Flow<List<Driver>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: Driver)

    @Update
    suspend fun updateDriver(driver: Driver)

    @Query("DELETE FROM drivers WHERE id = :driverId")
    suspend fun deleteDriver(driverId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(drivers: List<Driver>)

    @Query("SELECT * FROM drivers WHERE name = :driverName LIMIT 1")
    fun getDriverByName(driverName: String): Flow<Driver?>

}

class DriverRepository(private val driverDao: DriverDao) {

    val allDrivers: Flow<List<Driver>> = driverDao.getAllDrivers()

    suspend fun addDriver(driver: Driver) {
        driverDao.insertDriver(driver)
    }

    suspend fun updateDriver(driver: Driver) {
        driverDao.updateDriver(driver)
    }

    suspend fun deleteDriver(driverId: Int) {
        driverDao.deleteDriver(driverId)
    }
}

class DriverViewModel(private val repository: DriverRepository) : ViewModel() {

    private val _driverName = MutableStateFlow("")
    val driverName: StateFlow<String> = _driverName.asStateFlow()

    private val _vehicleType = MutableStateFlow("")
    val vehicleType: StateFlow<String> = _vehicleType.asStateFlow()

    private val _selectedDriver = MutableStateFlow<Driver?>(null)
    val selectedDriver: StateFlow<Driver?> = _selectedDriver.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    val allDrivers: StateFlow<List<Driver>> = repository.allDrivers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun setDriverName(name: String) {
        _driverName.value = name
    }

    fun setVehicleType(type: String) {
        _vehicleType.value = type
    }

    fun selectDriver(driver: Driver) {
        _selectedDriver.value = driver
    }

    fun addDriver() {
        val name = _driverName.value.trim()
        val vehicle = _vehicleType.value.trim()

        if (name.isNotEmpty() && vehicle.isNotEmpty()) {
            viewModelScope.launch {
                repository.addDriver(Driver(name = name, vehicleType = vehicle))
                _eventFlow.emit("Driver added successfully!")
                clearFields()
            }
        } else {
            viewModelScope.launch {
                _eventFlow.emit("Please enter valid driver details.")
            }
        }
    }

    fun updateDriver(driver: Driver) {
        viewModelScope.launch {
            repository.updateDriver(driver)
            _eventFlow.emit("Driver updated successfully!")
        }
    }

    fun deleteDriver(driverId: Int) {
        viewModelScope.launch {
            repository.deleteDriver(driverId)
            _eventFlow.emit("Driver deleted successfully!")
        }
    }

    private fun clearFields() {
        _driverName.value = ""
        _vehicleType.value = ""
    }
}

class DriverViewModelFactory(private val repository: DriverRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DriverViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DriverViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
