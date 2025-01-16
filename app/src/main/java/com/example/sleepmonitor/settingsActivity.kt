package com.example.sleepmonitor

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var thresholdSeekBar: SeekBar
    private lateinit var thresholdValueText: TextView
    private lateinit var mobileNumberInput: EditText
    private lateinit var alarmDurationInput: EditText
    private lateinit var saveButton: Button

    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        // Initialize views
        thresholdSeekBar = findViewById(R.id.thresholdSeekBar)
        thresholdValueText = findViewById(R.id.thresholdValueText)
        mobileNumberInput = findViewById(R.id.mobileNumberInput)
        alarmDurationInput = findViewById(R.id.alarmDurationInput)
        saveButton = findViewById(R.id.saveButton)

        // Initialize Room database
        appDatabase = AppDatabase.getDatabase(this)

        // Set default values
        loadSettings()

        // Update threshold text dynamically
        thresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val threshold = progress / 100.0 // Convert to decimal
                thresholdValueText.text = "Threshold: %.2f".format(threshold)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Save settings on button click
        saveButton.setOnClickListener {
            val mobileNumber = mobileNumberInput.text.toString()
            val alarmDuration = alarmDurationInput.text.toString().toIntOrNull() ?: 10
            val threshold = thresholdSeekBar.progress / 100.0

            if (mobileNumber.isEmpty()) {
                Toast.makeText(this, "Please enter a mobile number", Toast.LENGTH_SHORT).show()
            } else {
                // Save values to the database
                saveSettings(threshold, mobileNumber, alarmDuration)
            }
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            val settings = appDatabase.settingsDao().getSettings()
            if (settings != null) {
                thresholdSeekBar.progress = (settings.threshold * 100).toInt() // Convert back to int
                thresholdValueText.text = "Threshold: %.2f".format(settings.threshold)
                mobileNumberInput.setText(settings.mobileNumber)
                alarmDurationInput.setText(settings.alarmDuration.toString())
            } else {
                // Insert default values if no settings are found
                val defaultSettings = Settings(threshold = 0.2, mobileNumber = "", alarmDuration = 10)
                appDatabase.settingsDao().insertSettings(defaultSettings)
            }
        }
    }

    private fun saveSettings(threshold: Double, mobileNumber: String, alarmDuration: Int) {
        lifecycleScope.launch {
            val settings = Settings(id = 1, threshold = threshold, mobileNumber = mobileNumber, alarmDuration = alarmDuration)
            appDatabase.settingsDao().updateSettings(settings)
            Toast.makeText(this@SettingsActivity, "Settings Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle the back button on the AppBar
    override fun onSupportNavigateUp(): Boolean {
        finish() // Close activity
        return true
    }
}