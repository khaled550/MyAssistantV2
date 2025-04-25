package com.example.myassistantv2

import android.R
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myassistantv2.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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

    private var updateTrip:Trip? = null
    private var selectedTripId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        val tripDao = AppDatabase.getDatabase(application).tripDao()
        val repository = TripRepository(tripDao)

        val factory = TripViewModel.TripViewModelFactory(repository)
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        binding.swapButton.setOnClickListener {
            swapText()
        }
        binding.alltripsBtn.setOnClickListener{
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
        binding.clearBtn.setOnLongClickListener {
            updateTrip?.let { it1 -> deleteItem(it1) }
            true
        }
        binding.clearBtn.setOnClickListener{
            cleatFields()
        }
        binding.send.setOnClickListener{
            sendToDriver()
        }
        /*binding.getDetailsBtn.setOnClickListener {
            ExtractionDialog(this, this).show()
        }*/

        binding.date.editText?.text = tripViewModel.todayDate
        binding.calBtn.setOnClickListener {
            showDatePicker(binding.calBtn)
        }

        binding.requester.doOnTextChanged { text, _, _, _ ->
            lifecycleScope.launch {
                tripViewModel.getProductLineByRequesterPartialMatch(text.toString()).collectLatest { productLines ->
                    val filteredList = productLines
                        .filter { it.isNotEmpty() }  // Keep only non-empty strings
                    if (filteredList.isNotEmpty()){
                        val plPosition = (binding.productLine.adapter as ArrayAdapter<String>).getPosition(filteredList[0])
                        binding.productLine.setSelection(plPosition)
                    }
                }
            }
        }

        driverDao = AppDatabase.getDatabase(application).driverDao()
        allDrivers = driverDao.getAllDrivers()

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
                            binding.cost.text = Editable.Factory.getInstance().newEditable("%.0f".format(updateTrip?.cost))
                            binding.requester.text = Editable.Factory.getInstance().newEditable(updateTrip?.requester)
                            binding.notes.text = Editable.Factory.getInstance().newEditable(updateTrip?.notes)

                            val plPosition = (binding.productLine.adapter as ArrayAdapter<String>).getPosition(updateTrip?.productLine.toString())
                            binding.productLine.setSelection(plPosition)

                            val driverPosition = (binding.driver.adapter as ArrayAdapter<String>).getPosition(updateTrip?.driver.toString())
                            binding.driver.setSelection(driverPosition)
                        }
                    }
                }
            }
        }

        initiateRequesters()
        initiateDriversList()
        initiateplList()

    }

    private fun initiateRequesters() {
        lifecycleScope.launch {
            tripViewModel.getContactDetails().collectLatest { contactDetails ->
                val filteredList = contactDetails
                    .filter { it.split(" ").size >= 2 }  // Keep only strings with ≥2 words
                    .map { it.split(" ").take(2).joinToString(" ") }
                val adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_list_item_1,
                    filteredList
                )
                binding.requester.setAdapter(adapter)
            }
        }
    }

    private fun sendToDriver() {
        val shareMessage = "From ${binding.startPoint.text.toString().trim()} to ${binding.endPoint.text} \n${binding.requester.text}"
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
                val driverNames = listOf("Select Driver") + drivers.map { it.name }
                val adapter = ArrayAdapter(this@MainActivity, R.layout.simple_spinner_item, driverNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.driver.adapter = adapter
            }
        }
        binding.driver.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                lifecycleScope.launch {
                    driverDao.getDriverByName(parent.getItemAtPosition(position).toString())
                        .collect { driver ->
                            if (driver != null) {
                                selectedDriver = driver
                                if (!isUpdating){
                                    binding.startPoint.setOnEditorActionListener { v, actionId, _ ->
                                        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                                            // Handle Enter key press
                                            lifecycleScope.launch {
                                                tripViewModel.findSimilarTrips(
                                                    search = v.text.toString(),
                                                    vehicleType = selectedDriver.vehicleType
                                                ).collectLatest { similarTrips ->
                                                    if (similarTrips.isNotEmpty()) {
                                                        showTripsDialog(similarTrips)
                                                    } else {
                                                        Toast.makeText(this@MainActivity, "No similar trips found", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            true  // Consume the event
                                        } else {
                                            false // Let system handle other actions
                                        }
                                    }
                                    /*val startPoint = binding.startPoint.text.toString().trim()
                                    val endPoint = binding.endPoint.text.toString().trim()
                                    tripViewModel.getCostByDriver(
                                        startPoint = startPoint,
                                        endPoint = endPoint,
                                        vehicleType = selectedDriver.vehicleType
                                    ).collectLatest {
                                        val costs = it
                                        if (costs.isNotEmpty()) {
                                            if (startPoint == endPoint){
                                                binding.cost.text = Editable.Factory.getInstance().newEditable((costs.min()*1.5).toString())
                                            } else
                                                binding.cost.text = Editable.Factory.getInstance().newEditable(costs.min().toString())
                                            println("TripCost: "+costs.min().toString())
                                        }
                                        println("TripCost: NA")
                                    }*/
                                }
                            }
                        }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

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
        val newTrip = binding.cost.text.toString().toDoubleOrNull()?.let {
            Trip(
                date = binding.calBtn.text.toString(),
                vehicleType = selectedDriver.vehicleType,
                productLine = binding.productLine.selectedItem.toString(),
                driver = selectedDriver.name,
                startPoint = binding.startPoint.text.toString().trim(),
                endPoint = binding.endPoint.text.toString().trim(),
                cost = it,
                requester = binding.requester.text.toString().trim(),
                notes = binding.notes.text.toString().trim(),
            )
        }
        if (newTrip != null) {
            if (isUpdate){
                binding.textAdded.text = "Updated for "+newTrip.productLine
                Toast.makeText(this@MainActivity, "Updated for "+newTrip.productLine, Toast.LENGTH_SHORT).show()
                updateTrip = binding.cost.text.toString().toDoubleOrNull()?.let {
                    Trip(
                        id = selectedTripId,
                        date = binding.calBtn.text.toString(),
                        vehicleType = selectedDriver.vehicleType,
                        productLine = binding.productLine.selectedItem.toString(),
                        driver = selectedDriver.name,
                        startPoint = binding.startPoint.text.toString(),
                        endPoint = binding.endPoint.text.toString(),
                        cost = it,
                        requester = binding.requester.text.toString(),
                        notes = binding.notes.text.toString(),
                    )
                }
                updateTrip?.let {
                    tripViewModel.update(it){
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
        binding.date.editText?.text = tripViewModel.todayDate
        binding.driver.setSelection(0)
    }

    private fun deleteItem(deleteTrip:Trip){
        tripViewModel.delete(deleteTrip)
        Toast.makeText(this@MainActivity, "Deleted Trip for: ${updateTrip?.productLine}", Toast.LENGTH_SHORT).show()
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

    private fun showTripsDialog(similarTripsList:List<Trip>) {
        SimilarTripsDialog(
            this,
            similarTripsList,
            onTripSelected = { selectedTrip ->
                binding.apply {
                    startPoint.text = Editable.Factory.getInstance().newEditable(selectedTrip.startPoint)
                    endPoint.text = Editable.Factory.getInstance().newEditable(selectedTrip.endPoint)
                    cost.text = Editable.Factory.getInstance().newEditable(selectedTrip.cost.toString())
                    //requester.text = Editable.Factory.getInstance().newEditable(selectedTrip.requester)
                    notes.text = Editable.Factory.getInstance().newEditable(selectedTrip.notes)
                }
            },
            onDismiss = {
                // Optional: Clear the search if needed
            }
        )
    }
}
