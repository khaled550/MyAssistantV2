package com.example.myassistantv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimilarTripsAdapter(
    private val trips: List<Trip>,
    private val onTripSelected: (Trip) -> Unit
) : RecyclerView.Adapter<SimilarTripsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoute: TextView = view.findViewById(R.id.tvRoute)
        val tvVehicleType: TextView = view.findViewById(R.id.tv_vehicle_type)
        val tvCost: TextView = view.findViewById(R.id.tvCost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_similar_trip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]
        holder.tvRoute.text = "${trip.startPoint} → ${trip.endPoint}"
        holder.tvVehicleType.text = "Vehicle Type: ${trip.vehicleType}"
        holder.tvCost.text = "Cost: $${"%.0f".format(trip.cost)}"

        holder.itemView.setOnClickListener {
            onTripSelected(trip)
        }
    }

    override fun getItemCount() = trips.size
}