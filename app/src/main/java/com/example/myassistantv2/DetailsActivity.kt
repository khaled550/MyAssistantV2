package com.example.myassistantv2

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myassistantv2.databinding.ActivityDetailsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.log

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var excelExporter: ExcelExporter
    private lateinit var  adapter:TripAdapter

    private val tripViewModel: TripViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = TripRepository(database.tripDao())
                @Suppress("UNCHECKED_CAST")
                return TripViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val driverAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.drivers_array,
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.spinnerDriver.adapter = driverAdapter
        val plAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.pl_array,
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.spinnerPl.adapter = plAdapter

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            tripViewModel.allData.collectLatest { trips ->
                adapter = TripAdapter(trips){ selectedTrip ->
                    val resultIntent = Intent().apply {
                        putExtra("selected_trip_id", selectedTrip.id)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                recyclerView.adapter = adapter
            }
        }

        // Handle driver selection
        binding.spinnerDriver.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDriver = parent?.getItemAtPosition(position).toString()
                adapter.setFilters(selectedDriver, binding.spinnerPl.selectedItem.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // Handle Product Line selection
        binding.spinnerPl.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedPL = parent?.getItemAtPosition(position).toString()
                adapter.setFilters(binding.spinnerDriver.selectedItem.toString(), selectedPL)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_trips, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                excelExporter = ExcelExporter(this)
                exportDataToExcel()
                Toast.makeText(this, "Exporting...", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_drivers -> {
                fetchDriversWithNoTrips()
                Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportDataToExcel() {
        lifecycleScope.launch {
            val filePath = excelExporter.exportTripsToExcel(tripViewModel.allData)

            if (filePath != null) {
                Log.i("ExcelExport", "Exported to: $filePath", )
                Toast.makeText(this@DetailsActivity, "Exported to: $filePath", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@DetailsActivity, "No data to export!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDriversWithNoTrips() {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val date = LocalDate.now().format(formatter) // Today
        lifecycleScope.launch {
            tripViewModel.getDriversWithNoTrips(date).collectLatest { drivers ->
                val message = if (drivers.isNotEmpty()) {
                    "Drivers with no trips:\n" + drivers.joinToString("\n")
                } else {
                    "All drivers had trips on this date."
                }

                showDriversDialog(message)
            }
        }
    }

    private fun showDriversDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Drivers Availability")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
