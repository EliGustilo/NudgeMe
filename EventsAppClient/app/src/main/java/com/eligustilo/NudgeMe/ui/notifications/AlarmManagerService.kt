package com.eligustilo.NudgeMe.ui.notifications

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.eligustilo.NudgeMe.ui.ReminderManager

class AlarmManagerService : JobIntentService () {
    private val TAG = "AlarmManagerService"


    companion object {
        //JOB_ID is an arbitrary reference number to the position the task has in android framework
        private const val JOB_ID = 1993
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, AlarmManagerService::class.java, JOB_ID, intent)
        }
    }

    override fun onHandleWork(intent: Intent) {//TODO how is this linked to the ReminderManager.
        val intentEventEventID = intent.getStringExtra("contactsEventDetailsIDKey")
        if (intent.getStringExtra("contactsEventDetailsIDKey") != null){
//            Log.d(TAG, intent.getStringExtra("the intentEventEventID is ${intent.getStringExtra("contactsEventDetailsIDKey")}"))//TODO why can this be null?
        }
        Log.d(TAG, intentEventEventID)
        Log.d(TAG, intent.getStringExtra("contactsEventDetailsEventNameKey"))
        Log.d(TAG, intent.getIntExtra("contactsEventDetailsEventReminderDateKey", 0).toString())
        //TODO Key contactsEventDetailsEventReminderDateKey expected Integer but value was a java.lang.String.  The default value 0 was returned. on notifcation
        val testLogTHINGS = intent.getIntExtra("contactsEventDetailsEventReminderRemainingKey", 0)

        Log.d(TAG, "$testLogTHINGS")

        val eventName = intent.getStringExtra("contactsEventDetailsEventNameKey")
        // TODO: replace with custom message
        val reminderManagerInstance = ReminderManager(applicationContext)
        reminderManagerInstance.displayNotification( "Your event: $eventName is coming up!", "This is your alarm for $eventName")
    }
}