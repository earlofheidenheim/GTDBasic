package com.beispiel.gtdbasic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beispiel.gtdbasic.model.Project

class ProjectAdapter(
    private val onProjectClicked: (project: Project, isReselection: Boolean) -> Unit
) : ListAdapter<Project, ProjectAdapter.ProjectViewHolder>(ProjectDiffCallback()) {

    private var selectedProjectId: Long? = null

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProjectName: TextView = itemView.findViewById(R.id.tvProjectName)
        val tvProjectNotesPreview: TextView = itemView.findViewById(R.id.tvProjectNotesPreview)
        val tvProjectDauer: TextView = itemView.findViewById(R.id.tvProjectDauer)
        val tvProjectKategorie: TextView = itemView.findViewById(R.id.tvProjectKategorie)
        val tvProjectStatus: TextView = itemView.findViewById(R.id.tvProjectStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = getItem(position)
        holder.tvProjectName.text = project.name

        // Notiz-Vorschau
        if (project.notes.isNotBlank()) {
            holder.tvProjectNotesPreview.text = project.notes.substringBefore("\n")
            holder.tvProjectNotesPreview.visibility = View.VISIBLE
        } else {
            holder.tvProjectNotesPreview.visibility = View.GONE
        }

        // Dauer im HH:mm Format anzeigen
        holder.tvProjectDauer.text = "Dauer: ${formatDuration(project.dauer)}"
        holder.tvProjectKategorie.text = if (project.kategorie.isNotBlank()) "Kategorie: ${project.kategorie}" else "Kategorie: -"
        holder.tvProjectStatus.text = if (project.status.isNotBlank()) "Status: ${project.status}" else "Status: -"

        holder.itemView.isSelected = (project.id == selectedProjectId)

        holder.itemView.setOnClickListener {
            val isReselection = (project.id == selectedProjectId)
            selectedProjectId = project.id
            notifyDataSetChanged() // Aktualisiert die Markierung
            onProjectClicked(project, isReselection)
        }
    }

    /**
     * Formatiert die Dauer von Gesamtminuten in einen "Stunden:Minuten"-String.
     */
    private fun formatDuration(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format("%d:%02d", hours, minutes)
    }

    fun getSelectedProject(): Project? {
        return selectedProjectId?.let { id -> currentList.firstOrNull { it.id == id } }
    }

    fun clearSelection() {
        selectedProjectId = null
        notifyDataSetChanged()
    }
}

class ProjectDiffCallback : DiffUtil.ItemCallback<Project>() {
    override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
        return oldItem == newItem
    }
}
