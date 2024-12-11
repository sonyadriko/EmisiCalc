package com.example.menghitungemisi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.menghitungemisi.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class MainActivity : AppCompatActivity() {
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    // to store point A on start, and point B on end
    // this needed to calculate the distance
    private var previousLocation: Location? = null
    private var totalDistance: Float = 0f

    // Class-level variables
    private var ccValue: Int = 1 // Default to 1 to avoid division by zero
    private var fuelValue: Double = 0.0
    private var previousSpeed: Double = 0.0

    // Location services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,300).apply {
            setMinUpdateIntervalMillis(200)
        }.build()
    }
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            calculateLocation(result)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi FusedLocationProvider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLastLocation { getCurrentLocation() }
        setupView()

        binding.btnRiwayat.setOnClickListener{
            startActivity(Intent(this, RiwayatActivity::class.java))
        }
        // reset value sharedPreference
        // val sharedPreferences = getSharedPreferences("Riwayat", MODE_PRIVATE)
        // sharedPreferences.edit().remove("history").apply()

    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        binding.buttonStart.setOnClickListener {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun calculateLocation(result: LocationResult) {
        if (result.lastLocation != null) {
            val currentLocation = result.lastLocation!!

            // render speed and distances
            val speed = currentLocation.speed * 3.6 // Speed in km/h
            previousSpeed = speed // Track last known speed
            binding.speed.text = String.format(Locale.getDefault(), "%.2f", speed) + " km/h"

            if (previousLocation != null) {
                val distance = previousLocation!!.distanceTo(currentLocation) // Distance in meters
                totalDistance += distance
            }

            binding.latitude.text = currentLocation.latitude.toString()
            binding.longitude.text = currentLocation.longitude.toString()

            previousLocation = currentLocation

            binding.distance.text =
                String.format(Locale.getDefault(), "%.2f", totalDistance/1000) + "km"

            // Calculate emission based on ccValue and fuelValue
            binding.emission.apply {
                if (ccValue > 0) {
                    val emission = calculateEmission(ccValue, fuelValue, totalDistance/1000)
                    text = String.format(Locale.getDefault(), "%.4f", emission)
                } else {
                    text = "Invalid CC value"
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupView() {
        // Data untuk CC Kendaraan
        val dataCcKendaraan = arrayOf("50-110 CC", "125-150 CC", "1000-1499 CC", "1500-2000 CC", ">2000 CC")
        val ccValues = mapOf(
            "50-110 CC" to 40,
            "125-150 CC" to 50,
            "1000-1499 CC" to 80,
            "1500-2000 CC" to 100,
            ">2000 CC" to 120
        )
        val adapterCc = ArrayAdapter(this, android.R.layout.simple_spinner_item, dataCcKendaraan)
        adapterCc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCcKendaraan.adapter = adapterCc

        // Data untuk Bahan Bakar
        val dataBahanBakar = arrayOf("Bensin", "Solar")
        val fuelValues = mapOf(
            "Bensin" to 2.31,
            "Solar" to 1.8
        )
        val adapterBahanBakar = ArrayAdapter(this, android.R.layout.simple_spinner_item, dataBahanBakar)
        adapterBahanBakar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBahanBakar.adapter = adapterBahanBakar

        // Set default values to avoid null issues
        ccValue = ccValues[dataCcKendaraan[0]] ?: 1
        fuelValue = fuelValues[dataBahanBakar[0]] ?: 0.0

        binding.spinnerCcKendaraan.onItemSelectedListener = createSpinnerListener(ccValues) { selectedCc ->
            ccValue = ccValues[selectedCc] ?: 1
            Log.d("SpinnerSelection", "Selected CC Value: $ccValue")
        }

        binding.spinnerBahanBakar.onItemSelectedListener = createSpinnerListener(fuelValues) { selectedFuel ->
            fuelValue = fuelValues[selectedFuel] ?: 0.0
            Log.d("SpinnerSelection", "Selected Fuel Value: $fuelValue")
        }

        binding.btnStop.setOnClickListener {
            // crash potential
            lifecycleScope.launch {
                try {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                Log.d("Location Stop", "StopLocation updates successful! ")
                                saveResultToLocalStorage(
                                    ccValue = ccValue,
                                    fuelValue = fuelValue,
                                    speed = previousSpeed, // Track speed before stopping
                                    distanceKm = totalDistance / 1000, // Convert to km
                                    emission = calculateEmission(ccValue, fuelValue, totalDistance / 1000) // Calculate emission
                                )
                                // Show a Snackbar notification for success
                                Snackbar.make(findViewById(android.R.id.content), "Data saved successfully!", Snackbar.LENGTH_SHORT).show()
                            } else {
                                Log.e(
                                    "Location Stop",
                                    "StopLocation updates unsuccessful! " + it.result
                                )
                            }
                        }
                } catch (e: Exception) {
                    Log.e("Location", "Error removing location updates: ${e.message}")
                }
            }
        }
    }

    private fun <T> createSpinnerListener(
        data: Map<String, T>,
        onItemSelected: (String) -> Unit
    ): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position) as String
                onItemSelected(selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Handle no selection case
            }
        }
    }

    private fun saveResultToLocalStorage(ccValue: Int, fuelValue: Double, speed: Double, distanceKm: Float, emission: Double) {
        val sharedPreferences = getSharedPreferences("Riwayat", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Retrieve existing data as a JSON array, default to an empty array if null
        val existingData = sharedPreferences.getString("history", null)
        val jsonArray = if (existingData != null) {
            try {
                JSONArray(existingData)
            } catch (e: JSONException) {
                e.printStackTrace()
                JSONArray() // Start fresh if there's an error
            }
        } else {
            JSONArray() // Initialize with an empty array if data is null
        }

        // Mapping value to a readable range
        val ccRange = getCcRange(ccValue)
        val fuelType = getFuelType(fuelValue)

        // Create a new entry
        val newEntry = JSONObject().apply {
            put("ccValue", ccRange)
            put("fuelValue", fuelType)
            put("speed", String.format(Locale.getDefault(), "%.2f", speed))
            put("distanceKm", String.format(Locale.getDefault(), "%.2f", distanceKm))
            put("emission", String.format(Locale.getDefault(), "%.4f", emission))
        }

        // Add the new entry to the array
        jsonArray.put(newEntry)

        // Save the updated JSON array back to SharedPreferences
        editor.putString("history", jsonArray.toString())
        editor.apply()

        Log.d("SharedPreferences", "Data saved: ${jsonArray.toString()}")
    }

    // Function to map ccValue to a readable range
    private fun getCcRange(ccValue: Int): String {
        return when (ccValue) {
            40 -> "50-110 CC"
            35 -> "120-150 CC"
            15 -> "1000-1499 CC"
            12 -> "1500-2000 CC"
            9 -> ">2000 CC"
            else -> "Unknown CC Range"
        }
    }

    // Function to map fuelValue to a specific fuel type
    private fun getFuelType(fuelValue: Double): String {
        return when (fuelValue) {
            2.31 -> "Bensin"
            2.68 -> "Solar"
            else -> "Unknown Fuel Type"
        }
    }

    private fun calculateEmission(ccValue: Int, fuelValue: Double, distanceKm: Float): Double {
        return if (ccValue > 0) {
            (1.0 / ccValue) * fuelValue * distanceKm
        } else {
            0.0 // Handle invalid ccValue
        }
    }

    private fun getLastLocation(callback: () -> Unit) {
        // Cek izin lokasi
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Dapatkan lokasi terakhir
            fusedLocationClient.lastLocation.addOnSuccessListener {
                    callback.invoke()
                }.addOnFailureListener {
                    Toast.makeText(
                        this, "Gagal mendapatkan lokasi", Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            // Minta izin jika belum diberikan
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
}