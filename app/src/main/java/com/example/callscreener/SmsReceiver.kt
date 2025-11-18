package com.example.callscreener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            val pdus = bundle["pdus"] as? Array<*>
            val format = bundle["format"] as? String
            if (pdus != null) {
                for (pdu in pdus) {
                    val sms = SmsMessage.createFromPdu(pdu as ByteArray, format)
                    val from = sms.originatingAddress ?: "unknown"
                    val body = sms.messageBody

                    val channelId = "sms_reply_channel"
                    val nm = NotificationManagerCompat.from(context)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val ch = android.app.NotificationChannel(channelId, "Caller Replies", android.app.NotificationManager.IMPORTANCE_HIGH)
                        nm.createNotificationChannel(ch)
                    }

                    val builder = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.sym_action_chat)
                        .setContentTitle("Reply from $from")
                        .setContentText(body)
                        .setAutoCancel(true)

                    nm.notify(("sms_$from").hashCode(), builder.build())
                }
            }
        }
    }
}
