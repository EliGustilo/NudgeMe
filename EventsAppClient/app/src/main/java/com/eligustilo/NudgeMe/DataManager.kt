package com.eligustilo.NudgeMe

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.eligustilo.NudgeMe.ui.ReminderManager
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


data class TopLevelData(
    @SerializedName("data")
    val graphQLData: UserData
)

data class UserData(
    @SerializedName("users")
    val userDetailsData: ArrayList<UserDetails>
)

data class UserDetails (
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("display_name")
    val userName: String,
    /*@SerializedName("avatar")
    val userAvatarId: String,*/
    @SerializedName("friendsByUserId")
    val userFriends: ArrayList<FriendsDetails>
//TODO need to add my events capabilities to show up under my friends events area.
)

data class FriendsDetails (
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("user_id")
    val friendID: String,
    @SerializedName("events")
    val userEvents: ArrayList<EventsDetails>
)

data class EventsDetails (
    @SerializedName("event_id")
    val EventId: String,
    @SerializedName("event_name")
    val EventName: String,
    @SerializedName("date")
    val EventDate: String,
    @SerializedName("reminders")
    val ReminderArray: ArrayList<Reminders>,
    var friendName: String,
    var ownerId: String,
    var displayId: String
)

data class Reminders(
    @SerializedName("reminder")
    val reminder: Int,
    @SerializedName("reminder_id")
    val reminderID: String
)


object DataManager {
    private val TAG = "DataManager"
    private var dataManagerMemoryCache: ArrayList<FriendsDetails>? = null //TODO this is the memory cache? this was/is the parsed data coming in.
    private var userDetails: UserDetails? = null
    private lateinit var context: Context
    var authToken: String? = null
    var hasuraId: String? = null //this is the userID
    private var topLevelDataToBeSaved = ""
    //var isSynced = false
    val TOP_LEVEL_DATA_SAVED_KEY = "DataManager"
    private val dataManagersListToNotify = ArrayList<DataManagerDone>()
    lateinit var newPersonalEventEventId: String
    var syncIsSet = false
    lateinit var currentUserFirebaseAuthID: String


    interface DataManagerDone {
        fun dataReady (friendsArray: ArrayList<FriendsDetails>)
    }

    //singleton boilerplate code //TODO no longer singleton. Findout difference between old singleton and this. What was the advantages of our data refractor besides simplicity?
    fun initWith(newContext: Context) {
        context = newContext
        syncDataWithServer()
        okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
        getUserDefaults()
    }

    // methods add and remove objects to be notified
    fun addMeToBeNotified(objectToAdd:DataManagerDone) {
        dataManagersListToNotify.add(objectToAdd)
    }

    fun removeMeFromNotifyArray(objectToRemove: DataManagerDone) {
        dataManagersListToNotify.remove(objectToRemove)
    }

    fun notifyAllObjects(friendsDetails: ArrayList<FriendsDetails>) {
        for(notifyMe in dataManagersListToNotify) {
            notifyMe.dataReady(friendsDetails)
        }
    }

    fun addEventToCache(newEvent: EventsDetails, friendIDToAddEventTo: String) {
        Log.d(TAG, "addEventToCache called $newEvent is the event given. $friendIDToAddEventTo is the id given.")
        val anInstanceOfDataCache =
            dataManagerMemoryCache
        if (anInstanceOfDataCache != null){
            for (friend in anInstanceOfDataCache){
                if(friend.friendID == friendIDToAddEventTo){
                    friend.userEvents.add(newEvent)
                    dataManagerMemoryCache = anInstanceOfDataCache
                }
            }
        }
    }

    fun deleteEventFromCache(eventToBeDeleted: EventsDetails) {
        val anInstanceOfDataCache =
            dataManagerMemoryCache
        if (anInstanceOfDataCache != null){
            for (friend in anInstanceOfDataCache){
                val myCollection = friend.userEvents
                val iterator = myCollection.iterator()
                while(iterator.hasNext()){
                    val item = iterator.next()
                    if(item.EventId == eventToBeDeleted.EventId){
                        iterator.remove()
                        dataManagerMemoryCache = anInstanceOfDataCache
                        return
                    }
                }
            }
        }
    }

