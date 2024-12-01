package com.example.menghitungemisi

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.math.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var startLatitude: Double? = null
    private var startLongitude: Double? = null

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var latitudeField: EditText
    private lateinit var longitudeField: EditText
    private lateinit var startButton: Button
    private lateinit var resetButton: Button
    private lateinit var xTextView: TextView
    private lateinit var yTextView: TextView
    private lateinit var zTextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart: Button = findViewById(R.id.buttonStart)
        val btnStop: Button = findViewById(R.id.btnStop)
        val editTextLatitude: EditText = findViewById(R.id.editTextLatitude)
        val editTextLongitude: EditText = findViewById(R.id.editTextLongitude)
        val editTextJarak: EditText = findViewById(R.id.inputJarak)

        btnStart.setOnClickListener {
            // Simpan titik awal
            startLatitude = editTextLatitude.text.toString().toDoubleOrNull()
            startLongitude = editTextLongitude.text.toString().toDoubleOrNull()
            if (startLatitude != null && startLongitude != null) {
                Toast.makeText(this, "Titik awal disimpan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Masukkan koordinat awal yang valid!", Toast.LENGTH_SHORT).show()
            }
        }

        btnStop.setOnClickListener {
            val endLatitude = editTextLatitude.text.toString().toDoubleOrNull()
            val endLongitude = editTextLongitude.text.toString().toDoubleOrNull()

            if (startLatitude != null && startLongitude != null && endLatitude != null && endLongitude != null) {
                val distance = haversine(startLatitude!!, startLongitude!!, endLatitude, endLongitude)
                editTextJarak.setText(String.format("%.2f km", distance))
                Toast.makeText(this, "Jarak dihitung: $distance km", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Masukkan koordinat akhir yang valid!", Toast.LENGTH_SHORT).show()
            }
        }

        val spinnerCcKendaraan: Spinner = findViewById(R.id.spinnerCcKendaraan)
        val spinnerBahanBakar: Spinner = findViewById(R.id.spinnerBahanBakar)

        // Data untuk CC Kendaraan
        val dataCcKendaraan = arrayOf("<1500 CC", "1500 - 2500 CC", ">2500 CC")
        val adapterCc = ArrayAdapter(this, android.R.layout.simple_spinner_item, dataCcKendaraan)
        adapterCc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCcKendaraan.adapter = adapterCc

        // Data untuk Bahan Bakar
        val dataBahanBakar = arrayOf("Bensin", "Diesel", "Gas", "Listrik")
        val adapterBahanBakar = ArrayAdapter(this, android.R.layout.simple_spinner_item, dataBahanBakar)
        adapterBahanBakar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBahanBakar.adapter = adapterBahanBakar

        // Menghubungkan komponen UI (untuk menampilkan nilai akselerasi)
        xTextView = findViewById(R.id.x_value)
        yTextView = findViewById(R.id.y_value)
        zTextView = findViewById(R.id.z_value)

        // Mendapatkan SensorManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Mendapatkan sensor accelerometer
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Inisialisasi UI
        latitudeField = findViewById(R.id.editTextLatitude)
        longitudeField = findViewById(R.id.editTextLongitude)
        startButton = findViewById(R.id.buttonStart)
        resetButton = findViewById(R.id.buttonReset)

        // Inisialisasi FusedLocationProvider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Saat tombol start ditekan
        startButton.setOnClickListener {
            getCurrentLocation()
        }

        // Saat tombol reset ditekan
        resetButton.setOnClickListener {
            resetFields()
        }
    }

    private fun resetFields() {
        latitudeField.setText("")  // Kosongkan kolom Latitude
        longitudeField.setText("") // Kosongkan kolom Longitude
    }

    private fun getCurrentLocation() {
        // Cek izin lokasi
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Minta izin jika belum diberikan
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Dapatkan lokasi terakhir
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Isi latitude dan longitude
                    latitudeField.setText(location.latitude.toString())
                    longitudeField.setText(location.longitude.toString())
                } else {
                    Toast.makeText(this, "Lokasi tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mendapatkan lokasi", Toast.LENGTH_SHORT).show()
            }
    }
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onResume() {
        super.onResume()
        // Mendaftarkan listener untuk menerima update dari accelerometer
        accelerometer?.also { acc ->
            sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Melepaskan listener saat aplikasi tidak aktif
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Mendapatkan nilai percepatan pada sumbu X, Y, Z
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Menampilkan nilai percepatan pada UI
            xTextView.text = "X: $x"
            yTextView.text = "Y: $y"
            zTextView.text = "Z: $z"
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }


    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Radius bumi dalam kilometer
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}