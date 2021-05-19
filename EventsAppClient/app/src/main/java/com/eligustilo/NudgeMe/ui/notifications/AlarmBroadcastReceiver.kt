package com.eligustilo.NudgeMe.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmBroadcastReceiver : BroadcastReceiver() {
    private var TAG = "AlarmBroadcastReceiver"

    override fun onReceive(context: Context, intent: Intent) {

        /*Log.d(TAG, intent.getStringExtra("contactsEventDetailsIDKey"))
        Log.d(TAG, intent.getStringExtra("contactsEventDetailsEventDateKey"))
        Log.d(TAG, intent.getStringExtra("contactsEventDetailsEventNameKey"))
        val reminderInt = intent.getIntExtra("contactsEventDetailsEventReminderDateKey", 0)
        Log.d(TAG, "$reminderInt")
        val testLogTHINGS = intent.getIntExtra("contactsEventDetailsEventReminderRemainingKey", 0)
        Log.d(TAG, "$testLogTHINGS")*/

        AlarmManagerService.enqueueWork(context, intent)
    }
}