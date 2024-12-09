package com.example.menghitungemisi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val historyList: List<EmissionData>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val data = historyList[position]
        holder.bind(data)
    }

    override fun getItemCount(): Int = historyList.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ccValueTextView: TextView = itemView.findViewById(R.id.ccValueText)
        private val fuelValueTextView: TextView = itemView.findViewById(R.id.fuelValueText)
        private val speedTextView: TextView = itemView.findViewById(R.id.speedText)
        private val distanceKmTextView: TextView = itemView.findViewById(R.id.distanceKmText)
        private val emissionTextView: TextView = itemView.findViewById(R.id.emissionText)

        fun bind(data: EmissionData) {
            ccValueTextView.text = "CC Value: ${data.ccValue}"
            fuelValueTextView.text = "Fuel Value: ${data.fuelValue}"
            speedTextView.text = "Speed: ${data.speed} km/h"
            distanceKmTextView.text = "Distance: ${data.distanceKm} km"
            emissionTextView.text = "Emission: ${data.emission}"
        }
    }
}