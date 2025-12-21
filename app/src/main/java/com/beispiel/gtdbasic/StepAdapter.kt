package com.beispiel.gtdbasic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beispiel.gtdbasic.model.Step

class StepAdapter(
    private val steps: MutableList<Step>,
    private val onStepClicked: (Step) -> Unit
) : RecyclerView.Adapter<StepAdapter.StepViewHolder>() {

    private var selectedStepId: Long? = null  // (≈ CurrentRecord-ID)

    // ViewHolder (≈ eine Zeile im Endlosformular)
    class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSortOrder: TextView = itemView.findViewById(R.id.tvStepSortOrder)
        val tvName: TextView = itemView.findViewById(R.id.tvStepName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_step, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val step = steps[position]

        holder.tvSortOrder.text = step.sortOrder.toString()
        holder.tvName.text = step.name

        // Auswahl darstellen (≈ markierte Zeile)
        val isSelected = (step.id == selectedStepId)
        holder.itemView.isSelected = isSelected
        holder.itemView.alpha = if (isSelected) 0.85f else 1.0f

        // Klick = Auswahl setzen
        holder.itemView.setOnClickListener {
            selectedStepId = step.id
            notifyDataSetChanged() // (≈ Liste neu zeichnen)
            onStepClicked(step)
        }
    }

    override fun getItemCount(): Int = steps.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = steps.removeAt(fromPosition)
        steps.add(toPosition, item)

        // sortOrder (≈ Sortierfeld)
        steps.forEachIndexed { index, step ->
            step.sortOrder = index + 1
        }

        notifyItemMoved(fromPosition, toPosition)

        val start = minOf(fromPosition, toPosition)
        val end = maxOf(fromPosition, toPosition)
        notifyItemRangeChanged(start, end - start + 1)
    }


    fun getSelectedStep(): Step? { // (≈ CurrentRecord)
        val id = selectedStepId ?: return null
        return steps.firstOrNull { it.id == id }
    }

    fun clearSelection() { // (≈ CurrentRecord zurücksetzen)
        selectedStepId = null
        notifyDataSetChanged()
    }
}
