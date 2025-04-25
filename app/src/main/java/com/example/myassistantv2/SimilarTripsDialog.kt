package com.example.myassistantv2

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.drawable.toDrawable

class SimilarTripsDialog (
    context: Context,
    similarTrips: List<Trip>,
    onTripSelected: (Trip) -> Unit,
    onDismiss: () -> Unit = {}
)
    : Dialog(context) {
    init {
        setContentView(R.layout.dialog_similar_trips)
        setTitle("Similar Trips")

        showSimilarTripsDialog(
            context = context,
            similarTrips = similarTrips,
            onTripSelected = onTripSelected,
            onDismiss = onDismiss
        )
    }

    private fun showSimilarTripsDialog(
        context: Context,
        similarTrips: List<Trip>,
        onTripSelected: (Trip) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val dialog = Dialog(context).apply {
            setContentView(R.layout.dialog_similar_trips)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            /*window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())*/

            val recyclerView = findViewById<RecyclerView>(R.id.rvSimilarTrips)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = SimilarTripsAdapter(similarTrips) { trip ->
                onTripSelected(trip)
                dismiss()
            }

            findViewById<Button>(R.id.btnCancel).setOnClickListener {
                onDismiss()
                dismiss()
            }

            setCancelable(false)
        }

        dialog.show()
    }
}
