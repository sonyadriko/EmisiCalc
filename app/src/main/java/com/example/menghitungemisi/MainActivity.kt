package com.example.menghitungemisi

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
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
import kotlinx.coroutines.launch
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

    // Location services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(300).apply {
            setPriority(Priority.PRIORITY_HIGH_ACCURACY)
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
            binding.speed.text =
                String.format(Locale.getDefault(), "%.2f", currentLocation.speed * 3.6) + "km/h"

            if (previousLocation != null) {
                val distance = previousLocation!!.distanceTo(currentLocation) // Distance in meters
                totalDistance += distance
            }

            previousLocation = currentLocation

            binding.distance.text =
                String.format(Locale.getDefault(), "%.2f", totalDistance / 1000) + "km"
            // binding.emission.text =
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupView() {
        // Data untuk CC Kendaraan
        val dataCcKendaraan = arrayOf("<1500 CC", "1500 - 2500 CC", ">2500 CC")
        val adapterCc = ArrayAdapter(this, android.R.layout.simple_spinner_item, dataCcKendaraan)
        adapterCc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCcKendaraan.adapter = adapterCc

        // Data untuk Bahan Bakar
        val dataBahanBakar = arrayOf("Bensin", "Diesel", "Gas", "Listrik")
        val adapterBahanBakar = ArrayAdapter(this, android.R.layout.simple_spinner_item, dataBahanBakar)
        adapterBahanBakar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBahanBakar.adapter = adapterBahanBakar

        binding.btnStop.setOnClickListener {
            // crash potential
            lifecycleScope.launch {
                try {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                Log.d("Location Stop", "StopLocation updates successful! ")
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