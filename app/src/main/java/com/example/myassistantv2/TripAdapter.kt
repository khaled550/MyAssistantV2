package com.example.myassistantv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TripAdapter(private var tripList: List<Trip>, private val onTripSelected: (Trip) -> Unit) :
    RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    private var filteredTrips: List<Trip> = tripList
    private var selectedDriver: String? = null
    private var selectedPL: String? = null

    class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_trip_date)
        val tvVehicleType: TextView = view.findViewById(R.id.tv_vehicle_type)
        val tvProductLine: TextView = view.findViewById(R.id.tv_product_line)
        val tvDriver: TextView = view.findViewById(R.id.tv_driver)
        val tvStartEnd: TextView = view.findViewById(R.id.tv_start_end)
        val tvCost: TextView = view.findViewById(R.id.tv_cost)
        val tvRequester: TextView = view.findViewById(R.id.tv_requester)
        val tvNotes: TextView = view.findViewById(R.id.tv_notes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]
        holder.tvDate.text = "Date: ${trip.date}"
        holder.tvVehicleType.text = "Vehicle Type: ${trip.vehicleType}"
        holder.tvProductLine.text = "Product Line: ${trip.productLine}"
        holder.tvDriver.text = "Driver: ${trip.driver}"
        holder.tvStartEnd.text = "Start: ${trip.startPoint} → End: ${trip.endPoint}"
        holder.tvCost.text = "Cost: $${trip.cost}"
        holder.tvRequester.text = "Requester: ${trip.requester}"
        holder.tvNotes.text = "Notes: ${trip.notes}"

        holder.itemView.setOnClickListener {
            onTripSelected(trip)
        }
    }

    override fun getItemCount(): Int = tripList.size

    fun updateList(newList: List<Trip>) {
        tripList = newList
        notifyDataSetChanged()
    }

    fun setFilters(driver: String?, productLine: String?) {
        selectedDriver = driver
        selectedPL = productLine
        filteredTrips = tripList.filter { trip ->
            (selectedDriver == null || trip.driver == selectedDriver) ||
                    (selectedPL == null || trip.productLine == selectedPL)
        }
        updateList(filteredTrips)
    }

}
