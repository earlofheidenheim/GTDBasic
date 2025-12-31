package com.beispiel.gtdbasic

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beispiel.gtdbasic.model.Step
import com.beispiel.gtdbasic.ui.GtdViewModel
import com.google.android.material.textfield.TextInputEditText

class StepDetailActivity : AppCompatActivity() {

    private val gtdViewModel: GtdViewModel by viewModels()
    private var currentStep: Step? = null

    // UI-Elemente
    private lateinit var etName: EditText
    private lateinit var etNotes: EditText
    private lateinit var tvDurationDisplay: TextInputEditText
    private lateinit var etAveragePulse: TextInputEditText
    private lateinit var etPeakPulse: TextInputEditText
    private lateinit var etAverageLoad: TextInputEditText
    private lateinit var actvFitnessLevel: AutoCompleteTextView
    private lateinit var etRepetitions: TextInputEditText
    private lateinit var etCalories: TextInputEditText
    private lateinit var etDistance: TextInputEditText
    private lateinit var tvLiveTimer: TextView

    // Timer-Logik
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_detail)

        val stepId = intent.getLongExtra("stepId", -1L)
        if (stepId == -1L) {
            finish()
            return
        }

        // UI-Elemente initialisieren
        etName = findViewById(R.id.etStepDetailName)
        etNotes = findViewById(R.id.etStepDetailNotes)
        tvDurationDisplay = findViewById(R.id.tvDurationDisplay)
        etAveragePulse = findViewById(R.id.etAveragePulse)
        etPeakPulse = findViewById(R.id.etPeakPulse)
        etAverageLoad = findViewById(R.id.etAverageLoad)
        actvFitnessLevel = findViewById(R.id.actvFitnessLevel)
        etRepetitions = findViewById(R.id.etRepetitions)
        etCalories = findViewById(R.id.etCalories)
        etDistance = findViewById(R.id.etDistance)
        tvLiveTimer = findViewById(R.id.tvLiveTimer)
        val btnToggleTimer = findViewById<Button>(R.id.btnToggleTimer)
        val btnBack = findViewById<ImageButton>(R.id.btnStepDetailBack)

        setupFitnessLevelDropdown()
        btnBack.setOnClickListener { finish() }

        handleWindowInsets()
        setupFocusListeners()

        gtdViewModel.getStepById(stepId).observe(this) { step ->
            if (step == null) {
                finish()
                return@observe
            }
            currentStep = step

            updateTextFields(step)

            // Timer-Status und UI aktualisieren
            if (step.isRunning) {
                tvLiveTimer.visibility = View.VISIBLE
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                startTimer(step)
            } else {
                tvLiveTimer.visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                stopTimer()
            }

            btnToggleTimer.text = if (step.isRunning) "Stop" else "Start"
        }

        btnToggleTimer.setOnClickListener {
            currentStep?.let {
                gtdViewModel.togglePlayPause(it)
            }
        }
    }

    private fun updateTextFields(step: Step) {
        if (!etName.isFocused && etName.text.toString() != step.name) {
            etName.setText(step.name)
        }
        if (!etNotes.isFocused && etNotes.text.toString() != step.notes) {
            etNotes.setText(step.notes)
        }
        
        tvDurationDisplay.setText(formatDuration(step.dauerInSeconds))

        if (!etAveragePulse.isFocused && etAveragePulse.text.toString() != step.averagePulse.toString()) {
            etAveragePulse.setText(if (step.averagePulse > 0) step.averagePulse.toString() else "")
        }
        if (!etPeakPulse.isFocused && etPeakPulse.text.toString() != step.peakPulse.toString()) {
            etPeakPulse.setText(if (step.peakPulse > 0) step.peakPulse.toString() else "")
        }
        if (!etAverageLoad.isFocused && etAverageLoad.text.toString() != step.averageLoad) {
            etAverageLoad.setText(step.averageLoad)
        }
        if (actvFitnessLevel.text.toString() != step.fitnessLevel.toString()) {
            actvFitnessLevel.setText(step.fitnessLevel.toString(), false)
        }
        if (!etRepetitions.isFocused && etRepetitions.text.toString() != step.repetitions.toString()) {
            etRepetitions.setText(if (step.repetitions > 0) step.repetitions.toString() else "")
        }
        if (!etCalories.isFocused && etCalories.text.toString() != step.calories.toString()) {
            etCalories.setText(if (step.calories > 0) step.calories.toString() else "")
        }
        if (!etDistance.isFocused && etDistance.text.toString() != step.distanceMeters.toString()) {
            etDistance.setText(if (step.distanceMeters > 0) step.distanceMeters.toString() else "")
        }
    }

    private fun startTimer(step: Step) {
        stopTimer() // Sicherstellen, dass kein alter Timer läuft
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsedMillis = System.currentTimeMillis() - step.startTimeMillis
                val totalSeconds = step.dauerInSeconds + (elapsedMillis / 1000)
                tvLiveTimer.text = formatDuration(totalSeconds)
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        if (::timerRunnable.isInitialized) {
            timerHandler.removeCallbacks(timerRunnable)
        }
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun setupFitnessLevelDropdown() {
        val fitnessLevels = (1..6).map { it.toString() }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fitnessLevels)
        actvFitnessLevel.setAdapter(adapter)
    }

    private fun setupFocusListeners() {
        val focusListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                saveChanges()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
        etName.onFocusChangeListener = focusListener
        etNotes.onFocusChangeListener = focusListener
        etAveragePulse.onFocusChangeListener = focusListener
        etPeakPulse.onFocusChangeListener = focusListener
        etAverageLoad.onFocusChangeListener = focusListener
        etRepetitions.onFocusChangeListener = focusListener
        etCalories.onFocusChangeListener = focusListener
        etDistance.onFocusChangeListener = focusListener
        actvFitnessLevel.onFocusChangeListener = focusListener

        findViewById<View>(R.id.stepDetailRoot).setOnClickListener {
            it.requestFocus()
        }

        findViewById<View>(R.id.stepDetailRoot).setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus){
                currentFocus?.clearFocus()
            }
        }
    }

    private fun handleWindowInsets() {
        val root = findViewById<View>(R.id.stepDetailRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
        saveChanges()
    }

    override fun finish() {
        saveChanges()
        super.finish()
    }

    private fun saveChanges() {
        val stepToSave = currentStep ?: return

        val newName = etName.text.toString().trim()
        val newNotes = etNotes.text.toString().trim()
        val newAvgPulse = etAveragePulse.text.toString().toIntOrNull() ?: 0
        val newPeakPulse = etPeakPulse.text.toString().toIntOrNull() ?: 0
        val newAvgLoad = etAverageLoad.text.toString().trim()
        val newFitnessLevel = actvFitnessLevel.text.toString().toIntOrNull() ?: 1
        val newRepetitions = etRepetitions.text.toString().toIntOrNull() ?: 0
        val newCalories = etCalories.text.toString().toIntOrNull() ?: 0
        val newDistance = etDistance.text.toString().toIntOrNull() ?: 0

        if (newName.isBlank()) {
            etName.setText(stepToSave.name) // Verhindert das Speichern eines leeren Namens
            return
        }

        // Das Feld exerciseDurationMinutes wird nicht mehr vom Benutzer eingegeben,
        // sondern spiegelt dauerInSeconds wider. Wir müssen es hier nicht mehr speichern.
        val updatedStep = stepToSave.copy(
            name = newName,
            notes = newNotes,
            averagePulse = newAvgPulse,
            peakPulse = newPeakPulse,
            averageLoad = newAvgLoad,
            fitnessLevel = newFitnessLevel,
            repetitions = newRepetitions,
            calories = newCalories,
            distanceMeters = newDistance
        )

        if (updatedStep != stepToSave) {
            gtdViewModel.updateStep(updatedStep)
        }
    }
}