    fun addReminderToCache(newReminder: Reminders, eventId: String) {
        val anInstanceOfDataCache =
            dataManagerMemoryCache
        if (anInstanceOfDataCache != null){
            for (friend in anInstanceOfDataCache){
                val myCollection = friend.userEvents
                val iterator = myCollection.iterator()
                while(iterator.hasNext()){
                    val item = iterator.next()
                    if(item.EventId == eventId){
                        // NOTE: because only allow 1 reminder remove all then add
                        item.ReminderArray.clear()
                        item.ReminderArray.add(newReminder)
                        dataManagerMemoryCache = anInstanceOfDataCache
                        return
                    }
                }
            }
        }
    }

    fun deleteReminderFromCache(reminderToDelete: Reminders, eventId: String) {
        val anInstanceOfDataCache =
            dataManagerMemoryCache
        if (anInstanceOfDataCache != null){
            for (friend in anInstanceOfDataCache){
                for (event in friend.userEvents){
                    val myCollection = friend.userEvents
                    val iterator = myCollection.iterator()
                    while(iterator.hasNext()){
                        val item = iterator.next()
                        if(item.EventId == eventId){
                            item.ReminderArray.remove(reminderToDelete)
                            dataManagerMemoryCache = anInstanceOfDataCache
                            return
                        }
                    }
                }
            }
        }
    }

    // CRUD methods for mem cache //TODO we want to eventually seperate out contact vs date building to make things more friendly. need to think on architecture.
    fun addEventContact(newEvent: EventsDetails, friendIDToAddEventTo: String) {
        Log.d(TAG, "addEvent called $newEvent is the event given. $friendIDToAddEventTo is the id given.")
        addEventToCache(
            newEvent,
            friendIDToAddEventTo
        )
        val anInstanceOfDataCache =
            dataManagerMemoryCache
        if(anInstanceOfDataCache != null) {
            notifyAllObjects(
                anInstanceOfDataCache
            )
            syncAddEventContacts(
                newEvent,
                friendIDToAddEventTo
            )
        }
    }

    fun deleteEvent (eventToBeDeleted: EventsDetails){
        val anInstanceOfDataCache =
            dataManagerMemoryCache
        if(anInstanceOfDataCache != null) {
            notifyAllObjects(
                anInstanceOfDataCache
            )
            syncDeleteEvent(eventToBeDeleted)
        }
    }

    fun addReminder(newReminder: Reminders, eventId: String) {
        addReminderToCache(
            newReminder,
            eventId
        )
        val anInstanceOfDataCache =
            dataManagerMemoryCache
        if(anInstanceOfDataCache != null) {
            notifyAllObjects(
                anInstanceOfDataCache
            )
            syncAddReminder(
                newReminder,
                eventId
            )
        }
    }

    fun deleteReminder(reminderToDelete: Reminders, eventId: String) {
        deleteReminderFromCache(
            reminderToDelete,
            eventId
        )
        val anInstanceOfDataCache =
            dataManagerMemoryCache
        if(anInstanceOfDataCache != null) {
            notifyAllObjects(
                anInstanceOfDataCache
            )
            syncDeleteReminder(
                reminderToDelete,
                eventId
            )
            Log.d(TAG, "reminder deleted ${reminderToDelete.reminderID}")
        }
    }

