package com.beispiel.gtdbasic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beispiel.gtdbasic.model.Project

class ProjectAdapter(
    private val projects: MutableList<Project>,
    private val onProjectClicked: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    private var selectedProjectId: Long? = null

    // ViewHolder (≈ eine Zeile im Endlosformular)
    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSortOrder: TextView = itemView.findViewById(R.id.tvSortOrder)
        val tvProjectName: TextView = itemView.findViewById(R.id.tvProjectName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        holder.tvSortOrder.text = project.sortOrder.toString()
        holder.tvProjectName.text = project.name

        // Auswahl-Markierung (≈ aktueller Datensatz)
        holder.itemView.isSelected = (project.id == selectedProjectId)

        holder.itemView.setOnClickListener {
            selectedProjectId = project.id
            notifyDataSetChanged()
            onProjectClicked(project)
        }
    }

    override fun getItemCount(): Int = projects.size

    fun getSelectedProject(): Project? =
        selectedProjectId?.let { id -> projects.firstOrNull { it.id == id } }
    fun clearSelection() {
        selectedProjectId = null
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = projects.removeAt(fromPosition)
        projects.add(toPosition, item)

        projects.forEachIndexed { index, p ->
            p.sortOrder = index + 1
        }

        notifyItemMoved(fromPosition, toPosition)
        val start = minOf(fromPosition, toPosition)
        val end = maxOf(fromPosition, toPosition)
        notifyItemRangeChanged(start, end - start + 1)
    }
}
