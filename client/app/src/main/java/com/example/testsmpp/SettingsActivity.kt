package com.example.smpp_shak_bak

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.hide()

        val sharedPref = this.getSharedPreferences("gateway_config", MODE_PRIVATE)

        // Retrieve and display saved settings
        val hostEditText = findViewById<EditText>(R.id.editTextHost)
        hostEditText.setText(sharedPref.getString("host", ""))

        val portEditText = findViewById<EditText>(R.id.editTextPort)
        portEditText.setText(sharedPref.getInt("port", 0).toString())

        val usernameEditText = findViewById<EditText>(R.id.editTextUsername)
        usernameEditText.setText(sharedPref.getString("username", ""))

        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        passwordEditText.setText(sharedPref.getString("password", ""))

        val keyEditText = findViewById<EditText>(R.id.editKey)
        keyEditText.setText(sharedPref.getString("key", ""))

        // Set up button to save settings and navigate back to the main activity
        val submitButton = findViewById<Button>(R.id.buttonSave)
        submitButton.setOnClickListener {
            val host = hostEditText.text.toString()
            val port = portEditText.text.toString()
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val key = keyEditText.text.toString()

            saveConfig(host, port, username, password, key)

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Set up back button to finish the current activity
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }
    }

    // Function to save configuration settings
    private fun saveConfig(host: String, port: String, username: String, password: String, key: String) {
        val sharedPref = this.getSharedPreferences("gateway_config", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("host", host)
            putInt("port", port.toInt())
            putString("username", username)
            putString("password", password)
            putString("key", key)
            apply()
        }
    }
}
