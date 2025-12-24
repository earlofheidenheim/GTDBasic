package com.beispiel.gtdbasic

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beispiel.gtdbasic.model.Step
import com.beispiel.gtdbasic.ui.GtdViewModel
import java.util.Collections

class ProjectStepsActivity : AppCompatActivity() {

    private val gtdViewModel: GtdViewModel by viewModels()
    private lateinit var adapter: StepAdapter
    private var projectId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_steps)

        projectId = intent.getLongExtra("projectId", -1L)
        val projectName = intent.getStringExtra("projectName") ?: "Steps"

        setupToolbar(projectName)
        setupRecyclerView()
        observeViewModel()
        setupButtons()
        handleWindowInsets()
    }

    private fun setupToolbar(projectName: String) {
        val toolbar = findViewById<Toolbar>(R.id.toolbarSteps)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Steps of $projectName"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbar.setOnClickListener {
            onSupportNavigateUp()
        }
    }

    private fun setupRecyclerView() {
        val rvSteps = findViewById<RecyclerView>(R.id.rvSteps)

        adapter = StepAdapter(
            onStepClicked = { step, isReselection ->
                if (isReselection) {
                    Toast.makeText(this, "Springe zur Detailseite von ${step.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onPlayPauseClicked = { step ->
                gtdViewModel.togglePlayPause(step)
            }
        )

        rvSteps.adapter = adapter
        rvSteps.layoutManager = LinearLayoutManager(this)

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.absoluteAdapterPosition
                val toPosition = target.absoluteAdapterPosition

                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false
                }

                val currentList = adapter.currentList.toMutableList()
                Collections.swap(currentList, fromPosition, toPosition)
                adapter.submitList(currentList)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    val position = viewHolder.absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val step = adapter.currentList[position]
                        showResetDurationDialog(step)
                    }
                }
                // Wichtig: Die Zeile wird durch die neue Liste vom ViewModel automatisch zurückgesetzt
                adapter.notifyItemChanged(viewHolder.absoluteAdapterPosition)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                gtdViewModel.updateStepSortOrder(adapter.currentList)
            }
        })
        itemTouchHelper.attachToRecyclerView(rvSteps)
    }

    private fun observeViewModel() {
        gtdViewModel.getStepsForProject(projectId).observe(this) { steps ->
            adapter.submitList(steps)
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnNewStep).setOnClickListener { showNewStepDialog() }
        findViewById<Button>(R.id.btnEditStep).setOnClickListener { showEditStepDialog() }
        findViewById<Button>(R.id.btnDeleteStep).setOnClickListener { showDeleteStepDialog() }
    }

    private fun showNewStepDialog() {
        val input = EditText(this).apply { hint = "Step-Name" }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Neuer Step")
            .setView(input)
            .setPositiveButton("Speichern") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    val newStep = Step(projectId = projectId, name = name, sortOrder = adapter.itemCount + 1)
                    gtdViewModel.insertStep(newStep)
                } else {
                    Toast.makeText(this, "Name darf nicht leer sein.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .create()
        showKeyboardInDialog(dialog, input)
        dialog.show()
    }

    private fun showEditStepDialog() {
        val selectedStep = adapter.getSelectedStep() ?: run {
            Toast.makeText(this, "Bitte zuerst einen Step auswählen.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_step, null)
        val etName = dialogView.findViewById<EditText>(R.id.etStepName)
        val etZielZeit = dialogView.findViewById<EditText>(R.id.etStepZielZeit)
        val etNotes = dialogView.findViewById<EditText>(R.id.etStepNotes)

        etName.setText(selectedStep.name)
        val zielZeitInMinutes = selectedStep.zielZeitInSeconds / 60
        etZielZeit.setText(if (zielZeitInMinutes > 0) zielZeitInMinutes.toString() else "")
        etNotes.setText(selectedStep.notes)
        etName.setSelection(etName.text.length)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Step bearbeiten")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isBlank()) {
                    Toast.makeText(this, "Name darf nicht leer sein.", Toast.LENGTH_SHORT).show()
                } else {
                    val newZielZeitInMinutes = etZielZeit.text.toString().toLongOrNull() ?: 0
                    val newNotes = etNotes.text.toString().trim()

                    val updatedStep = selectedStep.copy(
                        name = newName,
                        zielZeitInSeconds = newZielZeitInMinutes * 60,
                        notes = newNotes
                    )
                    gtdViewModel.updateStep(updatedStep)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .create()

        showKeyboardInDialog(dialog, etName)
        dialog.show()
    }

    private fun showDeleteStepDialog() {
        val selectedStep = adapter.getSelectedStep() ?: run {
            Toast.makeText(this, "Bitte zuerst einen Step auswählen.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Step löschen")
            .setMessage("Soll \"${selectedStep.name}\" wirklich gelöscht werden?")
            .setPositiveButton("Löschen") { _, _ ->
                gtdViewModel.deleteStep(selectedStep)
                adapter.clearSelection()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun showResetDurationDialog(step: Step) {
        AlertDialog.Builder(this)
            .setTitle("Dauer zurücksetzen")
            .setMessage("Soll die Dauer für \"${step.name}\" wirklich auf 0:00:00 gesetzt werden?")
            .setPositiveButton("Zurücksetzen") { _, _ ->
                gtdViewModel.resetStepDuration(step)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.stepsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            val toolbar = findViewById<Toolbar>(R.id.toolbarSteps)
            toolbar.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun showKeyboardInDialog(dialog: AlertDialog, input: EditText) {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.setOnShowListener {
            input.requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
