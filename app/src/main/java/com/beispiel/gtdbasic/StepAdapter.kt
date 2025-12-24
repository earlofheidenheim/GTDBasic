package com.beispiel.gtdbasic

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beispiel.gtdbasic.model.Step

class StepAdapter(
    private val onStepClicked: (step: Step, isReselection: Boolean) -> Unit,
    private val onPlayPauseClicked: (step: Step) -> Unit
) : ListAdapter<Step, StepAdapter.StepViewHolder>(StepDiffCallback()) {

    private var selectedStepId: Long? = null

    class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStepName: TextView = itemView.findViewById(R.id.tvStepName)
        val tvStepNotesPreview: TextView = itemView.findViewById(R.id.tvStepNotesPreview)
        val tvStepZiel: TextView = itemView.findViewById(R.id.tvStepZiel)
        val tvStepDauer: TextView = itemView.findViewById(R.id.tvStepDauer)
        val btnPlayPause: ImageButton = itemView.findViewById(R.id.btnPlayPause)
        val defaultDauerTextColor: ColorStateList = tvStepDauer.textColors
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_step, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val step = getItem(position)
        holder.tvStepName.text = step.name

        // Notiz-Vorschau
        if (step.notes.isNotBlank()) {
            holder.tvStepNotesPreview.text = step.notes.substringBefore("\n")
            holder.tvStepNotesPreview.visibility = View.VISIBLE
        } else {
            holder.tvStepNotesPreview.visibility = View.GONE
        }

        // Ziel und Dauer im passenden Format anzeigen
        holder.tvStepZiel.text = "Ziel: ${formatSecondsToHM(step.zielZeitInSeconds)}"
        holder.tvStepDauer.text = "Dauer: ${formatSecondsToHMS(step.dauerInSeconds)}"

        // Das richtige Icon (Play/Pause) und Textfarbe basierend auf dem Zustand setzen
        if (step.isRunning) {
            holder.btnPlayPause.setImageResource(R.drawable.ic_pause)
            holder.tvStepDauer.setTextColor(Color.RED)
        } else {
            holder.btnPlayPause.setImageResource(R.drawable.ic_play_arrow)
            holder.tvStepDauer.setTextColor(holder.defaultDauerTextColor)
        }

        holder.itemView.isSelected = (step.id == selectedStepId)

        holder.itemView.setOnClickListener {
            val isReselection = (step.id == selectedStepId)
            selectedStepId = step.id
            notifyDataSetChanged() // Aktualisiert die Markierung
            onStepClicked(step, isReselection)
        }

        holder.btnPlayPause.setOnClickListener {
            onPlayPauseClicked(step)
        }
    }

    private fun formatSecondsToHM(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return String.format("%d:%02d", hours, minutes)
    }

    private fun formatSecondsToHMS(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d:%02d", hours, minutes, seconds)
    }

    fun getSelectedStep(): Step? {
        return selectedStepId?.let { id -> currentList.firstOrNull { it.id == id } }
    }

    fun clearSelection() {
        selectedStepId = null
        notifyDataSetChanged()
    }
}

class StepDiffCallback : DiffUtil.ItemCallback<Step>() {
    override fun areItemsTheSame(oldItem: Step, newItem: Step): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Step, newItem: Step): Boolean {
        // Vergleicht alle Felder. Da sich `dauerInSeconds` bei laufenden Timern ändert,
        // wird dies eine Neuanzeige auslösen, was für die Live-Anzeige gewollt ist.
        return oldItem == newItem
    }
}
