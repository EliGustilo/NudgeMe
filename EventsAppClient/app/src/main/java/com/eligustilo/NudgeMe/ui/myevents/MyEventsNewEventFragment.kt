package com.eligustilo.NudgeMe.ui.myevents

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eligustilo.NudgeMe.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.my_create_personal_event_screen_layout.*
import okhttp3.*
import java.io.IOException
import java.util.*

class MyEventsNewEventFragment: Fragment() {
    private val TAG = "MyNewEventFragment"
    lateinit var newPersonalEventName: String
    lateinit var newPersonalEventDateAsString: String
    private val dataManager = DataManager.getUserDetails()
    private val userName = dataManager?.userName
    private val userID = dataManager?.userId
    private val userAuthId = FirebaseAuth.getInstance().currentUser?.uid
    private var userToken = "getToken"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.my_create_personal_event_screen_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //vars for the view
        val newPersonalEventAvatar = view.findViewById<ImageView>(R.id.personalCreateEventImageView)
        val personalCancelButton = view.findViewById<Button>(R.id.personalCancelButton)
        val personalCreateButton = view.findViewById<Button>(R.id.personalCreateEventButton)

        //setting initial status of things
        newPersonalEventAvatar.setImageResource(R.drawable.icon_v3)
        personalCreateEventWelcomeTextEdit.text = "Want to create a new event ${userName}?"
        getToken(requireContext())
        //TODO figure out how we want to do avatar. Left blank to demostrate area's openness

        //Handles the click on the textEdit to enter the event name
        personalEventNameEntry.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                newPersonalEventName = personalEventNameEntry.text.toString()
            }
        })

        personalCreateButton.setOnClickListener(){
            if (newPersonalEventDateAsString.isNullOrBlank() == true && newPersonalEventName.isNullOrEmpty() == true){
                Log.d(TAG, "You need a name and date before I can create your event.")
                //TODO handle when user clicks go before name and date are inputed. Need to change above code to and/or instead of and
            } else {
                val eventName = newPersonalEventName
                if (userName != null && userAuthId != null) {
                    val newEvent = EventsDetails(
                        "needFromServer",
                        newPersonalEventName,
                        newPersonalEventDateAsString,
                        ArrayList<Reminders>(),
                        "",
                        "",
                        ""
                    )
                    MyEventDataManager.addEvent(newEvent)
                    
                }
                findNavController().navigate(R.id.my_new_personal_event_fragment_to_my_events)
                val toast = Toast.makeText(context, "Your event $eventName has been created.", Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        personalCancelButton.setOnClickListener(){
            findNavController().navigate(R.id.my_new_personal_event_fragment_to_my_events)
        }

        personalEventDateEntry.setOnClickListener(){
            dateWindowPopup(view)
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

            personalEventDateEntry.setText("Date: " + thisAMonth + "/" + thisDay + "/" + thisYear)
            val newDate: Calendar = Calendar.getInstance()
            newDate.set(thisYear, thisMonth, thisDay)
            val dateAsString = "$thisAYear-$thisAMonth-$thisADay"
            newPersonalEventDateAsString = dateAsString
            Log.d(TAG, "newDate to String = $newPersonalEventDateAsString")
        }, thisAYear, thisAMonth, thisADay)
        dpd.show()
        dpd.onDateChanged(dpd.datePicker, thisAYear, thisAMonth, thisADay)
    }

    private fun getToken(context: Context){
        val okHttpClient = OkHttpClient ()
        val tokenUrl = "https://reminder-auth-server.herokuapp.com/mobile?authId=$userAuthId"

        val okHttpRequest = Request.Builder()
            .url(tokenUrl)
            .build()//This sends the body as a post to the url and gets back GRAPHQL data.

        okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                val authorizationToken = response.body?.string()
                if (authorizationToken != null) {
                    userToken = authorizationToken
                }
                Log.d(TAG, "eli 123 test authorizationToken = $userToken")
            }
        })
    }
}