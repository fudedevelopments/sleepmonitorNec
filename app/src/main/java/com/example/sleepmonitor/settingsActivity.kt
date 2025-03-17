package com.example.sleepmonitor
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var thresholdInput: EditText
    private lateinit var mobileNumberInput: EditText
    private lateinit var alarmDurationInput: EditText
    private lateinit var saveButton: Button
    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        // Set up toolbar
        supportActionBar?.apply {
            title = "Settings"
            setDisplayHomeAsUpEnabled(true)
        }

        // Initialize views
        thresholdInput = findViewById(R.id.thresholdInput)
        mobileNumberInput = findViewById(R.id.mobileNumberInput)
        alarmDurationInput = findViewById(R.id.alarmDurationInput)
        saveButton = findViewById(R.id.saveButton)

        // Initialize Room database
        appDatabase = AppDatabase.getDatabase(this)

        // Set default values
        loadSettings()

        // Save settings on button click
        saveButton.setOnClickListener {
            val mobileNumber = mobileNumberInput.text.toString().trim()
            val alarmDuration = alarmDurationInput.text.toString().toIntOrNull() ?: 10
            val threshold = thresholdInput.text.toString().toIntOrNull()

            when {
                mobileNumber.isEmpty() -> {
                    showToast("Please enter a mobile number")
                    mobileNumberInput.requestFocus()
                }
                threshold == null -> {
                    showToast("Please enter a valid threshold value")
                    thresholdInput.requestFocus()
                }
                threshold < 0 -> {
                    showToast("Threshold must be 0 or greater")
                    thresholdInput.requestFocus()
                }
                alarmDuration < 0 -> {
                    showToast("Alarm duration must be 0 or greater")
                    alarmDurationInput.requestFocus()
                }
                else -> {
                    // Print values for debugging
                    println("Saving settings:")
                    println("Threshold: $threshold")
                    println("Mobile Number: $mobileNumber")
                    println("Alarm Duration: $alarmDuration")

                    // Save values to the database
                    saveSettings(threshold, mobileNumber, alarmDuration)
                }
            }
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            val settings = appDatabase.settingsDao().getSettings()
            if (settings != null) {
                thresholdInput.setText(settings.threshold.toString())
                mobileNumberInput.setText(settings.mobileNumber)
                alarmDurationInput.setText(settings.alarmDuration.toString())
            } else {
                // Insert default values if no settings are found
                val defaultSettings = Settings(threshold = 10, mobileNumber = "", alarmDuration = 10)
                appDatabase.settingsDao().insertSettings(defaultSettings)
                thresholdInput.setText("10")
                alarmDurationInput.setText("10")
            }
        }
    }

    private fun saveSettings(threshold: Int, mobileNumber: String, alarmDuration: Int) {
        lifecycleScope.launch {
            val settings = Settings(id = 1, threshold = threshold, mobileNumber = mobileNumber, alarmDuration = alarmDuration)
            appDatabase.settingsDao().updateSettings(settings)
            showToast("Settings saved successfully!")
            finish() // Close the activity after saving
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}