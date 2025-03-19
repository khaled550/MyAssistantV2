package com.example.myassistantv2

import android.R
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myassistantv2.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tripResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var driverDao: DriverDao
    private lateinit var allDrivers: Flow<List<Driver>>
    private lateinit var selectedDriver: Driver

    private var isUpdating = false
    private var tripsAdded = "Trip added: "
    private lateinit var tripViewModel: TripViewModel
    private lateinit var binding: ActivityMainBinding
    /*private val driverVehicleMap = mapOf(
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
        "Kuldip" to "7 Ton Pickup",
    )*/

    private var updateTrip:Trip? = null
    var selectedTripId = -1

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.swapButton.setOnClickListener {
            swapText()
        }
        binding.allTrips.setOnClickListener{
            showAllTrips()
        }
        binding.save.text = "Save"
        binding.save.setOnClickListener{
            saveTrip(isUpdating)
        }
        binding.save.setOnLongClickListener {
            saveTrip(false)
            true
        }
        binding.clear.setOnLongClickListener {
            updateTrip?.let { it1 -> deleteItem(it1) }
            true
        }
        binding.clear.setOnClickListener{
            cleatFields()
            /*saveTripToDatabase("from Abu-Dhabi to AD-111 BB\n" +
                    "Abdelrahman +971 54 221 3545")*/
        }
        binding.send.setOnClickListener{
            sendToDriver()
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        var todayDate = Editable.Factory.getInstance().newEditable(LocalDate.now().format(formatter))
        binding.date.editText?.text = todayDate
        binding.calBtn.setOnClickListener {
            showDatePicker(binding.calBtn)
        }

        driverDao = AppDatabase.getDatabase(application).driverDao()
        allDrivers = driverDao.getAllDrivers()

        val tripDao = AppDatabase.getDatabase(application).tripDao()
        val repository = TripRepository(tripDao)

        val factory = TripViewModelFactory(repository)
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        tripResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedTripId = result.data?.getIntExtra("selected_trip_id", -1) ?: -1
                if (selectedTripId != -1) {
                    isUpdating = true
                    binding.save.text = "Update"
                    lifecycleScope.launch {
                        tripViewModel.getTripById(selectedTripId) { trip ->
                            updateTrip = trip
                            binding.date.editText?.text = Editable.Factory.getInstance().newEditable(updateTrip?.date)
                            binding.startPoint.text = Editable.Factory.getInstance().newEditable(updateTrip?.startPoint)
                            binding.endPoint.text = Editable.Factory.getInstance().newEditable(updateTrip?.endPoint)
                            binding.cost.text = Editable.Factory.getInstance().newEditable(updateTrip?.cost.toString())
                            binding.requester.text = Editable.Factory.getInstance().newEditable(updateTrip?.requester)
                            binding.notes.text = Editable.Factory.getInstance().newEditable(updateTrip?.notes)

                            val plPosition = (binding.productLine.adapter as ArrayAdapter<String>).getPosition(updateTrip?.productLine.toString())
                            binding.productLine.setSelection(plPosition)

                            val driverPosition = (binding.driver.adapter as ArrayAdapter<String>).getPosition(updateTrip?.driver.toString())
                            binding.driver.setSelection(driverPosition)

                            /*val datePosition = (binding.date.adapter as ArrayAdapter<String>).getPosition(updateTrip?.date.toString())
                            binding.date.setSelection(datePosition)*/

                            Toast.makeText(this@MainActivity, "Selected Trip Driver: ${updateTrip?.driver}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        initiateRequesters()

        //spinners
        initiateDriversList()
        //initiateDateList("")
        initiateplList()

        /*val database = AppDatabase.getDatabase(applicationContext)
        val repository = TripRepository(database.tripDao())
        val factory = TripViewModelFactory(repository)
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]*/
    }

    private fun initiateRequesters() {
        val requesters = listOf(
            "Anil 0524468168",
            "Nizar 0564117280 ",
            "Ashique 0567804305",
            "Umair 0503868306 ",
            "Sarry 0581076513",
            "Hassan 0543049411",
            "Henry 0553377193",
            "Brian 0521043115",
            "Zahid 588954890")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, requesters)
        binding.requester.setAdapter(adapter)
    }

    private fun sendToDriver() {
        val shareMessage = "From ${binding.startPoint.text} to ${binding.endPoint.text} \n${binding.requester.text}"
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Share Trip Details"))
    }

    /*---------*/

    private fun initiateDriversList() {
        val driverDao = AppDatabase.getDatabase(application).driverDao()
        val allDrivers: Flow<List<Driver>> = driverDao.getAllDrivers()
        lifecycleScope.launch {
            allDrivers.collectLatest { drivers ->
                val driverNames = drivers.map { it.name }
                val adapter = ArrayAdapter(this@MainActivity, R.layout.simple_spinner_item, driverNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.driver.adapter = adapter
            }
        }
        binding.driver.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                //selectedDriver = driverDao.getDriverByName()
                lifecycleScope.launch {
                    driverDao.getDriverByName(parent.getItemAtPosition(position).toString())
                        .collect { driver ->
                            if (driver != null) {
                                selectedDriver = driver
                            }
                        }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    /*@RequiresApi(Build.VERSION_CODES.O)
    private fun initiateDateList(updatingDate:String) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val dateList = mutableListOf(
            LocalDate.now().format(formatter), // Today
            LocalDate.now().minusDays(1).format(formatter), // Yesterday
            LocalDate.now().plusDays(1).format(formatter)  // Tomorrow
        )
        if (isUpdating)
            dateList.add(updatingDate)
        val dateAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, dateList)
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        //binding.date.adapter = dateAdapter
    }*/

    private fun initiateplList() {
        val plList:List<String> = listOf(
            "Select PL",
            "WCT",
            "DS",
            "Rental",
            "WL",
            "Liner Hanger",
            "Fishing",
            "TRS",
            "Thru Tubing",
            "Coil Tubing",
            "ALS",
            "PCE",
            "PAS",
            "JAR",
            "M/S",
            "SDS",

            )
        val plAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, plList)
        plAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.productLine.adapter = plAdapter
    }

    private fun saveTrip(isUpdate:Boolean) {

        val requesterName = binding.requester.text.split(" ").first()
        val newTrip = binding.cost.text.toString().toDoubleOrNull()?.let {
            Trip(
                date = binding.date.toString(),
                vehicleType = selectedDriver.vehicleType,
                productLine = binding.productLine.selectedItem.toString(),
                driver = selectedDriver.name,
                startPoint = binding.startPoint.text.toString().trim(),
                endPoint = binding.endPoint.text.toString().trim(),
                cost = it,
                requester = requesterName.trim(),
                notes = binding.notes.text.toString().trim()
            )
        }
        if (newTrip != null) {
            if (isUpdate){
                binding.textAdded.text = "Updated for "+newTrip.productLine
                updateTrip = binding.cost.text.toString().toDoubleOrNull()?.let {
                    Trip(
                        id = selectedTripId,
                        date = binding.date.editText?.text.toString(),
                        vehicleType = selectedDriver.vehicleType,
                        productLine = binding.productLine.selectedItem.toString(),
                        driver = selectedDriver.name,
                        startPoint = binding.startPoint.text.toString(),
                        endPoint = binding.endPoint.text.toString(),
                        cost = it,
                        requester = requesterName,
                        notes = binding.notes.text.toString()
                    )
                }
                updateTrip?.let {
                    tripViewModel.update(it){
                        Toast.makeText(this@MainActivity, "Trip updated successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            } else{
                binding.textAdded.text = tripsAdded+newTrip.productLine
                sendToDriver()
                tripViewModel.insert(newTrip){
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Trip inserted successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showAllTrips() {
        val intent = Intent(this, DetailsActivity::class.java)
        tripResultLauncher.launch(intent)
    }

    private fun swapText() {
        binding.startPoint.animate().alpha(0f).setDuration(200).withEndAction {
            val temp = binding.startPoint.text.toString()
            binding.startPoint.setText(binding.endPoint.text.toString())
            binding.endPoint.setText(temp)
            binding.startPoint.animate().alpha(1f).duration = 200
        }
    }

    private fun cleatFields() {
        isUpdating = false
        binding.save.text = "Save"
        binding.startPoint.text?.clear()
        binding.endPoint.text?.clear()
        binding.cost.text?.clear()
        binding.requester.text?.clear()
        binding.notes.text?.clear()
        binding.productLine.setSelection(0)
        binding.date.editText?.text?.clear()
        binding.driver.setSelection(0)
    }

    private fun deleteItem(deleteTrip:Trip){
        tripViewModel.delete(deleteTrip)
        Toast.makeText(this@MainActivity, "Deleted Trip for: ${updateTrip?.productLine}", Toast.LENGTH_SHORT).show()
    }

    fun extractTripDetails(text: String): Trip? {
        val regex = """Date:\s*(\d{4}-\d{2}-\d{2})\s*Vehicle:\s*(\w+)\s*PL:\s*(\w+)\s*Driver:\s*([\w\s]+)\s*From:\s*([\w\s]+)\s*To:\s*([\w\s]+)\s*Cost:\s*(\d+(\.\d+)?)\s*Requester:\s*([\w\s]+)\s*Notes:\s*(.*)""".toRegex()

        val matchResult = regex.find(text)
        return matchResult?.let {
            val (date, vehicleType, productLine, driver, startPoint, endPoint, cost, _, requester, notes) = it.destructured
            Trip(
                date = date,
                vehicleType = vehicleType,
                productLine = productLine,
                driver = driver,
                startPoint = startPoint,
                endPoint = endPoint,
                cost = cost.toDouble(),
                requester = requester,
                notes = notes
            )
        }
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                editText.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
}