    // CRUD methods to sync server
    fun syncAddEventContacts(newEvent: EventsDetails, friendDisplayID: String) {//TODO note a friends display_id is their user_id
        if(authToken != null && friendDisplayID != null ) {
            val mutationUrl = "{\"query\":\"mutation NewEventMutation {\\n  insert_events_one(object: {event_name: \\\"${newEvent.EventName}\\\", date: \\\"${newEvent.EventDate}\\\", user_id: \\\"${userDetails?.userId}\\\", display_id: \\\"$friendDisplayID\\\"}) {\\n    event_id\\n    event_name\\n    date\\n  }\\n}\\n\",\"variables\":{}}"
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
                    Log.d(TAG, "Server Respnse = $response")
                    //TODO can I just add a return to the graph QL querry and then store that response as a var and then return. Thats what I need to do.
                    //TODO I need to parse the JSON coming back from the server.
                    val jsonObject = JSONObject(response)
                    val topDataObject = jsonObject.getJSONObject("data")
                    val responseData = topDataObject.getJSONObject("insert_events_one")
                    newPersonalEventEventId = responseData.getString("event_id")
                    Log.d(TAG, "the new eventID is ${newPersonalEventEventId}")
                    okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
                }
            })
        }
    }

    fun syncDeleteEvent (eventToBeDeleted: EventsDetails){
        if (eventToBeDeleted.ownerId == userDetails?.userId){
            //if i own the event
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
        }else{
            //not my event, delete my permissions to see it. Aka hide it
            if(authToken != "token" && hasuraId != null && userDetails?.userId != null ) {
                val mutationUrl = "{\"query\":\"mutation NewUserMutation {\\n  delete_event_permissions(where: {event_id: {_eq: \\\"${eventToBeDeleted.EventId}\\\"}, user_id: {_eq: \\\"${userDetails?.userId}\\\"}}) {\\n    returning {\\n      event_permissions_id\\n    }\\n  }\\n}\\n\",\"variables\":{}}"
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

    }

    fun syncAddReminder(newReminder: Reminders, eventId: String) {
        if(authToken != null && hasuraId != null ) {
            val newReminderQueryString =
                "{\"query\":\"mutation NewReminderMutation {\\n  insert_reminders_one(object: {reminder: ${newReminder.reminder}, user_id: \\\"${hasuraId}\\\", event_id: \\\"$eventId\\\"}) {\\n    reminder\\n  }\\n}\",\"variables\":{}}"
            val okHttpClient = OkHttpClient()
            val body: RequestBody =
                newReminderQueryString.toRequestBody("application/json".toMediaTypeOrNull())

            val okHttpRequest = Request.Builder()
                .url("https://reminder-app-server.herokuapp.com/v1/graphql")
                .post(body)
                .addHeader("Authorization", "Bearer " + authToken)
                .build()//This sends the body as a post to the url and gets back GRAPHQL data.

            okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                    if(response.isSuccessful) {
                        okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
                    } else {
                        Log.d(TAG, "ERROR! syncAddReminder = $response")
                    }
                }
            })
        }
    }

    fun syncDeleteReminder(reminderToDelete: Reminders, eventId: String) {
        if(authToken != "token" && hasuraId != null ) {
            val mutationUrl = "{\"query\":\"mutation NewUserMutation {\\n  delete_reminders(where: {reminder_id: {_eq: \\\"${reminderToDelete.reminderID}\\\"}}) {\\n    returning {\\n      reminder_id\\n    }\\n  }\\n}\\n\",\"variables\":{}}"
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
                    ReminderManager(context).turnOffReminder(reminderToDelete)
                    Log.d(TAG, "response call from server for syncDeleteReminder = $response")
                    okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
                }
            })
        }
    }

    /*fun clearRemindersForEvent(eventId: String) {
        val friendsData = DataManager.getParsedData()
        if(friendsData != null) {
            for (friend in friendsData) {
                for(event in friend.userEvents) {
                    if(event.EventId == eventId) {
                        for(reminder in event.ReminderArray) {
                            this.syncDeleteReminder(reminder, eventId)
                        }
                    }
                }
            }
        }
    }*/


    fun newFirebaseIdLoggedIn(){
        //goal of this is to make the new logged in Firebase id the FirebaseId for the User in the Hasura database upon login.
        val newFireBaseMutationString = "{\"query\":\"mutation newFireBaseId {\\n  update_users(where: {user_id: {_eq: \\\"${userDetails?.userId}\\\"}}, _set: {auth_id: \\\"$currentUserFirebaseAuthID\\\"}) {\\n    returning {\\n      auth_id\\n    }\\n  }\\n}\\n\",\"variables\":{}}"
        val okHttpClient = OkHttpClient ()
        val currentUserFirebaseAuthID = FirebaseAuth.getInstance().currentUser?.uid
        val tokenUrl = "https://reminder-auth-server.herokuapp.com/mobile?authId=$currentUserFirebaseAuthID"

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
                Log.d(TAG, "Token for auth is $authorizationToken")
                if (authorizationToken != null) {
                    authToken = authorizationToken
                    if (currentUserFirebaseAuthID != null) {
                        val body: RequestBody = newFireBaseMutationString.toRequestBody("application/json".toMediaTypeOrNull())

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
                                val okHttpJsonData = response.body?.string()
                                Log.d(TAG, "This is the response test for newFireBaseIDLogin: $okHttpJsonData")
                                okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
                            }
                        })
                    }
                }
            }
        })
    }

    fun mergeUsersDeleteOldUser (oldUserId: String, newUserId: String){
        val test = "{\"query\":\"mutation updateUserEverywhere($oldUserId: uuid_comparison_exp, $newUserId: uuid) {\\n  update_event_permissions(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      event_id\\n      event_permissions_id\\n      user_id\\n    }\\n  }\\n  update_events(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      event_id\\n    }\\n  }\\n  users: update_friends(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      friendship_id\\n    }\\n  }\\n  friends: update_friends(where: {friend_id: $oldUserId}, _set: {friend_id: $newUserId}) {\\n    returning {\\n      friendship_id\\n    }\\n  }\\n  update_reminders(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      reminder_id\\n    }\\n  }\\n  update_requests(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      request_id\\n    }\\n  }\\n  delete_users(where: {user_id: $oldUserId}) {\\n    affected_rows\\n  }\\n}\",\"variables\":{\"oldUserId\":{\"_eq\":\"bdd0d1f4-b20b-40f9-937b-24368ce4dd46\"},\"newUserId\":\"54e878b4-0de5-4fa2-a611-f972ab0c3281\"}}"
        val mutationString = "{\"query\":\"mutation updateUserEverywhere($oldUserId: uuid_comparison_exp, $newUserId: uuid) {\\n  update_event_permissions(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      event_id\\n      event_permissions_id\\n      user_id\\n    }\\n  }\\n  update_events(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      event_id\\n    }\\n  }\\n  users: update_friends(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      friendship_id\\n    }\\n  }\\n  friends: update_friends(where: {friend_id: $oldUserId}, _set: {friend_id: $newUserId}) {\\n    returning {\\n      friendship_id\\n    }\\n  }\\n  update_reminders(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      reminder_id\\n    }\\n  }\\n  update_requests(where: {user_id: $oldUserId}, _set: {user_id: $newUserId}) {\\n    returning {\\n      request_id\\n    }\\n  }\\n  delete_users(where: {user_id: $oldUserId}) {\\n    affected_rows\\n  }\\n}\",\"variables\":{\"oldUserId\":{\"_eq\":\"$oldUserId\"},\"newUserId\":\"$newUserId\"}}"
        val okHttpClient = OkHttpClient ()
        val currentUserFirebaseAuthID = FirebaseAuth.getInstance().currentUser?.uid
        val tokenUrl = "https://reminder-auth-server.herokuapp.com/mobile?authId=$currentUserFirebaseAuthID"

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
                Log.d(TAG, "Token for auth is $authorizationToken")
                if (authorizationToken != null) {
                    authToken = authorizationToken
                    if (currentUserFirebaseAuthID != null) {
                        val body: RequestBody = mutationString.toRequestBody("application/json".toMediaTypeOrNull())

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
                                val response = response.body?.string()
                                Log.d(TAG, "This is the response test for mergeTwoUsers: $response")
                                val jsonObject = JSONObject (response)
                                val data = jsonObject.getJSONObject("data")
                                val insertUsersOne = data.getJSONObject("users")
                                val newLoggedInUserId = insertUsersOne.getString("user_id")
                                Log.d(TAG, "the new logged in user ID is $newLoggedInUserId")
                                okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
                            }
                        })
                    }
                }
            }
        })
    }


    fun okHTTPDataDownloader(urlGiven: String) {
        getFirebaseId(urlGiven)
    }

    fun parseDataJSONObject (jsonData: String): ArrayList<FriendsDetails> {
        val returnFriendsDetailArray = ArrayList<FriendsDetails>()

        val jsonObject = JSONObject(jsonData)

        val topDataObject = jsonObject.getJSONObject("data")
        val userData = topDataObject.getJSONArray("users")
        val firstUser = userData[0] as JSONObject

        val userName = firstUser.getString("display_name")
        val userId = firstUser.getString("user_id")
        val friendsDetails = firstUser.getJSONArray("friends")
        val eventArrayWithDisplayId = firstUser.getJSONArray("events")

        var itemIndexFriendsArray = 0
        while (itemIndexFriendsArray < friendsDetails.length()){
            val friend = friendsDetails[itemIndexFriendsArray] as JSONObject
            val friendUser = friend.getJSONObject("userByFriendId")
            val friendDisplayName = friendUser.getString("display_name")
            val friendUserId = friendUser.getString("user_id")
            val eventsArray = friendUser.getJSONArray("events")

            val returnEventArray = ArrayList<EventsDetails>()
            var eventIndexArray = 0
            while (eventIndexArray < eventsArray.length()){
                val event = eventsArray[eventIndexArray] as JSONObject
                val eventDate = event.getString("date")
                val eventID = event.getString("event_id")
                val eventName = event.getString("event_name")
                val eventUserId = event.getString("user_id")
                val eventReminders = event.getJSONArray("reminders")
                val returnReminderArray = ArrayList<Reminders>()

                var remindersIndexArray = 0
                while (remindersIndexArray < eventReminders.length()){
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
                    friendDisplayName,
                    eventUserId,
                    friendUserId
                )

                returnEventArray.add(eventDetail)
                eventIndexArray = eventIndexArray + 1
            }
            // is there an event from a myPrivateEvents that needs to be added before this?
            val insertDisplayIdArray =
                checkForDisplayId(
                    eventArrayWithDisplayId,
                    friendUserId
                )
            if(insertDisplayIdArray != null) {
                returnEventArray.addAll(insertDisplayIdArray)
            }
            //sort events
            returnEventArray.sortWith(object: Comparator<EventsDetails>{
                override fun compare(event1: EventsDetails, event2: EventsDetails): Int = when {
                    getEventInMilliseconds(
                        event1
                    ) > getEventInMilliseconds(
                        event2
                    ) -> 1
                    getEventInMilliseconds(
                        event1
                    ) == getEventInMilliseconds(
                        event2
                    ) -> 0
                    else -> -1
                }
            })

            val friendDetail = FriendsDetails(
                friendDisplayName,
                friendUserId,
                returnEventArray
            )
            returnFriendsDetailArray.add(friendDetail)
            itemIndexFriendsArray = itemIndexFriendsArray + 1
        }
        userDetails =
            UserDetails(
                userId,
                userName,
                returnFriendsDetailArray
            )
        //after this point we should have all data.
        return returnFriendsDetailArray
    }

    private fun checkForDisplayId(eventJsonArray: JSONArray, friendId: String): ArrayList<EventsDetails>? {
        //We give this function the second array data set and one event from the first array data set. We run through the second array data set and compare it
        //to the event from the first array. TODO complete
        val returnArray = ArrayList<EventsDetails>()

        var index = 0
        while (index < eventJsonArray.length()) {
            val event = eventJsonArray[index] as JSONObject
            val eventID = event.getString("event_id")
            val eventName = event.getString("event_name")
            try {
                val eventDisplayUser = event.getJSONObject("display_user")
                val eventDate = event.getString("date")
                val ownerId = event.getString("user_id")
                val displayId = eventDisplayUser.getString("user_id")
                val displayName = eventDisplayUser.getString("display_name")
//            Log.d(TAG, "displayId = $displayId")
                if (displayId == friendId) {
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
                        displayName,
                        ownerId,
                        displayId
                    )
                    returnArray.add(eventDetail)
                }
            } catch(e: Exception){
                //do nothing
            }
            index += 1
        }
        //if we don't find one we return null.
        if(returnArray.size > 0) {
            return returnArray
        }
        return null
    }

    fun getParsedData(): ArrayList<FriendsDetails>? {
        return dataManagerMemoryCache
    }

    fun getUserDetails(): UserDetails?{
        return userDetails
    }

    fun syncDataWithServer(){
        syncIsSet = true
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.postDelayed(Runnable {
            syncIsSet = false
            okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
        }, 60000)
    }

    fun getEventInMilliseconds (eventReminderTime: EventsDetails): Long {
        //this splits string into 3 arrays for month, day, year
        val dateArray = eventReminderTime.EventDate.split('-')
        //TODO need to make this handle more date formats
        //this is the date of the event
        val month = dateArray[1].toInt()-1 //java months go 0-11 for 12 total thought this is at array position 0 so should be no problem. Odd.
        val day = dateArray[2].toInt()
        val currentYear = Calendar.getInstance()[Calendar.YEAR]
//        Log.d(TAG, "the date gotten from the contactsEventsDate.split are: month: ${month}, day: ${day}, currentYear: ${currentYear}")
        //gets a calendar object to be used to check event dates
        val eventCalendarDueDate = Calendar.getInstance()
        eventCalendarDueDate.set(currentYear,month,day,9,0,0)
//        Log.d(TAG, "the manually set calendar date to be used to check our event date is : ${eventCalendarDueDate.time}")
        //setting up variables to hold the event reminders
        var nextEventDay: Date = eventCalendarDueDate.time
        val today = Date()
//        Log.d(TAG, "The event reminder variables are today's date of: $today and the nextEventDate of $nextEventDay")
        //checking to see if event is in past or future and adjusting year to match next event date year
        if(nextEventDay.before(today)) {
            eventCalendarDueDate.set(currentYear+1,month,day,9,0,0)
            nextEventDay = eventCalendarDueDate.time
//            Log.d(TAG, "because the nextEventDate is already past we have set it for next years eventDate and that date is: $nextEventDay")
        }
        //this is the actual reminder math. everything is in milliseconds
        val nextEventDayMiliseconds = eventCalendarDueDate.timeInMillis
//        Log.d(TAG, "the nextEventDate in milliseconds is: $nextEventDayMiliseconds")
        return nextEventDayMiliseconds
    }

    fun getEventDateAsString (eventReminderTime: EventsDetails): String {
        val formatter = SimpleDateFormat("MMMM d, YYYY")
        val eventInMilliseconds =
            getEventInMilliseconds(
                eventReminderTime
            )
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = eventInMilliseconds
        return formatter.format(calendar.time)
    }

    fun getEventDateAsDayOfWeek (eventReminderTime: EventsDetails): String{
        val formatter = SimpleDateFormat("E")
        val eventInMilliseconds =
            getEventInMilliseconds(
                eventReminderTime
            )
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = eventInMilliseconds
        return formatter.format(calendar.time)
    }

    fun getJustTokenFromAuthServer() {
        val okHttpClient = OkHttpClient ()
        val currentUserFirebaseAuthID = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserFirebaseAuthID != null) {
//            didSignIn()
            Log.d(TAG, "current firebase auth is $currentUserFirebaseAuthID")
        } else {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener {
                    Log.d(TAG, "FIREBASE ID: ${it.credential}")
                }
                .addOnFailureListener {
                    Log.d(TAG, "FIREBASE Failure: $it")
                }
        }
        val tokenUrl = "https://reminder-auth-server.herokuapp.com/mobile?authId=$currentUserFirebaseAuthID"
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
                Log.d(TAG, "Just Get Token for auth is $authorizationToken")
                if (authorizationToken != null) {
                    authToken = authorizationToken
                }
            }
        })
    }

     fun getFirebaseId(urlGiven: String){
        currentUserFirebaseAuthID = FirebaseAuth.getInstance().currentUser?.uid.toString()
        if (currentUserFirebaseAuthID != null) {
            getAuthToken(urlGiven)
        } else {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener {
                    Log.d(TAG, "FIREBASE ID: ${it.credential}")
                    currentUserFirebaseAuthID ="$it.credential"
                    getAuthToken(urlGiven)
                }
                .addOnFailureListener {
                    Log.d(TAG, "FIREBASE Failure: $it")
                }
        }
    }

    fun getAuthToken(urlGiven: String){
        val okHttpClient = OkHttpClient ()
        val currentUserFirebaseAuthID = FirebaseAuth.getInstance().currentUser?.uid
        val tokenUrl = "https://reminder-auth-server.herokuapp.com/mobile?authId=$currentUserFirebaseAuthID"

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
                Log.d(TAG, "Token for auth is $authorizationToken")
                if (authorizationToken != null) {
                    authToken = authorizationToken
                    if (currentUserFirebaseAuthID != null) {
                        getUserID(
                            currentUserFirebaseAuthID,
                            urlGiven,
                            authorizationToken
                        )
                    }
                }
            }
        })
    }

    private fun getUserID(firebaseAuthId: String, urlGiven: String, authToken: String){
        val okHttpClient = OkHttpClient ()
        Log.d(TAG, "CurrentUser for Token Data is $firebaseAuthId")

        val getUserIDUrl = "{\"query\":\"query MyQuery {\\n  users(where: {auth_id: {_eq: \\\"$firebaseAuthId\\\"}}) {\\n    auth_id\\n    user_id\\n  }\\n}\\n\",\"variables\":{}}"

        val body: RequestBody = getUserIDUrl.toRequestBody("application/json".toMediaTypeOrNull())

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
                //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                val hasuraJSON = response.body?.string()
                Log.d(TAG, "2nd step json blob $hasuraJSON")
                if(hasuraJSON != null) {
                    val jsonObject = JSONObject(hasuraJSON)
                    val topDataObject = jsonObject.getJSONObject("data")
                    val userData = topDataObject.getJSONArray("users")
                    if(userData.length() > 0) {
                        val userInArray = userData[0] as JSONObject
                        var userId = userInArray.getString("user_id")
                        hasuraId = userId
                        Log.d(TAG, "userId: $userId")
                        getUserDetailsData(
                            userId,
                            urlGiven,
                            authToken,
                            firebaseAuthId
                        )
                    }
                }
            }
        })
    }

    private fun getUserDetailsData(userId: String, urlGiven: String, authToken: String, firebaseAuthId: String) {
        val okHttpClient = OkHttpClient ()
        val requiredGraphQLQuerryString = "{\"query\":\"query MyQuery {\\n  users(where: {auth_id: {_eq: \\\"$firebaseAuthId\\\"}, _and: {}}) {\\n    display_name\\n    user_id\\n    friends(order_by: {userByFriendId: {display_name: asc}}) {\\n      userByFriendId {\\n        display_name\\n        user_id\\n        events(where: {event_permissions: {user_id: {_eq: \\\"$userId\\\"}}}) {\\n          date\\n          event_id\\n          event_name\\n          user_id\\n          reminders {\\n            reminder\\n            reminder_id\\n          }\\n        }\\n      }\\n    }\\n    events(where: {display_id: {_is_null: false}}) {\\n      event_id\\n      event_name\\n      display_user {\\n        display_name\\n        user_id\\n      }\\n      date\\n      user_id\\n      reminders {\\n        reminder\\n        reminder_id\\n      }\\n    }\\n  }\\n}\\n\",\"variables\":{}}"

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
                Log.d(TAG, "This is the response test for getUserDetailsData: $okHttpJsonData")

                if(okHttpJsonData != null) {
                    syncDataWithServer()
                    topLevelDataToBeSaved = okHttpJsonData
                    //parse data here due to async nature of OKHttp. if used master function to control all data flow, will have null okHttp data due to background thread not completed.
                    val friendsDetailsArray =
                        parseDataJSONObject(
                            okHttpJsonData
                        )
                    dataManagerMemoryCache = friendsDetailsArray
                    notifyAllObjects(
                        friendsDetailsArray
                    )
                    saveDataForOffline()
                    if(!syncIsSet) {
                        syncDataWithServer()
                    }
                    val reminderManager = ReminderManager(context)
                    for (friend in friendsDetailsArray){
                        reminderManager.syncReminder(friend.userEvents)
//                        Log.d(TAG, "This is the friend whose details are being Synced $friend")
                    }
                }
            }
        })
    }

    private fun saveDataForOffline(){
        val pendingIntentHashValueKey = 0
        val userDefaults: SharedPreferences = context.getSharedPreferences(
            TOP_LEVEL_DATA_SAVED_KEY, 0)//TODO reminder_key is linked to just on jsonRemindersAsString? doesn't this need to be a for loop.
        val editor = userDefaults.edit()
        editor.putString(
            TOP_LEVEL_DATA_SAVED_KEY,
            topLevelDataToBeSaved
        )
        editor.putInt("pendingIntentKey", pendingIntentHashValueKey)
//        Log.d(TAG, "Data to be saved is $topLevelDataToBeSaved")
        editor.apply()
    }

    private fun getUserDefaults() {
        val userDefaults: SharedPreferences = context.getSharedPreferences(
            TOP_LEVEL_DATA_SAVED_KEY, 0)//TODO reminder_key is linked to just on jsonRemindersAsString? doesn't this need to be a for loop.
        if(userDefaults.contains(TOP_LEVEL_DATA_SAVED_KEY)) {
            //if there is previously saved data give it to view model
            topLevelDataToBeSaved = userDefaults.getString(
                TOP_LEVEL_DATA_SAVED_KEY, "").toString()
            if(topLevelDataToBeSaved.length > 0) {
                Log.d(TAG, "the userDefaults data is: ${topLevelDataToBeSaved}")
                val friendsDetailsArray =
                    parseDataJSONObject(
                        topLevelDataToBeSaved
                    )
                dataManagerMemoryCache = friendsDetailsArray
                notifyAllObjects(
                    friendsDetailsArray
                )
            }
        }
    }


//    private fun parseDataGSON(jsonData: String): TopLevelData {

//        val gsonParser = Gson()
//        val gsonDataType: Type = object : TypeToken<TopLevelData>() {}.type
//        val gsonResults: TopLevelData = gsonParser.fromJson(jsonData, gsonDataType)
//        Log.d(TAG, "gson results are: $gsonResults")
//        return gsonResults
//    } Not doing this due to messy JSON inbound. TODO make this work later as a technology proof. Aka I can do it.
}


