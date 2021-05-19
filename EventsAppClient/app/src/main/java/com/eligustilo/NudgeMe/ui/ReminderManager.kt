package com.eligustilo.NudgeMe.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.eligustilo.NudgeMe.DataManager
import com.eligustilo.NudgeMe.EventsDetails
import com.eligustilo.NudgeMe.MainActivity
import com.eligustilo.NudgeMe.Reminders
import com.eligustilo.NudgeMe.ui.notifications.AlarmBroadcastReceiver
import org.json.JSONObject
import java.util.*


data class TestReminder(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int
)

//this class is only for reminders atm
class ReminderManager(val reminderManagerContext: Context) {
    private val TAG = "ReminderManager"
//    var hashMapReminders = HashMap< String, Int>()
    var jsonRemindersObject = JSONObject()
    var pendingIntentHashValueKey = 0
    private var JSON_OBJECT_KEY = "ReminderManager"
    var testReminder: TestReminder? = null

    init {
        readUserDefaults()
    }

    fun isReminderEnabled(reminder: Reminders): Boolean {
        if(jsonRemindersObject.has(reminder.reminderID)) {
            val pendingIntentAdjustedID = jsonRemindersObject.getInt(reminder.reminderID)
            val notifyIntent = Intent(reminderManagerContext, AlarmBroadcastReceiver::class.java)
            val isWorking =
                PendingIntent.getBroadcast(
                    reminderManagerContext,
                    pendingIntentAdjustedID,
                    notifyIntent,
                    PendingIntent.FLAG_NO_CREATE
                ) != null //just changed the flag

            return isWorking
        }
        return false
    }

    fun clearAlarmsForEvent(eventId: String) {
        val friendsData = DataManager.getParsedData()
        if(friendsData != null) {
            for (friend in friendsData) {
                for(event in friend.userEvents) {
                    if(event.EventId == eventId) {
                        for(reminder in event.ReminderArray) {
                            this.turnOffReminder(reminder)
                        }
                    }
                }
            }
        }
    }

    fun turnOffReminder(reminder: Reminders) {
        val alarmManager: AlarmManager = reminderManagerContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntentAdjustedID = jsonRemindersObject.getInt(reminder.reminderID)
        val notifyIntent = Intent(reminderManagerContext, AlarmBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            reminderManagerContext,
            pendingIntentAdjustedID,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "this reminder was deleted $pendingIntent")
    }

