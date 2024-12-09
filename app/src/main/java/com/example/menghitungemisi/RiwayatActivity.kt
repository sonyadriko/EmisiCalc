package com.example.menghitungemisi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.menghitungemisi.databinding.ActivityMainBinding
import com.example.menghitungemisi.databinding.ActivityRiwayatBinding
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class RiwayatActivity : AppCompatActivity() {

    private var _binding: ActivityRiwayatBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityRiwayatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadHistory()
    }

    // Load history data from SharedPreferences and display it in RecyclerView
    private fun loadHistory() {
        val sharedPreferences = getSharedPreferences("Riwayat", MODE_PRIVATE)
        val existingData = sharedPreferences.getString("history", null)

        // If data is found in SharedPreferences, parse and display it
        if (existingData != null) {
            try {
                val jsonArray = JSONArray(existingData)
                val historyList = mutableListOf<EmissionData>()

                // Parse JSON data into EmissionData objects
                for (i in 0 until jsonArray.length()) {
                    val entry = jsonArray.getJSONObject(i)
                    val ccValue = entry.getString("ccValue")
                    val fuelValue = entry.getString("fuelValue")
                    val speed = entry.getString("speed")
                    val distanceKm = entry.getString("distanceKm")
                    val emission = entry.getString("emission")

                    // Add parsed data to the list
                    historyList.add(EmissionData(ccValue, fuelValue, speed, distanceKm, emission))
                }

                // Setup RecyclerView with the adapter
                binding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)
                val adapter = HistoryAdapter(historyList) // Set the adapter with the data list
                binding.recyclerViewHistory.adapter = adapter

            } catch (e: JSONException) {
                e.printStackTrace()
                Log.e("Riwayat", "Failed to parse history data")
            }
        } else {
            Log.d("Riwayat", "No history data found")
        }
    }
}