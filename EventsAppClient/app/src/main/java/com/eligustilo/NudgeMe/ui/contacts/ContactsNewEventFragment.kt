package com.eligustilo.NudgeMe.ui.contacts


import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eligustilo.NudgeMe.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.contacts_new_event_fragment.*
import java.util.*

class ContactsNewEventFragment: Fragment() {
    private val TAG = "NewEventFragment"
    lateinit var newEventName: String
    lateinit var newEventDateAsString: String
    private val userName = DataManager.getUserDetails()?.userName
    private val userAuthId = FirebaseAuth.getInstance().currentUser?.uid
    private var displayId: String? = null
    private var friendsFriendId: String? = null
    private var friendName: String? = null
    private lateinit var friendToAddEventTo: FriendsDetails

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.contacts_new_event_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //vars for the view
        val newEventAvatar = view.findViewById<TextView>(R.id.createEventAvatar)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val createButton = view.findViewById<Button>(R.id.createEventButton)

        //setting initial status of things

        displayId = arguments?.getString("display_id")
        friendsFriendId = arguments?.getString("friend_id")
        friendName = arguments?.getString("friend_name")
        createEventWelcomeTextEdit.setText("Create an event for ${friendName}?")
        if (friendName?.length!! > 0){
            val displayAvatarLetter = friendName?.substring(0, 1)?.capitalize()
            newEventAvatar.text = displayAvatarLetter
        } else{
            newEventAvatar.text = "N/A"
        }

        //Handles the click on the textEdit to enter the event name
        eventNameEntry.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                newEventName = eventNameEntry.text.toString()
            }
        })

        //handles the calendar popup
        eventDateEntry.setOnClickListener(){
            dateWindowPopup(view)
        }

        //handles create button
        createButton.setOnClickListener(){
            if (newEventDateAsString.isNullOrBlank() == true && newEventName.isNullOrEmpty() == true){
                Log.d(TAG, "You need a name and date before I can create your event.")
                //TODO handle when user clicks go before name and date are inputed.
            } else {
                val eventName = newEventName
                if (userName != null && userAuthId != null) {

                    val newEvent = EventsDetails(
                        "needFromServer",
                        newEventName,
                        newEventDateAsString,
                        ArrayList<Reminders>(),
                        "",
                        "",
                        ""
                    )

                    if (newEvent != null && friendsFriendId != null){
                        DataManager.addEventContact(newEvent, friendsFriendId!!)
                    }
                }
                findNavController().navigate(R.id.events_new_event_fragment_to_navigation_events)
                val toast = Toast.makeText(context, "Your event $eventName has been created.", Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        //handles cancel/back button
        cancelButton.setOnClickListener(){
            findNavController().navigate(R.id.events_new_event_fragment_to_navigation_events)
        }
    }



    private fun dateWindowPopup(view: View) {
        var date: Calendar = Calendar.getInstance()
        var thisAYear = date.get(Calendar.YEAR).toInt()
        var thisAMonth = date.get(Calendar.MONTH).toInt()
        var thisADay = date.get(Calendar.DAY_OF_MONTH).toInt()

        val dpd = DatePickerDialog(this.requireContext(), DatePickerDialog.OnDateSetListener { view2, thisYear, thisMonth, thisDay ->
            // Display Selected date in textbox
            thisAMonth = thisMonth + 1
            thisADay = thisDay
            thisAYear = thisYear

            eventDateEntry.setText("Date: " + thisAMonth + "/" + thisDay + "/" + thisYear)
            val newDate:Calendar =Calendar.getInstance()
            newDate.set(thisYear, thisMonth, thisDay)
            val dateAsString = "$thisAYear-$thisAMonth-$thisADay"
            newEventDateAsString = dateAsString
            Log.d(TAG, "newDate to String = $newEventDateAsString")
        }, thisAYear, thisAMonth, thisADay)
        dpd.show()
        dpd.onDateChanged(dpd.datePicker, thisAYear, thisAMonth, thisADay)
    }
}