    fun turnOnReminder(reminder: Reminders, event: EventsDetails) {
        val alarmManager: AlarmManager = reminderManagerContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        //create a new intent that calls the BroactCastReciever
        val notifyIntent = Intent(reminderManagerContext, AlarmBroadcastReceiver::class.java)

        //creates keys linked to the array database to grab specific data pieces from the arrayList
        notifyIntent.putExtra("contactsEventDetailsIDKey", event.EventId)
        notifyIntent.putExtra("contactsEventDetailsEventDateKey", event.EventDate)
        notifyIntent.putExtra("contactsEventDetailsEventNameKey", event.EventName)
        notifyIntent.putExtra("contactsEventDetailsEventReminderDateKey", reminder.toString())
        //create a pending intent based on the notifyIntent and places it in a hashmap//TODO hashvalue and placement of hashmap needs to be learned better.

        var currentPendingIntentAjustedID = this.pendingIntentHashValueKey
        if(jsonRemindersObject.has(reminder.reminderID)) {
            currentPendingIntentAjustedID = jsonRemindersObject.getInt(reminder.reminderID)
        } else {
            this.pendingIntentHashValueKey = this.pendingIntentHashValueKey + 1
            jsonRemindersObject.put(reminder.reminderID, pendingIntentHashValueKey)
            saveUserDefaults()
            currentPendingIntentAjustedID = this.pendingIntentHashValueKey
        }

        val pendingIntent = PendingIntent.getBroadcast(
            reminderManagerContext,
            currentPendingIntentAjustedID,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        //sets the system ALARM_Service to milisecond time. and Executes the pendingIntent, aka the alarm.
        val timeReminder = getReminderOffset(event.EventDate, reminder.reminder)
        Log.d(TAG, "${timeReminder}")
        alarmManager.set(AlarmManager.RTC_WAKEUP, timeReminder, pendingIntent)
        Log.d(TAG, "the set intent for the reminder is $pendingIntent")
    }

    fun syncReminder(eventArray: ArrayList<EventsDetails>) {//TODO loop through all users to add multi user functionality.
        for (event in eventArray){
            val remindersToCheckArray = event.ReminderArray
            Log.d(TAG, "this is the event that is being synced $event")
            for (reminder in remindersToCheckArray){//TODO how to handle if remindersToCheckArray is empty
                if(!isReminderEnabled(reminder)) {
                    this.turnOnReminder(reminder, event)
                }
            }
        }
    }

    fun displayNotification(titleOfNotification: String, messageOfNotification: String) {
        //boilerplate code
        val displayNotificationManager = reminderManagerContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(reminderManagerContext, MainActivity::class.java)//calls main activity when notification popup is clicked.

         //this builds a recipe. Builds a pending intent that can be used later
        val stackBuilder = TaskStackBuilder.create(reminderManagerContext)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(notificationIntent)//TODO can I change to different fragments from the notificationIntent? How exactly does it work.
        val notificationPendingIntent = stackBuilder.getPendingIntent(
            0,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

         //this sets the icon, title, text of the notification and applies it to a pending intent/recipe
        val builder = NotificationCompat.Builder(reminderManagerContext)//TODO need to find out non depreciated way to build this. .builder
        builder.setSmallIcon(com.eligustilo.NudgeMe.R.drawable.icon_v3)
            .setContentTitle(titleOfNotification)
            .setContentText(messageOfNotification)
            .setContentIntent(notificationPendingIntent)

         //if checks for version is later than version x of os. Have to have this or notification don't work. Needs to be a modern SDK. If it is a modern SDK provides channel necessary.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ANDROID_CHANNEL_ID = "com.eligustilo.eventsapp.NOTIFICATION_ID"

            val name: CharSequence = "NudgeMe" //This is the name of the notifiaction
            val channel = NotificationChannel(
                ANDROID_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            )
            displayNotificationManager.createNotificationChannel(channel)
            builder.setChannelId(ANDROID_CHANNEL_ID)
        }

        builder.setAutoCancel(true)//this says if we get a duplicate notification on same channel with same title then throw away old one.
        displayNotificationManager.notify(0, builder.build())//this is where the notification is actually built and displayed. ID is a random tag to keep track. not specific
    }

     fun getReminderOffset(eventDateAsString: String, eventReminderOffset: Int): Long {
         //this splits string into 3 arrays for month, day, year
         val dateArray = eventDateAsString.split('-')//TODO this split is breaking things.
         //TODO need to make this handle more date formats

         //this is the date of the event
         val month = dateArray[1].toInt()-1 //java months go 0-11 for 12 total thought this is at array position 0 so should be no problem. Odd.
         val day = dateArray[2].toInt()
         val currentYear = Calendar.getInstance()[Calendar.YEAR]

         Log.d(
             TAG,
             "the date gotten from the contactsEventsDate.split are: month: ${month}, day: ${day}, currentYear: ${currentYear}"
         )

         //gets a calendar object to be used to check event dates
         val eventCalendarDueDate = Calendar.getInstance()

         val testReminder = this.testReminder
         if(testReminder != null) {
             eventCalendarDueDate.set(
                 testReminder.year,
                 testReminder.month,
                 testReminder.day,
                 testReminder.hour,
                 testReminder.minute,
                 testReminder.second
             )
         } else {
             //eventCalendarDueDate.set(currentYear, month, day, 17, 49, 0)
             eventCalendarDueDate.set(currentYear, month, day, 9, 0, 0)

         }
         Log.d(
             TAG,
             "the manually set calendar date to be used to check our event date is : ${eventCalendarDueDate.time}"
         )

         //setting up variables to hold the event reminders
         var nextEventDay = eventCalendarDueDate.time
         val today = Date()
         Log.d(
             TAG,
             "The event reminder variables are today's date of: $today and the nextEventDate of $nextEventDay"
         )

         //checking to see if event is in past or future and adjusting year to match next event date year
         if(nextEventDay.before(today)) {
             if(testReminder != null) {
                 eventCalendarDueDate.set(
                     testReminder.year,
                     testReminder.month,
                     testReminder.day,
                     testReminder.hour,
                     testReminder.minute,
                     testReminder.second
                 )
             } else {
                 eventCalendarDueDate.set(currentYear + 1, month, day, 9, 0, 0)
             }
             nextEventDay = eventCalendarDueDate.time
             Log.d(
                 TAG,
                 "because the nextEventDate is already past we have set it for next years eventDate and that date is: $nextEventDay"
             )
         }

         //this is the actual reminder math. everything is in milliseconds
         val nextEventDayMiliseconds = eventCalendarDueDate.timeInMillis
         Log.d(TAG, "the nextEventDate in milliseconds is: $nextEventDayMiliseconds")

//         val firstReminderTime: Reminders = eventReminderTime.ReminderArray[0]
         val adjustedEventReminderOffset = (eventReminderOffset * 60000).toLong()
         val adjNextEventDayMiliseconds = nextEventDayMiliseconds - adjustedEventReminderOffset
         Log.d(TAG, "the adjustedEventDay in milliseconds is :$adjNextEventDayMiliseconds")

         eventCalendarDueDate.timeInMillis = adjNextEventDayMiliseconds
         val calendarDateOfEventWithReminder1 = eventCalendarDueDate.time
         Log.d(TAG, "The next event with a reminder date is: $calendarDateOfEventWithReminder1")


         val returnMillis = adjNextEventDayMiliseconds //- todayInMillis  AlarmManager is smart enough to cast a reminder notification from the adjNextEventDayMiliseconds
         Log.d(TAG, "the milliseconds until the reminder is due is: ${returnMillis}")

         return returnMillis
    }

    private fun saveUserDefaults(){
        val userDefaults: SharedPreferences = reminderManagerContext.applicationContext.getSharedPreferences(
            JSON_OBJECT_KEY,
            0
        )//TODO reminder_key is linked to just on jsonRemindersAsString? doesn't this need to be a for loop.
        val editor = userDefaults.edit()
        val jsonRemindersAsString = jsonRemindersObject.toString()//TODO jsonReminders is a for loop of all remiders being added to it.
        editor.putString(JSON_OBJECT_KEY, jsonRemindersAsString)
        editor.putInt("pendingIntentKey", this.pendingIntentHashValueKey)
        editor.commit()
    }

    private fun readUserDefaults(){
        val userDefaults: SharedPreferences = reminderManagerContext.applicationContext.getSharedPreferences(
            JSON_OBJECT_KEY,
            0
        )
        if (userDefaults.contains(JSON_OBJECT_KEY)){//TODO shouldn't this be a for loop to go through every reminder key for every jsonReminders for every reminders that got looped?
            val jsonRemindersAsString = userDefaults.getString(JSON_OBJECT_KEY, null)
            if (jsonRemindersAsString != null){
                this.jsonRemindersObject = JSONObject(jsonRemindersAsString)
            }
        }
        if(userDefaults.contains("pendingIntentKey")) {
            this.pendingIntentHashValueKey = userDefaults.getInt("pendingIntentKey", 0)
        }
    }
}