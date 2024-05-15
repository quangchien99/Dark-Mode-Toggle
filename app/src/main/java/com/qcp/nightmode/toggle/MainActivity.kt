package com.qcp.nightmode.toggle

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.qcp.nightmode.toggle.databinding.ActivityMainBinding

/**
 * @author chienpham
 * @since 15/05/2024
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val FIRST_START = "FIRST_START"
        const val NIGHT_MODE = "NIGHT_MODE"
        const val PREF = "AppSettingsPrefs"
    }

    private lateinit var appSettingsPrefs: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appSettingsPrefs = getSharedPreferences(PREF, 0)

        handleCurrentMode()
        setUpView()
    }

    override fun onResume() {
        super.onResume()
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
    }

    private fun handleCurrentMode() {
        val isNightModeOn: Boolean = appSettingsPrefs.getBoolean(NIGHT_MODE, false)
        // Set checked without animation during initialization
        binding.nightModeToggle.setChecked(isNightModeOn, animate = false)
        val mode =
            if (isNightModeOn) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun setUpView() {
        binding.nightModeToggle.setOnCheckedChangeListener { isNightMode ->
            handleUIMode(isNightMode)
        }
    }

    private fun handleUIMode(isNightMode: Boolean) {
        val currentMode =
            if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(currentMode)
        appSettingsPrefs.edit().apply {
            putBoolean(FIRST_START, false)
            putBoolean(NIGHT_MODE, isNightMode)
            apply()
        }
        if (isNightMode != appSettingsPrefs.getBoolean(NIGHT_MODE, !isNightMode)) {
            recreate()  // Only recreate if there's a change to reduce redundant calls
        }
    }
}