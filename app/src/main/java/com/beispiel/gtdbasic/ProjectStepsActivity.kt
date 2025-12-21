package com.beispiel.gtdbasic

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beispiel.gtdbasic.model.Step

class ProjectStepsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_steps)

        // --- Insets (Status-/Navigation-Bar-Abstand) ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.stepsRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // --- Übergabeparameter (≈ OpenArgs) ---
        val projectId = intent.getLongExtra("projectId", -1L)
        val projectName = intent.getStringExtra("projectName") ?: "Unbekannt"

        // --- Toolbar ---
        val toolbar = findViewById<Toolbar>(R.id.toolbarSteps)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Steps – $projectName"

        // --- Header ---
        val tvHeader = findViewById<TextView>(R.id.tvStepsHeader)
        tvHeader.text = "Steps: $projectName (ID: $projectId)"

        // --- RecyclerView ---
        val rvSteps = findViewById<RecyclerView>(R.id.rvSteps)
        rvSteps.layoutManager = LinearLayoutManager(this)

        // --- Datenquelle ---
        val steps = StepStore.getSteps(projectId)

        // --- Adapter + Auswahl ---
        val stepAdapter = StepAdapter(steps) { step ->
            android.widget.Toast.makeText(this, "Ausgewählt: ${step.name}", android.widget.Toast.LENGTH_SHORT).show()
        }
        rvSteps.adapter = stepAdapter

        // --- Drag & Drop (≈ Zeilen verschieben) ---
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
                stepAdapter.moveItem(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Swipe deaktiviert (0), daher leer
            }
        })
        touchHelper.attachToRecyclerView(rvSteps)

        // --- Buttons ---
        val btnNewStep = findViewById<Button>(R.id.btnNewStep)
        val btnEditStep = findViewById<Button>(R.id.btnEditStep)
        val btnDeleteStep = findViewById<Button>(R.id.btnDeleteStep)

        // Helper: Tastatur stabil öffnen (kein toggle!)
        fun showKeyboardStable(input: android.widget.EditText, dialog: AlertDialog) {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

            input.isFocusableInTouchMode = true
            input.requestFocus()

            input.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }

            input.postDelayed({
                val imm = getSystemService(INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }, 250)
        }

        // =========================
        // NEU
        // =========================
        btnNewStep.setOnClickListener {

            val input = android.widget.EditText(this)
            input.hint = "Step-Name"

            val dialog = AlertDialog.Builder(this)
                .setTitle("Neuer Step")
                .setView(input)
                .setPositiveButton("Speichern", null)
                .setNegativeButton("Abbrechen", null)
                .create()

            dialog.setOnShowListener {
                showKeyboardStable(input, dialog)

                val btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                btnSave.setOnClickListener {
                    val name = input.text.toString().trim()
                    if (name.isBlank()) {
                        android.widget.Toast.makeText(this, "Name darf nicht leer sein.", android.widget.Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val newId = (steps.maxOfOrNull { it.id } ?: 0L) + 1L
                    val newSortOrder = steps.size + 1

                    steps.add(
                        Step(
                            id = newId,
                            projectId = projectId,
                            name = name,
                            sortOrder = newSortOrder
                        )
                    )

                    stepAdapter.clearSelection()
                    stepAdapter.notifyItemInserted(steps.size - 1)
                    rvSteps.scrollToPosition(steps.size - 1)

                    dialog.dismiss()
                }
            }

            dialog.show()
        }

        // =========================
        // BEARBEITEN (robust + stabile Tastatur)
        // =========================
        btnEditStep.setOnClickListener {

            val selected = stepAdapter.getSelectedStep()
            if (selected == null) {
                android.widget.Toast.makeText(this, "Bitte zuerst einen Step auswählen.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val input = android.widget.EditText(this)
            input.setText(selected.name)
            input.setSelection(input.text.length)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Step bearbeiten")
                .setView(input)
                .setPositiveButton("Speichern", null)
                .setNegativeButton("Abbrechen", null)
                .create()

            dialog.setOnShowListener {
                showKeyboardStable(input, dialog)

                val btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                btnSave.setOnClickListener {
                    val newName = input.text.toString().trim()
                    if (newName.isBlank()) {
                        android.widget.Toast.makeText(this, "Name darf nicht leer sein.", android.widget.Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val index = steps.indexOfFirst { it.id == selected.id }
                    if (index == -1) {
                        android.widget.Toast.makeText(this, "Auswahl nicht gefunden.", android.widget.Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    steps[index].name = newName
                    stepAdapter.notifyItemChanged(index)

                    dialog.dismiss()
                }
            }

            dialog.show()
        }

        // =========================
        // LÖSCHEN
        // =========================
        btnDeleteStep.setOnClickListener {

            val selected = stepAdapter.getSelectedStep()
            if (selected == null) {
                android.widget.Toast.makeText(this, "Bitte zuerst einen Step auswählen.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Step löschen")
                .setMessage("Möchten Sie den Step \"${selected.name}\" wirklich löschen?")
                .setPositiveButton("Löschen") { _, _ ->
                    val index = steps.indexOfFirst { it.id == selected.id }
                    if (index >= 0) {
                        steps.removeAt(index)
                        stepAdapter.clearSelection()
                        stepAdapter.notifyItemRemoved(index)

                        steps.forEachIndexed { i, step -> step.sortOrder = i + 1 }
                        stepAdapter.notifyItemRangeChanged(index, steps.size - index)
                    }
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
