package com.example.myassistantv2

import android.R
import android.os.Bundle
import android.text.Editable
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myassistantv2.databinding.ActivityEditDriverBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class EditDriverActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditDriverBinding

    private lateinit var driverViewModel: DriverViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val driverDao = AppDatabase.getDatabase(application).driverDao()
        val repository = DriverRepository(driverDao)

        val factory = DriverViewModelFactory(repository)
        driverViewModel = ViewModelProvider(this, factory)[DriverViewModel::class.java]

        lifecycleScope.launch {
            driverViewModel.allDrivers.collectLatest { drivers ->
                val driverNames = drivers.map { it.name }
                val adapter = ArrayAdapter(this@EditDriverActivity, R.layout.simple_spinner_item, driverNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.driverSpinner.adapter = adapter
            }
        }

        binding.driverSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                lifecycleScope.launch {
                    val driversList = driverViewModel.allDrivers.firstOrNull() ?: emptyList()
                    if (position in driversList.indices) {
                        val selectedDriver = driversList[position]
                        driverViewModel.selectDriver(selectedDriver)
                        binding.editDriverName.text = Editable.Factory.getInstance().newEditable(selectedDriver.name)
                        binding.editVehicleType.text = Editable.Factory.getInstance().newEditable(selectedDriver.vehicleType)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.btnUpdateDriver.setOnClickListener {
            val name = binding.editDriverName.text.toString().trim()
            val vehicle = binding.editVehicleType.text.toString().trim()
            if (name.isNotEmpty() && vehicle.isNotEmpty()) {
                driverViewModel.selectedDriver.value?.name = binding.editDriverName.text.toString()
                driverViewModel.selectedDriver.value?.vehicleType = binding.editVehicleType.text.toString()
                driverViewModel.selectedDriver.value?.let { it1 -> driverViewModel.updateDriver(it1) }
            } else {
                Toast.makeText(this, "Please enter valid details", Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(this, "Driver Added", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddDriver.setOnClickListener {
            val name = binding.editDriverName.text.toString().trim()
            val vehicle = binding.editVehicleType.text.toString().trim()
            if (name.isNotEmpty() && vehicle.isNotEmpty()) {
                driverViewModel.addDriver(name, vehicle)
                Toast.makeText(this, "Updated driver...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter valid details", Toast.LENGTH_SHORT).show()
            }

        }
    }
}

