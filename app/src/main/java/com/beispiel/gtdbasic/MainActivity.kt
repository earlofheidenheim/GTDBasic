package com.beispiel.gtdbasic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beispiel.gtdbasic.model.Project
import com.beispiel.gtdbasic.ui.GtdViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private val gtdViewModel: GtdViewModel by viewModels()
    private lateinit var adapter: ProjectAdapter
    private var categoryList: List<String> = emptyList()
    private var statusList: List<String> = emptyList()
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var statusChipGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        categoryChipGroup = findViewById(R.id.chipGroupCategory)
        statusChipGroup = findViewById(R.id.chipGroupStatus)

        setupDemoMode()
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        setupButtons()
        handleWindowInsets()
    }

    private fun setupDemoMode() {
        val sharedPrefs = getSharedPreferences("gtd_prefs", Context.MODE_PRIVATE)
        val isDemoMode = sharedPrefs.getBoolean("is_demo_mode", false)
        gtdViewModel.setDemoMode(isDemoMode)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbarMain)
        setSupportActionBar(toolbar)

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                return true
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return super.onSingleTapConfirmed(e)
            }
        })

        var tapCount = 0
        var lastTapTime: Long = 0

        toolbar.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < 500) { // 500ms window for multi-tap
                    tapCount++
                } else {
                    tapCount = 1
                }
                lastTapTime = currentTime

                if (tapCount >= 3) {
                    toggleDemoModeAndRestart()
                    tapCount = 0 // Reset counter
                }
            }
            true
        }
    }

    private fun toggleDemoModeAndRestart() {
        val sharedPrefs = getSharedPreferences("gtd_prefs", Context.MODE_PRIVATE)
        val isCurrentlyDemo = sharedPrefs.getBoolean("is_demo_mode", false)
        sharedPrefs.edit().putBoolean("is_demo_mode", !isCurrentlyDemo).commit() // Use commit() for synchronous save

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    private fun setupRecyclerView() {
        val rvProjects = findViewById<RecyclerView>(R.id.rvProjects)
        rvProjects.layoutManager = LinearLayoutManager(this)

        adapter = ProjectAdapter { project, isReselection ->
            if (isReselection) {
                val intent = Intent(this, ProjectStepsActivity::class.java)
                intent.putExtra("projectId", project.id)
                intent.putExtra("projectName", project.name)
                startActivity(intent)
            }
        }
        rvProjects.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
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

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                gtdViewModel.updateProjectSortOrder(adapter.currentList)
            }
        })
        itemTouchHelper.attachToRecyclerView(rvProjects)
    }

    private fun observeViewModel() {
        gtdViewModel.projects.observe(this, Observer { projects ->
            projects?.let { adapter.submitList(it) }
        })

        gtdViewModel.allCategories.observe(this) { categories ->
            updateChips(categoryChipGroup, categories) { category ->
                gtdViewModel.setCategoryFilter(category)
            }
        }

        gtdViewModel.allStatuses.observe(this) { statuses ->
            updateChips(statusChipGroup, statuses) { status ->
                gtdViewModel.setStatusFilter(status)
            }
        }

        gtdViewModel.allCategoryNames.observe(this) { categories ->
            this.categoryList = categories
        }

        gtdViewModel.allStatusNames.observe(this) { statuses ->
            this.statusList = statuses
        }

        lifecycleScope.launch {
            gtdViewModel.isDemoMode.collectLatest { isDemo ->
                if (isDemo) {
                    supportActionBar?.title = "Projects DEMO"
                } else {
                    supportActionBar?.title = "Projects"
                }
            }
        }
    }

    private fun updateChips(chipGroup: ChipGroup, items: List<String>, onFilter: (String?) -> Unit) {
        chipGroup.removeAllViews()

        val allChip = Chip(this)
        allChip.text = "Alle"
        allChip.isCheckable = true
        allChip.isChecked = true
        allChip.id = ViewCompat.generateViewId()
        chipGroup.addView(allChip)

        for (item in items) {
            val chip = Chip(this)
            chip.text = item
            chip.isCheckable = true
            chip.id = ViewCompat.generateViewId()
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedChip = group.findViewById<Chip>(checkedId)
            if (selectedChip != null) {
                val selectedItem = if (selectedChip.text == "Alle") null else selectedChip.text.toString()
                onFilter(selectedItem)
            } else {
                onFilter(null)
                val firstChip = group.getChildAt(0) as? Chip
                firstChip?.isChecked = true
            }
        }
    }

    private fun setupButtons() {
        val btnNew = findViewById<Button>(R.id.btnNewProject)
        val btnEdit = findViewById<Button>(R.id.btnEditProject)
        val btnDelete = findViewById<Button>(R.id.btnDeleteProject)

        btnNew.setOnClickListener { showNewProjectDialog() }

        btnEdit.setOnClickListener {
            val selected = adapter.getSelectedProject()
            if (selected == null) {
                Toast.makeText(this, "Bitte zuerst ein Project auswählen.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showEditProjectDialog(selected)
        }

        btnDelete.setOnClickListener {
            val selected = adapter.getSelectedProject()
            if (selected == null) {
                Toast.makeText(this, "Bitte zuerst ein Project auswählen.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showDeleteConfirmationDialog(selected)
        }
    }

    private fun showNewProjectDialog() {
        val input = EditText(this)
        input.hint = "Project-Name"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Neues Project")
            .setView(input)
            .setPositiveButton("Speichern") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    val newSortOrder = adapter.itemCount + 1
                    val newProject = Project(id = 0, name = name, sortOrder = newSortOrder)
                    gtdViewModel.insertProject(newProject)
                } else {
                    Toast.makeText(this, "Name darf nicht leer sein.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .create()

        showKeyboardInDialog(dialog, input)
        dialog.show()
    }

    private fun showEditProjectDialog(project: Project) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_project, null)
        val etName = dialogView.findViewById<EditText>(R.id.etProjectName)
        val actvKategorie = dialogView.findViewById<AutoCompleteTextView>(R.id.actvProjectKategorie)
        val actvStatus = dialogView.findViewById<AutoCompleteTextView>(R.id.actvProjectStatus)
        val etNotes = dialogView.findViewById<EditText>(R.id.etProjectNotes)

        etName.setText(project.name)
        val oldCategory = project.kategorie
        val oldStatus = project.status
        actvKategorie.setText(oldCategory)
        actvStatus.setText(oldStatus)
        etNotes.setText(project.notes)
        etName.setSelection(etName.text.length)

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryList)
        actvKategorie.setAdapter(categoryAdapter)

        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statusList)
        actvStatus.setAdapter(statusAdapter)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Project bearbeiten")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isBlank()) {
                    Toast.makeText(this, "Name darf nicht leer sein.", Toast.LENGTH_SHORT).show()
                } else {
                    val newCategory = actvKategorie.text.toString().trim()
                    val newStatus = actvStatus.text.toString().trim()
                    val newNotes = etNotes.text.toString().trim()

                    handleItemRename(oldCategory, newCategory, "Kategorie") { gtdViewModel.renameCategory(oldCategory, newCategory) }
                    handleItemRename(oldStatus, newStatus, "Status") { gtdViewModel.renameStatus(oldStatus, newStatus) }

                    val updatedProject = project.copy(
                        name = newName,
                        kategorie = newCategory,
                        status = newStatus,
                        notes = newNotes
                    )
                    gtdViewModel.updateProject(updatedProject)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .create()

        showKeyboardInDialog(dialog, etName)
        dialog.show()
    }

    private fun handleItemRename(oldName: String, newName: String, itemName: String, renameAction: () -> Unit) {
        val hasChanged = oldName != newName && oldName.isNotBlank()
        if (hasChanged) {
            AlertDialog.Builder(this)
                .setTitle("$itemName umbenennen")
                .setMessage("Soll '$oldName' für alle Projekte in '$newName' umbenannt werden?")
                .setPositiveButton("Alle umbenennen") { _, _ -> renameAction() }
                .setNegativeButton("Nur dieses", null)
                .setNeutralButton("Abbrechen", null)
                .show()
        }
    }


    private fun showDeleteConfirmationDialog(project: Project) {
        AlertDialog.Builder(this)
            .setTitle("Project löschen")
            .setMessage("Soll \"${project.name}\" wirklich gelöscht werden?")
            .setPositiveButton("Löschen") { _, _ ->
                gtdViewModel.deleteProject(project)
                adapter.clearSelection()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun handleWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            val toolbar = findViewById<Toolbar>(R.id.toolbarMain)
            toolbar.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun showKeyboardInDialog(dialog: AlertDialog, input: EditText) {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.setOnShowListener {
            input.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}
