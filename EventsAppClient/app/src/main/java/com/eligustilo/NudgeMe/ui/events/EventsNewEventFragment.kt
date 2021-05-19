package com.eligustilo.NudgeMe.ui.events


import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eligustilo.NudgeMe.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.event_new_event_layout.*


class EventsNewEventFragment: Fragment() {
    private val TAG = "EventNewEventFragment"
    private lateinit var newEventName: String
    private lateinit var newEventDateAsString: String
    private val userName = DataManager.getUserDetails()?.userName
    private val userID = DataManager.getUserDetails()?.userId
    private val userAuthId = FirebaseAuth.getInstance().currentUser?.uid
    private var userToken = "getToken"
    private var userId = DataManager.getUserDetails()?.userId
    private lateinit var friendToAddEventTo: FriendsDetails
    val oneDayReminder = 1440
    lateinit var eventDetail: EventsDetails
    lateinit var friendsFriendID: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.event_new_event_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //vars for the view
        //val newEventAvatar = view.findViewById<TextView>(R.id.eventsCreateEventAvatar)
        val cancelButton = view.findViewById<Button>(R.id.eventsCancelButton)
        val createButton = view.findViewById<Button>(R.id.eventsCreateEventButton)

        //setting initial status of things
        val displayId = arguments?.getString("display_id")
        val eventDate = arguments?.getString("event_date").toString()
//        val formatter = SimpleDateFormat("MMMM d, YYYY")
//        val displayDate = formatter.format(eventDate)
        eventsCreateEventWelcomeTextEdit.text = "Create a new event for:"
        newEventAvatarDate.text = eventDate
        newEventDateAsString = eventDate

        //Handles the click on the textEdit to enter the event name
        eventsEventNameEntry.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                newEventName = eventsEventNameEntry.text.toString()
            }
        })

        //handles the builder/whichfriend popup
        whichFriendSelector.setOnClickListener(){

            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)



            val myContactsList = DataManager.getParsedData()
            val builderContactList = ArrayList<String>()
            if (myContactsList != null) {
                for (contact in myContactsList){
                    builderContactList.add(contact.displayName)
                }
            }
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Which contact do you want to assign this event to?")
            //syntax mean when builder is clicked it gives me the dialog, which is the spinner box that appears after the click. Which is the item inside the
            //dialog box that is selected. Aka it is the arrayOf, contactsList.
            //when statement is for when which item is selected and then does x code.
            //which is the item inside the dialog popup window. The array count position.

            //builder.setSingleChoiceItems builds the list in the dialog box from the builderContactList. The Dialog, which -> is a
            //lambeda and is meant to allow for more precise control???? Find tutorials.
            var itemSelected = 0
            builder.setSingleChoiceItems(builderContactList.toTypedArray(), -1){ dialog, which ->
                // leave empty
                Log.d(TAG, "test builder selctor ${which.toString()}")
                itemSelected = which
            }

            //this sets the ok button need to tell it what is inside the choices list otherwise cnannot handle clicking on item and then clicking okay.
            builder.setPositiveButton("Confirm"){ dialog, which ->
                val contact = myContactsList?.get(itemSelected)
                Log.d(TAG, contact?.displayName)
                Log.d(TAG, contact?.friendID)
                if(contact != null) {
                    friendsFriendID = contact.friendID
                }
            }

            //always backs out of the builder window.
            builder.setNegativeButton("Cancel", null)

            //this instatiates the builder and finalizes it then shows it
            //dialog is a random name and not related to above dialog at all. Unsure why its called dialog. Rename
            val dialog = builder.create()
            dialog.show()
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

                    if (newEvent != null && userID != null){
                        DataManager.addEventContact(newEvent, friendsFriendID)
                        Log.d(
                            TAG,
                            "newEvent name is: ${newEvent.EventName}, and the user id to give it to is: $userID"
                        )
                    }
                }
                findNavController().navigate(R.id.events_new_event_fragment_to_navigation_events)
                val toast = Toast.makeText(
                    context,
                    "Your event $eventName has been created.",
                    Toast.LENGTH_SHORT
                )
                toast.show()
            }
        }

//        handles cancel/back button
        cancelButton.setOnClickListener(){
            findNavController().navigate(R.id.events_new_event_fragment_to_navigation_events)
        }
    }
}

