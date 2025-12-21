package com.beispiel.gtdbasic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beispiel.gtdbasic.model.Project
import androidx.recyclerview.widget.ItemTouchHelper



class MainActivity : AppCompatActivity() {   // Activity (≈ Formular)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)   // Formular anzeigen
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }





        val btnDelete = findViewById<android.widget.Button>(R.id.btnDeleteProject)
        val btnNew = findViewById<android.widget.Button>(R.id.btnNewProject)
        val btnEdit = findViewById<android.widget.Button>(R.id.btnEditProject)

        // RecyclerView (≈ Endlosformular)
        val rvProjects = findViewById<RecyclerView>(R.id.rvProjects)
        rvProjects.layoutManager = LinearLayoutManager(this)

        // Fake-Daten (≈ temporäres Recordset im Speicher)
        val projects = mutableListOf(
            Project(id = 1, name = "Gesundheit", sortOrder = 1),
            Project(id = 2, name = "Android lernen", sortOrder = 2),
            Project(id = 3, name = "Haushalt", sortOrder = 3)
        )

        val adapter = ProjectAdapter(projects) { project ->
            val intent = android.content.Intent(this, ProjectStepsActivity::class.java)
            intent.putExtra("projectId", project.id)
            intent.putExtra("projectName", project.name)
            startActivity(intent)
        }


        rvProjects.adapter = adapter

        btnDelete.setOnClickListener {

            val selected = adapter.getSelectedProject() // (≈ CurrentRecord)
            if (selected == null) {
                android.widget.Toast.makeText(this, "Bitte zuerst ein Project auswählen.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val indexToRemove = projects.indexOfFirst { it.id == selected.id }
            if (indexToRemove < 0) {
                android.widget.Toast.makeText(this, "Auswahl nicht gefunden.", android.widget.Toast.LENGTH_SHORT).show()
                adapter.clearSelection()
                return@setOnClickListener
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Project löschen")
                .setMessage("Soll „${selected.name}“ wirklich gelöscht werden?")
                .setPositiveButton("Löschen") { _, _ ->

                    // 1) Datensatz entfernen (≈ Delete im Recordset)
                    projects.removeAt(indexToRemove)

                    // 2) Auswahl zurücksetzen (wichtig, sonst zeigt Selection auf gelöschte ID)
                    adapter.clearSelection()

                    // 3) sortOrder neu setzen (≈ Sortierfeld neu nummerieren)
                    projects.forEachIndexed { idx, p -> p.sortOrder = idx + 1 }

                    // 4) UI informieren (≈ Endlosformular aktualisieren)
                    adapter.notifyItemRemoved(indexToRemove)
                    adapter.notifyItemRangeChanged(indexToRemove, projects.size - indexToRemove)
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }

        btnEdit.setOnClickListener {

            val selected = adapter.getSelectedProject() // (≈ CurrentRecord)
            if (selected == null) {
                android.widget.Toast.makeText(this, "Bitte zuerst ein Project auswählen.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val input = android.widget.EditText(this)
            input.setText(selected.name)                 // Name vorausfüllen
            input.setSelection(input.text.length)        // Cursor ans Ende
            input.isFocusableInTouchMode = true

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Project bearbeiten")
                .setView(input)
                .setPositiveButton("Speichern") { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isBlank()) {
                        android.widget.Toast.makeText(this, "Name darf nicht leer sein.", android.widget.Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Datensatz ändern (≈ Update im Recordset)
                    selected.name = newName

                    // UI aktualisieren (≈ Endlosformular aktualisieren)
                    val index = projects.indexOfFirst { it.id == selected.id }
                    if (index >= 0) {
                        adapter.notifyItemChanged(index)
                    } else {
                        adapter.notifyDataSetChanged()
                    }
                }
                .setNegativeButton("Abbrechen", null)
                .create()

            dialog.setOnShowListener {
                dialog.window?.setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                )

                input.requestFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }

            dialog.show()
        }

        btnNew.setOnClickListener {

            val input = android.widget.EditText(this)
            input.hint = "Project-Name"
            input.isFocusableInTouchMode = true

            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Neues Project")
                .setView(input)
                .setPositiveButton("Speichern") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isBlank()) {
                        android.widget.Toast.makeText(this, "Name darf nicht leer sein.", android.widget.Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val newId = (projects.maxOfOrNull { it.id } ?: 0L) + 1L
                    val newSortOrder = projects.size + 1
                    projects.add(Project(id = newId, name = name, sortOrder = newSortOrder))

                    adapter.notifyItemInserted(projects.size - 1)
                    rvProjects.scrollToPosition(projects.size - 1)
                }
                .setNegativeButton("Abbrechen", null)
                .create()

            dialog.setOnShowListener {
                dialog.window?.setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                )

                input.requestFocus()

                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }

            dialog.show()
        }

        // Drag & Drop (≈ Zeilen verschieben im Endlosformular)
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                adapter.moveItem(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Swipe deaktiviert (0), daher leer
            }
        })

        touchHelper.attachToRecyclerView(rvProjects)

    }
}
