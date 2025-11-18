package com.example.callscreener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_REJECT = "com.example.callscreener.REJECT"
        const val ACTION_ASK_PURPOSE = "com.example.callscreener.ASK_PURPOSE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra("number") ?: return

        when (intent.action) {
            ACTION_REJECT -> {
                Toast.makeText(context, "Reject requested for $number (screening)", Toast.LENGTH_SHORT).show()
            }
            ACTION_ASK_PURPOSE -> {
                sendAskSMS(context, number)
                Toast.makeText(context, "SMS asking for purpose sent to $number", Toast.LENGTH_SHORT).show()
            }
        }

        NotificationManagerCompat.from(context).cancel(number.hashCode())
    }

    private fun sendAskSMS(context: Context, number: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val message = "Hello, I missed your call. Please reply with your name and purpose of the call. — (Your Name)"
            smsManager.sendTextMessage(number, null, message, null, null)
        } catch (ex: Exception) {
            Toast.makeText(context, "SMS send failed: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }
}
