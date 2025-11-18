package com.example.callscreener

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat

class MyCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle?.schemeSpecificPart ?: "unknown"

        val prefs = applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val autoBlock = prefs.getBoolean("auto_block", false)

        // Show notification with Accept/Reject/Ask purpose
        showNotification(applicationContext, incomingNumber)

        // If autoBlock is enabled and caller not in contacts, block
        val shouldBlock = if (autoBlock && !isContact(incomingNumber)) true else false

        val responseBuilder = CallResponse.Builder()
            .setDisallowCall(shouldBlock)
            .setRejectCall(shouldBlock)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)

        respondToCall(callDetails, responseBuilder.build())
    }

    private fun isContact(number: String): Boolean {
        if (number.isEmpty()) return false
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            applicationContext.contentResolver.query(uri, projection, null, null, null).use { cursor ->
                cursor != null && cursor.moveToFirst()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun showNotification(context: Context, number: String) {
        val channelId = "call_screen_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Call Screening", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(ch)
        }

        // Accept: open phone dialer
        val acceptIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        val pAccept = PendingIntent.getActivity(context, 0, acceptIntent, PendingIntent.FLAG_IMMUTABLE)

        // Reject: request the ActionReceiver to handle (note: actual call blocking is done via respondToCall)
        val rejectIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.ACTION_REJECT
            putExtra("number", number)
        }
        val pReject = PendingIntent.getBroadcast(context, 1, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Ask purpose: send SMS template
        val askIntent = Intent(context, ActionReceiver::class.java).apply {
            action = ActionReceiver.ACTION_ASK_PURPOSE
            putExtra("number", number)
        }
        val pAsk = PendingIntent.getBroadcast(context, 2, askIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Incoming: $number")
            .setContentText("Tap Accept / Reject / Ask purpose")
            .addAction(android.R.drawable.sym_action_call, "Accept", pAccept)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", pReject)
            .addAction(android.R.drawable.ic_dialog_email, "Ask purpose", pAsk)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        nm.notify(number.hashCode(), notif)
    }
}
