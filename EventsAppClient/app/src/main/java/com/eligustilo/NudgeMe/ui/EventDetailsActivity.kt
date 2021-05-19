package com.eligustilo.NudgeMe.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.eligustilo.NudgeMe.*
import kotlinx.android.synthetic.main.event_details.*


class ReminderAdapter(private val context: EventDetailsActivity, val reminderTimesArray: Array<Int?>, val reminderIdArray: Array<String?>, val eventId: String) : ArrayAdapter<Int?>(context,0, reminderTimesArray) {
    private val TAG = "EventDetails"
    private var reminderRemade = Reminders(1440, "Null")

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val reminderTime = reminderTimesArray[position] //Equivilant to simplfiedDataViewHolderNeeds?
        val reminderDay = (reminderTime?.div(1440))
        val reminderId = reminderIdArray[position]
        if (reminderId != null){
            if (reminderTime != null){
                reminderRemade =
                    Reminders(reminderTime, reminderId)
            }
        }

        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.event_recylcer_view_details_cell, null, true)
        val textView = rowView.findViewById<TextView>(R.id.homeRecyclerViewEventNameTextView)
        textView.text = "There is a reminder $reminderDay day before this event."
        rowView.setOnClickListener(){
            if(reminderId != "id_set_by_server") {
                deleteReminderPopup(reminderRemade, eventId)
                DataManager.syncDataWithServer() //TODO make refresh better
            }
        }
        return rowView
    }

    fun deleteReminderPopup(reminderToDelete: Reminders, eventId: String) {
        Log.d(TAG, "Trash got clicked")
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Are you sure you want to delete?")

        builder.setPositiveButton("Yes"){ dialog, which2 ->

            context.eventsReminderTimesArray?.remove(reminderToDelete.reminder)
            context.eventsReminderIdArray?.remove(reminderToDelete.reminderID)
            context.setRemindersAdapter()
            context.updateOneDayButtonState()
            DataManager.deleteReminder(reminderToDelete, eventId)
        }
        builder.setNegativeButton("No", null)
        val dialog = builder.create()
        dialog.show()
    }
}

class EventDetailsActivity: Activity() {
    private val TAG = "EventDetails"
    private lateinit var userNameTextView: TextView
    private lateinit var eventNameTextView: TextView
    private lateinit var eventDateTextView: TextView
    private lateinit var avatarImageView: ImageView
    private lateinit var reminderManager: ReminderManager
    private lateinit var userId: String
    private lateinit var eventId: String
    private lateinit var remindMeOneDay: Button
    private lateinit var doneButton: Button
    private lateinit var eventName: String
    private lateinit var eventDate: String
    private lateinit var eventOwnerName: String
    var eventsReminderTimesArray: ArrayList<Int>? = null
    var eventsReminderIdArray: ArrayList<String>? = null
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.event_details)
        val userName = intent.getStringExtra("userName")
        userId = intent.getStringExtra("userId")
        eventName = intent.getStringExtra("eventName")
        eventDate = intent.getStringExtra("eventDate")
        eventId = intent.getStringExtra("eventId")
        eventOwnerName = intent.getStringExtra("ownerName")
        if(intent.hasExtra("reminderTimesArray")) {
            eventsReminderTimesArray = intent.getIntegerArrayListExtra("reminderTimesArray")
        }
        if(intent.hasExtra("reminderIdArray")) {
            eventsReminderIdArray = intent.getStringArrayListExtra("reminderIdArray")
        }
        Log.d(TAG, "Reminder to be made for $userName, for event $eventName, at date $eventDate")
        reminderManager = ReminderManager(this)

        //set onCreateVars
        eventNameTextView = findViewById(R.id.eventNameTextView)
        eventDateTextView = findViewById(R.id.eventDateTextView)
        avatarImageView = findViewById(R.id.eventDetailsEventAvatar)
        remindMeOneDay = findViewById(R.id.standardReminderButton)
        doneButton = findViewById(R.id.remindersActivityDoneButton)
        var eventOwnerTextView = findViewById<TextView>(R.id.eventOwnerTextView)
        var welcomeTextView = findViewById<TextView>(R.id.eventDetailsEventNameTextView)

        //second initial status
        eventNameTextView.text = "Name: $eventName"
        eventDateTextView.text = "Date: $eventDate"
        eventOwnerTextView.text = "Owner: $eventOwnerName"
        welcomeTextView.text = "$eventName"
        avatarImageView.setImageResource(R.drawable.icon_v3)

        //set the reminders listView
        listView = findViewById(R.id.listOfEventReminders)
        this.setRemindersAdapter()

        //setting buttons up
        doneButton.setOnClickListener(){
            this.finish()
        }

        this.updateOneDayButtonState()
        remindMeOneDay.setOnClickListener(){
            val oneDayReminder = 1440
            if (eventDate != null && userId != null && eventId != null){
                Log.d(TAG, "mutation for one day is $eventName, id $eventId")
                //reminderManager.getReminderOffset(eventDate, oneDayReminder)
                val newReminder = Reminders(
                    oneDayReminder,
                    "id_to_replace"
                )
                DataManager.addReminder(newReminder, eventId)
                this.eventsReminderTimesArray?.add(oneDayReminder)
                this.eventsReminderIdArray?.add("id_set_by_server")
                this.setRemindersAdapter()
                remindMeOneDay.isEnabled = false
                //getTokenFromAuthServer(oneDayReminder)
                val toast = Toast.makeText(
                    applicationContext,
                    "Your standard reminder for one day before the event has been created.",
                    Toast.LENGTH_SHORT
                )
                toast.show()
            }
        }

        customReminderButton.setOnClickListener(){
            val toast = Toast.makeText(
                applicationContext,
                "I'm sorry this hasn't been implemented for our alpha version. Expect to see this in the beta.",
                Toast.LENGTH_SHORT
            )
            toast.show()

            /*val newEvent = EventsDetails(
                "needFromServer",
                "Test Reminder Name 6",
                "2020-09-30",
                ArrayList<Reminders>(),
                "",
                "",
                "")
            val newReminder = Reminders(1440, "replace_reminder_id")
            val reminderMgr = ReminderManager(this)
            reminderMgr.testReminder = TestReminder(2020, 8, 23, 18, 30, 0)
            reminderMgr.turnOnReminder(newReminder, newEvent)*/
        }
    }

    fun getReminderOffsetForCustomReminder(){
        //TODO: build this
    }

    fun setRemindersAdapter() {
        val reminderIdArray = this.eventsReminderIdArray
        val reminderTimesArray = this.eventsReminderTimesArray
        if(reminderIdArray != null && reminderTimesArray != null) {
            val reminderTimesArray = reminderTimesArray
            val listItems = arrayOfNulls<Int>(reminderTimesArray.size)
            for (i in 0 until reminderTimesArray.size) {
                val reminder = reminderTimesArray[i]
                listItems[i] = reminder
            }

            val reminderIdArray = reminderIdArray
            val listItemsId = arrayOfNulls<String?>(reminderIdArray.size)
            for (i in 0 until reminderIdArray.size) {
                val reminderId = reminderIdArray[i]
                listItemsId[i] = reminderId
            }
            val adapter = ReminderAdapter(this, listItems, listItemsId, eventId)
            listView.adapter = adapter
        }
    }

    fun updateOneDayButtonState() {
        if(eventsReminderTimesArray != null && eventsReminderTimesArray!!.size > 0) {
            remindMeOneDay.isEnabled = false
        } else {
            remindMeOneDay.isEnabled = true
        }
    }
}