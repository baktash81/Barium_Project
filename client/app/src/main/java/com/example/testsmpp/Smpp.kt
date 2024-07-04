package com.example.smpp_shak_bak

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.cloudhopper.commons.charset.CharsetUtil
import com.cloudhopper.smpp.SmppBindType
import com.cloudhopper.smpp.SmppSession
import com.cloudhopper.smpp.SmppSessionConfiguration
import com.cloudhopper.smpp.impl.DefaultSmppClient
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler
import com.cloudhopper.smpp.pdu.DeliverSm
import com.cloudhopper.smpp.pdu.PduRequest
import com.cloudhopper.smpp.pdu.PduResponse
import com.cloudhopper.smpp.pdu.SubmitSm
import com.cloudhopper.smpp.type.Address
import com.cloudhopper.smpp.type.SmppBindException
import com.cloudhopper.smpp.type.SmppChannelException
import com.cloudhopper.smpp.type.SmppInvalidArgumentException
import com.cloudhopper.smpp.type.SmppTimeoutException
import com.cloudhopper.smpp.type.UnrecoverablePduException
import java.util.concurrent.TimeUnit

class Smpp(private val context: Context) : DefaultSmppSessionHandler() {

    private var rSession: SmppSession? = null

    // Function to send SMS using SMPP
    fun sendSMS(number: String, text: String): Boolean {
        try {
            val client = DefaultSmppClient()
            val session = client.bind(getSessionConfig(SmppBindType.TRANSCEIVER))

            val sharedPref = context.getSharedPreferences(
                "gateway_config",
                AppCompatActivity.MODE_PRIVATE
            )

            val key = sharedPref.getString("key", "")
            Log.v("keeeeyyy", text)
            val enText = PUtil.encryptText(text, key.toString())

            val sm = createSubmitSm("9891211", "98$number", "$$$enText$$", "UCS-2")

            println("Try to send message")
            session.submit(sm, TimeUnit.SECONDS.toMillis(60))
            Log.v("smpp", "hello")
            println("Message sent")
            println("Wait 10 seconds")

            TimeUnit.SECONDS.sleep(10)

            println("Destroy session")

            session.close()
            session.destroy()

            println("Destroy client")

            client.destroy()

            println("Bye!")

            return true

        } catch (ex: SmppTimeoutException) {
            Log.v("session", "SmppTimeoutException")
        } catch (ex: SmppChannelException) {
            Log.v("session", "SmppChannelException")
        } catch (ex: SmppBindException) {
            Log.v("session", "SmppBindException")
        } catch (ex: UnrecoverablePduException) {
            Log.v("session", "UnrecoverablePduException")
        } catch (ex: InterruptedException) {
            Log.v("session", "InterruptedException")
        }
        return false
    }

    // Function to configure SMPP session
    private fun getSessionConfig(type: SmppBindType): SmppSessionConfiguration? {
        val sessionConfig = SmppSessionConfiguration()
        sessionConfig.type = type

        val sharedPref = context.getSharedPreferences("gateway_config", Context.MODE_PRIVATE)

        sessionConfig.host = sharedPref.getString("host", "")
        sessionConfig.port = sharedPref.getInt("port", 0)
        sessionConfig.systemId = sharedPref.getString("username", "")
        sessionConfig.password = sharedPref.getString("password", "")

        return sessionConfig
    }

    // Function to create SubmitSm for sending SMS
    @Throws(SmppInvalidArgumentException::class)
    fun createSubmitSm(src: String?, dst: String?, text: String?, charset: String?): SubmitSm? {
        val sm = SubmitSm()

        // For alpha numeric will use
        // TON=5
        // NPI=0
        sm.sourceAddress = Address(5.toByte(), 0.toByte(), src)

        // For national numbers will use
        // TON=1
        // NPI=1
        sm.destAddress = Address(1.toByte(), 1.toByte(), dst)

        // Set datacoding to UCS-2
        sm.dataCoding = 8.toByte()

        // Encode text
        sm.shortMessage = CharsetUtil.encode(text, charset)
        return sm
    }

    // Function to bind to SMSC for receiving messages
    fun bindToSMSC() {
        try {
            val client = DefaultSmppClient()
            rSession = client.bind(getSessionConfig(SmppBindType.RECEIVER))
            println("Connected to SMSC...")
            println("Ready to receive PDU...")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Override function to handle received PDU requests
    override fun firePduRequestReceived(pduRequest: PduRequest<*>): PduResponse? {
        val response = pduRequest.createResponse()
        val sms = pduRequest as DeliverSm
        if (sms.dataCoding.toInt() == 0) {
            println("From: " + sms.sourceAddress.address)
            println("To: " + sms.destAddress.address)
            println("Content: " + String(sms.shortMessage))
        }
        return response
    }

    // Function to wait for an exit signal
    private fun waitForExitSignal() {
        rSession?.unbind(0)
    }
}
