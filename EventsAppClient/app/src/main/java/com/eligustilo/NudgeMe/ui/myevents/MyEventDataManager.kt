package com.eligustilo.NudgeMe

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

data class MyEventUserDetails (
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("display_name")
    val userName: String,
    @SerializedName("events")
    val userEvents: ArrayList<EventsDetails>
)

object MyEventDataManager { 
    private val TAG = "MyEventDataManager"
    private var myEventUserDetailsMemoryCache: MyEventUserDetails? = null
    private var authToken = "token"
    //TODO cache authToken
    //var isSynced = false
    val TOP_LEVEL_DATA_SAVED_KEY = "MyDataManager"
    private var topLevelDataToBeSaved = ""
    private lateinit var context: Context
    private val objectsToNotifyArray = ArrayList<MyEventDataManagerDone>()
    private var hasuraId: String? = null //this is the userID
    lateinit var newPersonalEventEventId: String

    interface MyEventDataManagerDone {
        fun dataReady (eventDetails: MyEventUserDetails)
    }

    fun initWith(newContext: Context) {
        this.context = newContext
        this.syncDataWithServer()
        this.readFromUserDefaults()
    }

    // methods add and remove objects to be notified
    fun addMeToBeNotified(objectToAdd: MyEventDataManagerDone) { //TODO learn details how this interface array works. Its calling the refrence to the implementation?
        objectsToNotifyArray.add(objectToAdd)
    }

    fun removeMeFromNotifyArray(objectToRemove: MyEventDataManagerDone) {
        objectsToNotifyArray.remove(objectToRemove)
    }

    fun notifyAllObjects(eventDetails: MyEventUserDetails) {
        for(notifyMe in objectsToNotifyArray) {
                notifyMe.dataReady(eventDetails)
        }
    }

    // CRUD methods for mem cache
    fun addEvent(newEvent: EventsDetails) {
        this.addToCache(newEvent)
        val eventDetails = this.myEventUserDetailsMemoryCache
        if( eventDetails != null) {
            this.notifyAllObjects(eventDetails) // update UI
        }
        this.syncAddEvent(newEvent)
    }

    fun deleteEvent (eventToBeDeleted: EventsDetails){
        this.deleteFromCache(eventToBeDeleted)
        val eventDetails = this.myEventUserDetailsMemoryCache
        if( eventDetails != null) {
            this.notifyAllObjects(eventDetails) // update UI
        }
        this.syncDeleteEvent(eventToBeDeleted)// update server
    }

    fun updateEvent (eventToBeUpdated: EventsDetails, userToGiveEventTo: String){
        this.updateCache(eventToBeUpdated)
        val eventDetails = this.myEventUserDetailsMemoryCache
        if( eventDetails != null) {
            this.notifyAllObjects(eventDetails) // update UI
        }
        this.syncUpdateEvent(eventToBeUpdated, userToGiveEventTo)// update server
    }

    // separate method for unit tests
    fun addToCache(newEvent: EventsDetails) {
        this.myEventUserDetailsMemoryCache?.userEvents?.add(newEvent)
    }

    fun updateCache(eventToBeUpdated: EventsDetails) {
        val eventDetails = this.myEventUserDetailsMemoryCache
        if (eventDetails != null){
            val myCollection = eventDetails.userEvents
            val iterator = myCollection.iterator()
            while(iterator.hasNext()){ //TODO learn abut iterator and hasNext terminology. is this a mutableIterator?
                //TODO iterator is basically a safe?? way to call collections. What is the difference between a collection and a array?
                //TODO
                val item = iterator.next()
                if(item.EventId == eventToBeUpdated.EventId){
                    iterator.remove()
                }
            }
        }
        this.myEventUserDetailsMemoryCache?.userEvents?.add(eventToBeUpdated)
        this.myEventUserDetailsMemoryCache = eventDetails
    }

    fun deleteFromCache(eventToBeDeleted: EventsDetails) {
        this.myEventUserDetailsMemoryCache?.userEvents?.remove(eventToBeDeleted)// update memory cache
    }

    private fun checkForTokenAndHasuraId() {
        val firebaseId = DataManager.currentUserFirebaseAuthID
        val currentAuthToken = DataManager.authToken
        val userId = DataManager.hasuraId
        if(userId != null && currentAuthToken != null) {
            this.authToken = currentAuthToken
            this.hasuraId = userId
        }
    }

