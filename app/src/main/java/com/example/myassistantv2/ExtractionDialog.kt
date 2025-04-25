/*
package com.example.myassistantv2

import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.util.regex.Pattern

class ExtractionDialog(
    private val context: Context,
    private val listener: ExtractionResultListener
) : Dialog(context) {

    init {
        setContentView(R.layout.dialog_text_extraction)
        setTitle("Extract Trip Details")

        val inputText = findViewById<EditText>(R.id.etInputText)
        val btnExtract = findViewById<Button>(R.id.btnExtract)

        btnExtract.setOnClickListener {
            val text = inputText.text.toString()
            if (text.isBlank()) {
                Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val details = parseText(text)
            listener.onDetailsExtracted(details) // Send back to activity
            dismiss()
        }
    }

    private fun parseText(text: String): Trip {
        val normalizedText = text.replace(Regex("\\s+"), " ").trim()

        // Improved regex for From-To extraction
        val fromToRegex = Regex("(?i)from\\s+(.+?)\\s+to\\s+([^\\s]+(?:\\s+[^\\s]+){0,1})")
        val fromToMatch = fromToRegex.find(normalizedText)

        val from = fromToMatch?.groupValues?.get(1)?.trim() ?: ""
        val to = fromToMatch?.groupValues?.get(2)?.trim() ?: ""

        // Contact extraction remains the same
        val contactRegex = Regex("(\\w+)\\s+(\\d{10,})")
        val contactMatch = contactRegex.find(normalizedText)

        val contact = if (contactMatch != null) {
            "${contactMatch.groupValues[1]} ${contactMatch.groupValues[2]}"
        } else {
            ""
        }

        return Trip(
            startPoint = from,
            endPoint = to,
            requester = contact,
            date = "",
            vehicleType = "",
            productLine = "",
            driver = "",
            cost = 0.0,
            notes = "",
            id = 0
        )
    }


    private fun parseTripLine(line: String): Pair<String, String> {
        val pattern = Pattern.compile("(?i)(.*?)\\s+to\\s+(.*)")
        val matcher = pattern.matcher(line)

        return if (matcher.find()) {
            (matcher.group(1)?.trim() ?: "") to (matcher.group(2)?.trim() ?: "")
        } else {
            "" to ""
        }
    }
}*/
