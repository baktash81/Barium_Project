package com.example.smpp_shak_bak

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.CellInfo
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.example.smpp_shak_bak.PUtil.Companion.decryptText
import com.example.smpp_shak_bak.PUtil.Companion.encryptText
import com.example.smpp_shak_bak.PUtil.Companion.hashText

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity(), LocationListener {

    // SMS sending variables
    private var isSendPending = false
    private var recipientNumber = ""
    private var messageText = ""

    // SMS receiving variables
    private var isReceivePending = false
    private var receivedNumber = ""
    private var receivedText = ""

    // Status flags
    private var isSMSSent = false
    private val smsPermission: String = Manifest.permission.RECEIVE_SMS
    private val smsRequestCode: Int = 2

    // Location and telephony managers
    private lateinit var locationHandler: Handler
    private lateinit var locationRunnable: Runnable
    // Threshold for signal strength
    private var signalStrengthThreshold = 2.5
    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var isMonitoringActive = false
    private val locationUpdateInterval = 5000 // 5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        val numberEditText = findViewById<EditText>(R.id.number_input)
        val locationEditText = findViewById<EditText>(R.id.loc_input)
        val signalStrengthEditText = findViewById<EditText>(R.id.value_input)
        val cellInfoTextView: TextView = findViewById(R.id.cell_info_input)
        val smppClient = Smpp(this)

        // Initialize location and telephony managers
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request necessary permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
                smsRequestCode)
        }

        // Thread to handle SMS sending
        val smsSendingThread = Thread {
            try {
                while (true) {
                    if (isSendPending) {
                        isSMSSent = smppClient.sendSMS(recipientNumber, messageText)
                        isSendPending = false

                        runOnUiThread {
                            if (isSMSSent)
                                displaySendStatus(true, false)
                            else
                                displaySendStatus(false, true)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        smsSendingThread.start()

        // Thread to handle SMS receiving
        val smsReceivingThread = Thread {
            try {
                while (true) {
                    if (isReceivePending) {
                        Log.v("HASH receivedText", receivedText)
                        val hashedText = hashText(receivedText)
                        val ackText = "$hashedText | SHAKBAK"

                        smppClient.sendSMS(receivedNumber, ackText)
                        isReceivePending = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        smsReceivingThread.start()

        // Settings button click listener
        val settingsButton = findViewById<ImageButton>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }


        // Load encryption key from shared preferences
        val sharedPref = this.getSharedPreferences("gateway_config", MODE_PRIVATE)
        val encryptionKey = sharedPref.getString("key", "")

        // Send button click listener
        val sendButton = findViewById<Button>(R.id.send_button)
        sendButton.setOnClickListener {
            displaySendStatus(false, false)
            displayAckStatus(false, false)
            recipientNumber = numberEditText.text.toString()
            val location = locationEditText.text.toString()
            val value = signalStrengthEditText.text.toString()
            val cell = cellInfoTextView.text.toString()

            messageText = "LOC $location \n VAL $value \n CINFO $cell"
            isSendPending = true
        }

        // Encrypt button click listener
        val encryptButton = findViewById<Button>(R.id.crypt_button)
        encryptButton.setOnClickListener {
            val encryptedTextView = findViewById<TextView>(R.id.crypt_text)
            encryptedTextView.text = encryptText(messageText, encryptionKey.toString())
        }

        // Decrypt button click listener
        val decryptButton = findViewById<Button>(R.id.decrypt_button)
        decryptButton.setOnClickListener {
            val decryptedTextView = findViewById<TextView>(R.id.decrypt_text)
            decryptedTextView.text = decryptText(messageText, encryptionKey.toString())
        }

        // Hash button click listener
        val hashButton = findViewById<Button>(R.id.hash_button)
        hashButton.setOnClickListener {
            val hashedTextView = findViewById<TextView>(R.id.hash_text)
            hashedTextView.text = hashText(messageText)
        }


        // Start monitoring button click listener
        val startMonitoringButton = findViewById<Button>(R.id.start_id)
        startMonitoringButton.setOnClickListener {
            startMonitoring()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Update received SMS status
        isReceivePending = intent.getBooleanExtra("isReceive", false)
        receivedNumber = intent.getStringExtra("phoneNumber").toString()
        receivedText = intent.getStringExtra("text").toString()

        // Update acknowledgment status
        val isAckReceived = intent.getBooleanExtra("ackReceive", false)
        val ackNumber = intent.getStringExtra("ackPhoneNumber").toString()
        val ackText = intent.getStringExtra("ackText").toString()
        if (isAckReceived) {
            val expectedHash = hashText(messageText)
            displaySendStatus(true, false)

            if (expectedHash == ackText && ackNumber == recipientNumber)
                displayAckStatus(true, false)
            else
                displayAckStatus(false, true)
        }
    }

    private fun startMonitoring() {
        if (!isMonitoringActive) {
            isMonitoringActive = true
            Toast.makeText(this@MainActivity, "Updating...", Toast.LENGTH_SHORT).show()

            // Initialize handler and runnable for periodic updates
            locationHandler = Handler(Looper.getMainLooper())
            locationRunnable = object : Runnable {
                override fun run() {
                    Toast.makeText(this@MainActivity, "Updating...", Toast.LENGTH_SHORT).show()
                    updateInfo()
                    locationHandler.postDelayed(this, locationUpdateInterval.toLong())
                }
            }

            // Start periodic updates
            locationHandler.post(locationRunnable)
        }
    }

    private fun updateInfo() {
        // Get and update location information
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val locationEditText = findViewById<EditText>(R.id.loc_input)
                        locationEditText.setText("Lat: ${location.latitude}, Lon: ${location.longitude}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Failed to get location", e)
                }
        }

        // Get and update signal strength
        telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        // Get and update cell info
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            val cellInfo = telephonyManager.allCellInfo
            val cellInfoTextView: TextView = findViewById(R.id.cell_info_input)
            cellInfo?.let {
                val cellInfoString = it.joinToString(separator = "\n") { cell: CellInfo -> cell.toString() }
                cellInfoTextView.text = cellInfoString
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        val locationEditText = findViewById<EditText>(R.id.loc_input)
        locationEditText.setText("Lat: ${location.latitude}, Lon: ${location.longitude}")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.smsRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startMonitoring()
        }
    }


    private fun displayAckStatus(isSuccess: Boolean, isFailure: Boolean) {
        val successAckTextView = findViewById<TextView>(R.id.status_sa_textview)
        val failureAckTextView = findViewById<TextView>(R.id.status_fa_textview)

        successAckTextView.visibility = if (isSuccess) View.VISIBLE else View.INVISIBLE
        failureAckTextView.visibility = if (isFailure) View.VISIBLE else View.INVISIBLE
    }

    private fun displaySendStatus(isSuccess: Boolean, isFailure: Boolean) {
        val successSendTextView = findViewById<TextView>(R.id.status_sm_textview)
        val failureSendTextView = findViewById<TextView>(R.id.status_fm_textview)

        successSendTextView.visibility = if (isSuccess) View.VISIBLE else View.INVISIBLE
        failureSendTextView.visibility = if (isFailure) View.VISIBLE else View.INVISIBLE
    }

    // Override other LocationListener methods
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    // Define PhoneStateListener to get signal strength updates
    private val signalStrengthListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            super.onSignalStrengthsChanged(signalStrength)
            val signalLevel = signalStrength.level
            val signalStrengthEditText = findViewById<EditText>(R.id.value_input)
            signalStrengthEditText.setText(signalLevel.toString())

            if (signalLevel < signalStrengthThreshold) {
                Toast.makeText(this@MainActivity, "Sending to server", Toast.LENGTH_SHORT).show()
                // Click on Send button to notify server
                val sendButton = findViewById<Button>(R.id.send_button)
                sendButton.performClick()
            }
        }
    }
}
