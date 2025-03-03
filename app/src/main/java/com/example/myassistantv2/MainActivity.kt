package com.example.myassistantv2

import android.R
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myassistantv2.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var tripResultLauncher: ActivityResultLauncher<Intent>

    private var isUpdating = false
    private var tripsAdded = "Trip added: "
    private lateinit var tripViewModel: TripViewModel
    private lateinit var binding: ActivityMainBinding
    private val driverVehicleMap = mapOf(
        "Maninder" to "40ft Trailer",
        "Mohamed Tariq" to "Double Cabin",
        "Anwar" to "Double Cabin",
        "Shaban" to "Double Cabin",
        "Saleem" to "Double Cabin",
        "Abdullah" to "Double Cabin",
        "Jan Alam" to "Double Cabin",
        "Gul Habib" to "50ft 6x6 Trailer",
        "Zangi" to "6x6 Trailer",
        "Saif Ur Rehman" to "6x6 Trailer",
        "Nam Dev" to "6x6 Trailer",
        "Durga" to "6x6 Trailer",
        "Maniraj" to "6x6 Trailer",
        "Ahmed Shah" to "6x6 Trailer",
        "Zakir" to "6x6 Trailer",
        "Nek Zali" to "6x6 Trailer",
        "Ghanayia" to "6x6 Trailer",
        "Prabhjith" to "6x6 Trailer",
        "Varinder" to "50ft Trailer",
        "Rashid" to "Hiab",
        "Kuldip" to "7 Ton Pickup",
    )

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
            if (isUpdating){
                saveTrip(isUpdating)
            } else{

                saveTrip(isUpdating)
            }
        }
        binding.clear.setOnClickListener{
            cleatFields()
        }
        binding.send.setOnClickListener{
            sendToDriver()
        }

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
                            binding.startPoint.text = Editable.Factory.getInstance().newEditable(updateTrip?.startPoint)
                            binding.endPoint.text = Editable.Factory.getInstance().newEditable(updateTrip?.endPoint)
                            binding.cost.text = Editable.Factory.getInstance().newEditable(updateTrip?.cost.toString())
                            binding.requester.text = Editable.Factory.getInstance().newEditable(updateTrip?.requester)
                            binding.notes.text = Editable.Factory.getInstance().newEditable(updateTrip?.notes)
                            val position = (binding.productLine.adapter as ArrayAdapter<String>).getPosition(updateTrip?.productLine.toString())
                            binding.productLine.setSelection(position)
                            Toast.makeText(this@MainActivity, "Selected Trip Driver: ${updateTrip?.driver}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        initiateRequesters()

        //spinners
        initiateDriversList()
        initiateDateList("")
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
            "Zahid 588954890")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, requesters)
        binding.requester.setAdapter(adapter)
    }

    private fun sendToDriver() {
        val shareMessage = "From '${binding.startPoint.text}' to '${binding.endPoint.text}', ${binding.requester.text}"
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Share Trip Details"))
    }

    /*---------*/

    private fun initiateDriversList() {
        val driversAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, driverVehicleMap.entries.toList().map { it.key })
        driversAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.driver.adapter = driversAdapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initiateDateList(updatingDate:String) {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        val dateList = mutableListOf(
            LocalDate.now().format(formatter), // Today
            LocalDate.now().minusDays(1).format(formatter), // Yesterday
            LocalDate.now().plusDays(1).format(formatter)  // Tomorrow
        )
        if (isUpdating)
            dateList.add(updatingDate)
        val dateAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, dateList)
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.date.adapter = dateAdapter
    }

    private fun initiateplList() {
        val plList:List<String> = listOf(
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
                date = binding.date.selectedItem.toString(),
                vehicleType = driverVehicleMap[binding.driver.selectedItem.toString()].toString(),
                productLine = binding.productLine.selectedItem.toString(),
                driver = binding.driver.selectedItem.toString(),
                startPoint = binding.startPoint.text.toString(),
                endPoint = binding.endPoint.text.toString(),
                cost = it,
                requester = requesterName,
                notes = binding.notes.text.toString()
            )
        }
        if (newTrip != null) {
            if (isUpdate){
                binding.textAdded.text = "Updated for "+newTrip.productLine
                sendToDriver()
                updateTrip = binding.cost.text.toString().toDoubleOrNull()?.let {
                    Trip(
                        id = selectedTripId,
                        date = binding.date.selectedItem.toString(),
                        vehicleType = driverVehicleMap[binding.driver.selectedItem.toString()].toString(),
                        productLine = binding.productLine.selectedItem.toString(),
                        driver = binding.driver.selectedItem.toString(),
                        startPoint = binding.startPoint.text.toString(),
                        endPoint = binding.endPoint.text.toString(),
                        cost = it,
                        requester = requesterName,
                        notes = binding.notes.text.toString()
                    )
                }
                updateTrip?.let {
                    tripViewModel.update(it){
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Trip updated successfully", Toast.LENGTH_SHORT).show()
                        }
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
        binding.startPoint.text?.clear()
        binding.endPoint.text?.clear()
        binding.cost.text?.clear()
        binding.requester.text?.clear()
        binding.notes.text?.clear()
    }
}
