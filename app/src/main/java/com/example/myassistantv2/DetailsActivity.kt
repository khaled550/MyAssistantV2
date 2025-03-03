package com.example.myassistantv2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myassistantv2.databinding.ActivityDetailsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var tripResultLauncher: ActivityResultLauncher<Intent>

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

        /*tripResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedTripId = result.data?.getIntExtra("selected_trip_id", -1) ?: -1
                if (selectedTripId != -1) {
                    Toast.makeText(this, "Selected Trip for Driver: $selectedTripId",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }*/

        val recyclerView = binding.recyclerView

        val adapter = TripAdapter(emptyList()) { selectedTrip ->
            val resultIntent = Intent().apply {
                putExtra("selected_trip_id", selectedTrip.id)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            tripViewModel.allData.collectLatest { trips ->
                adapter.updateList(trips)
            }
        }
    }
}