    // CRUD methods to sync server
    fun syncAddEvent(newEvent: EventsDetails) {
        this.checkForTokenAndHasuraId()
        if(authToken != "token" && hasuraId != null ) {
            val mutationUrl = "{\"query\":\"mutation NewPrivateEventMutation {\\n  insert_events_one(object: {event_name: \\\"${newEvent.EventName}\\\", date: \\\"${newEvent.EventDate}\\\", user_id: \\\"$hasuraId\\\", personal_event: true}) {\\n    event_id\\n    event_name\\n    date\\n    personal_event\\n  }\\n}\",\"variables\":{}}"
            val okHttpClient = OkHttpClient ()
            val body: RequestBody = mutationUrl.toRequestBody("application/json".toMediaTypeOrNull())

            val okHttpRequest = Request.Builder()
                .url("https://reminder-app-server.herokuapp.com/v1/graphql")
                .post(body)
                .addHeader("Authorization" , "Bearer " + authToken)
                .build()//This sends the body as a post to the url and gets back GRAPHQL data.

            okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                    val response = response.body?.string()
                    Log.d(TAG, "success call from gabi server???? what is this??? = $response")
                    //TODO can I just add a return to the graph QL querry and then store that response as a var and then return. Thats what I need to do.
                    //TODO I need to parse the JSON coming back from the server.
                    val jsonObject = JSONObject(response)
                    val topDataObject = jsonObject.getJSONObject("data")
                    val responseData = topDataObject.getJSONObject("insert_events_one")
                    newPersonalEventEventId = responseData.getString("event_id")
                    Log.d(TAG, "the eventID is $newPersonalEventEventId")

                    okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
                }
            })
        }
    }

    fun syncDeleteEvent (eventToBeDeleted: EventsDetails){
        this.checkForTokenAndHasuraId()
        //TODO make graphql String to delete
        if(authToken != "token" && hasuraId != null ) {
            val mutationUrl =  "{\"query\":\"mutation DeleteMutation {\\n  delete_events(where: {event_id: {_eq: \\\"${eventToBeDeleted.EventId}\\\"}}) {\\n    returning {\\n      event_id\\n      event_name\\n      date\\n      display_id\\n    }\\n  }\\n}\\n\",\"variables\":{}}"
            val okHttpClient = OkHttpClient ()
            val body: RequestBody = mutationUrl.toRequestBody("application/json".toMediaTypeOrNull())

            val okHttpRequest = Request.Builder()
                .url("https://reminder-app-server.herokuapp.com/v1/graphql")
                .post(body)
                .addHeader("Authorization" , "Bearer " + authToken)
                .build()//This sends the body as a post to the url and gets back GRAPHQL data.

            okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                    val response = response.body?.string()
                    Log.d(TAG, "response call from server for syncDeleteEvent = $response")
                    okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
                }
            })
        }
    }

    fun syncUpdateEvent (eventToBeUpdated: EventsDetails, userToGiveEventTo: String){
        this.checkForTokenAndHasuraId()
        if(authToken != "token" && hasuraId != null ) {
            val mutationUrl = "{\"query\":\"mutation SyncEventMutation {\\n  update_events(where: {event_id: {_eq: \\\"${eventToBeUpdated.EventId}\\\"}}, _set: {date: \\\"${eventToBeUpdated.EventDate}\\\", event_name: \\\"${eventToBeUpdated.EventName}\\\", display_id: \\\"${userToGiveEventTo}\\\"}) {\\n    returning {\\n      display_id\\n      event_id\\n      event_name\\n      date\\n    }\\n  }\\n}\\n\",\"variables\":{}}"
            val okHttpClient = OkHttpClient ()
            val body: RequestBody = mutationUrl.toRequestBody("application/json".toMediaTypeOrNull())

            val okHttpRequest = Request.Builder()
                .url("https://reminder-app-server.herokuapp.com/v1/graphql")
                .post(body)
                .addHeader("Authorization" , "Bearer " + authToken)
                .build()//This sends the body as a post to the url and gets back GRAPHQL data.

            okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                    val response = response.body?.string()
                    Log.d(TAG, "success call from gabi server???? what is this??? = $response")
                    okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
                    DataManager.okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
                }
            })
        }
    }

    fun okHTTPDataDownloader(urlGiven: String) {
        val firebaseId = DataManager.currentUserFirebaseAuthID
        val currentAuthToken = DataManager.authToken
        val userId = DataManager.hasuraId
        if(userId != null && currentAuthToken != null) {
            this.authToken = currentAuthToken
            this.hasuraId = userId
            getUserDetailsData(userId, urlGiven, this.authToken, firebaseId)
        }
    }

    fun parseDataJSONObject (jsonData: String): MyEventUserDetails? {
        val returnEventArray = ArrayList<EventsDetails>()
        val jsonObject = JSONObject(jsonData)

        val topDataObject = jsonObject.getJSONObject("data")
        val userData = topDataObject.getJSONArray("users")
        if(userData.length() > 0) {
            val firstUser = userData[0] as JSONObject
            val userName = firstUser.getString("display_name")
            val userId = firstUser.getString("user_id")
            val eventsArray = firstUser.getJSONArray("events")
            var eventIndexArray = 0
            while (eventIndexArray < eventsArray.length()) {
                val event = eventsArray[eventIndexArray] as JSONObject
                val eventDate = event.getString("date")
                val eventID = event.getString("event_id")
                val eventName = event.getString("event_name")
                val eventReminders = event.getJSONArray("reminders")

                val returnReminderArray = ArrayList<Reminders>()
                var remindersIndexArray = 0
                while (remindersIndexArray < eventReminders.length()) {
                    val reminder = eventReminders[remindersIndexArray] as JSONObject
                    val reminderTime = reminder.getInt("reminder")
                    val reminderID = reminder.getString("reminder_id")
                    returnReminderArray.add(
                        Reminders(
                            reminderTime,
                            reminderID
                        )
                    )
                    remindersIndexArray = remindersIndexArray + 1
                }

                val eventDetail = EventsDetails(
                    eventID,
                    eventName,
                    eventDate,
                    returnReminderArray,
                    userName,
                    "",
                    ""
                )//TODO this might break
                returnEventArray.add(eventDetail)
                eventIndexArray = eventIndexArray + 1
            }
            val newMyEventUserDetails = MyEventUserDetails(userId, userName, returnEventArray)
            //after this point we should have all data.
            return newMyEventUserDetails
        }
        return null
    }

    fun getPrivateUserDetails(): MyEventUserDetails?{
        return myEventUserDetailsMemoryCache
    }

    fun syncDataWithServer (){
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                mainHandler.postDelayed(this, 600000)
                okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
            }
        })
    }

    private fun getUserDetailsData(userId: String, urlGiven: String, authToken: String, firebaseAuthId: String) {
        val okHttpClient = OkHttpClient ()

//        val requiredGraphQLQuerryString = "{\"query\":\"query MyQuery {\\n  users(where: {auth_id: {_eq: \\\"UctH0ItvHBc50vWDoFvH6WMOAM22\\\"}}) {\\n    display_name\\n    user_id\\n    events(where: {personal_event: {_eq: true}, _and: {display_id: {_is_null: true}}}) {\\n      date\\n      event_id\\n      event_name\\n      personal_event\\n      reminders {\\n        reminder\\n        reminder_id\\n      }\\n    }\\n  }\\n}\\n\",\"variables\":{}}"

        val requiredGraphQLQuerryString = "{\"query\":\"query MyQuery {\\n  users(where: {auth_id: {_eq: \\\"$firebaseAuthId\\\"}}) {\\n    display_name\\n    user_id\\n    events(where: {personal_event: {_eq: true}, _and: {display_id: {_is_null: true}}}) {\\n      date\\n      event_id\\n      event_name\\n      personal_event\\n      reminders {\\n        reminder\\n        reminder_id\\n      }\\n    }\\n  }\\n}\\n\",\"variables\":{}}"
        val body: RequestBody = requiredGraphQLQuerryString.toRequestBody("application/json".toMediaTypeOrNull())

        val okHttpRequest = Request.Builder()
            .url(urlGiven)
            .post(body)
            .addHeader("Authorization" , "Bearer " + authToken)
            .build()//This sends the body as a post to the url and gets back GRAPHQL data.

        okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val okHttpJsonData = response.body?.string()
                Log.d(TAG, "This is the response test for okHttp and GraphQL: $okHttpJsonData")

                if(okHttpJsonData != null) {
                    topLevelDataToBeSaved = okHttpJsonData
                    saveUserDefaults()
                    //parse data here due to async nature of OKHttp. if used master function to control all data flow, will have null okHttp data due to background thread not completed.
                    val myEventUserDetails = parseDataJSONObject(okHttpJsonData)
                    if(myEventUserDetails != null ) {
                        myEventUserDetailsMemoryCache = myEventUserDetails
                        notifyAllObjects(myEventUserDetails)
                        //objectThatWantsData.dataReady(myEventUserDetails)
                        /*if (isSynced == false) {
                            //syncDataWithServer()//goal is to sync data with server not reminders here.
                            isSynced = true
                        } else {
                            //do nothing
                        }*/
                    }
                }
            }
        })
    }

    fun getEventDateAsDayOfWeek (eventReminderTime: String): String{
        val formatter = SimpleDateFormat("E")
        val eventInMilliseconds = getEventInMilliseconds(eventReminderTime)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = eventInMilliseconds
        return formatter.format(calendar.time)
    }

    fun getEventInMilliseconds (eventReminderTime: String): Long {
        //this splits string into 3 arrays for month, day, year
        val dateArray = eventReminderTime.split('-')
        //TODO need to make this handle more date formats
        //this is the date of the event
        val month = dateArray[1].toInt()-1 //java months go 0-11 for 12 total thought this is at array position 0 so should be no problem. Odd.
        val day = dateArray[2].toInt()
        val currentYear = Calendar.getInstance()[Calendar.YEAR]
        Log.d(TAG, "the date gotten from the contactsEventsDate.split are: month: ${month}, day: ${day}, currentYear: ${currentYear}")
        //gets a calendar object to be used to check event dates
        val eventCalendarDueDate = Calendar.getInstance()
        eventCalendarDueDate.set(currentYear,month,day,9,0,0)
        Log.d(TAG, "the manually set calendar date to be used to check our event date is : ${eventCalendarDueDate.time}")
        //setting up variables to hold the event reminders
        var nextEventDay: Date = eventCalendarDueDate.time
        val today = Date()
        Log.d(TAG, "The event reminder variables are today's date of: $today and the nextEventDate of $nextEventDay")
        //checking to see if event is in past or future and adjusting year to match next event date year
        if(nextEventDay.before(today)) {
            eventCalendarDueDate.set(currentYear+1,month,day,9,0,0)
            nextEventDay = eventCalendarDueDate.time
            Log.d(TAG, "because the nextEventDate is already past we have set it for next years eventDate and that date is: $nextEventDay")
        }
        //this is the actual reminder math. everything is in milliseconds
        val nextEventDayMiliseconds = eventCalendarDueDate.timeInMillis
        Log.d(TAG, "the nextEventDate in milliseconds is: $nextEventDayMiliseconds")
        return nextEventDayMiliseconds
    }

    private fun saveUserDefaults(){
        val userDefaults: SharedPreferences = context.getSharedPreferences(TOP_LEVEL_DATA_SAVED_KEY, 0)//TODO reminder_key is linked to just on jsonRemindersAsString? doesn't this need to be a for loop.
        val editor = userDefaults.edit()
        editor.putString(TOP_LEVEL_DATA_SAVED_KEY, topLevelDataToBeSaved)
        Log.d(TAG, "Data to be saved is $topLevelDataToBeSaved")
        editor.apply()
    }

    fun readFromUserDefaults() {
        val userDefaults: SharedPreferences = context.getSharedPreferences(TOP_LEVEL_DATA_SAVED_KEY, 0)//TODO reminder_key is linked to just on jsonRemindersAsString? doesn't this need to be a for loop.
        if(userDefaults.contains(TOP_LEVEL_DATA_SAVED_KEY)) {
            //if there is previously saved data give it to view model
            topLevelDataToBeSaved = userDefaults.getString(TOP_LEVEL_DATA_SAVED_KEY, "").toString()
            if(topLevelDataToBeSaved.length > 0) {
                Log.d(TAG, topLevelDataToBeSaved)
                val myEventDetails = parseDataJSONObject(topLevelDataToBeSaved)
                if(myEventDetails != null) {
                    notifyAllObjects(myEventDetails)
                    myEventUserDetailsMemoryCache = myEventDetails
                }
            }
        }
    }
}

