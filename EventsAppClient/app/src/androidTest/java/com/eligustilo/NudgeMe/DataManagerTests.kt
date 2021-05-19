package com.eligustilo.NudgeMe

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.collections.ArrayList

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DataManagerTests {
    private val TAG = "DataManagerTests"
    private var hasuraId = ""
    private val friendId = "fcf2961d-95fe-4845-8bb1-5e982cda84c6"

    @Test
    fun testToken() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        DataManager.initWith(appContext)
        DataManager.okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")

        Thread.sleep(4000)
        val authToken = DataManager.authToken
        if(authToken != null) {
            Log.d(TAG, "this.token = $authToken")
        }
        assertNotNull(authToken)

        val userId = DataManager.getUserDetails()?.userId
        if(userId != null) {
            hasuraId = userId
        }
    }

    @Test
    fun testGetUserID() {
        if(DataManager.authToken == null) {
            testToken()
        }

        val userDetails = DataManager.getUserDetails()
        assertNotNull(userDetails)
    }

    @Test
    fun testGetData() {
        if(DataManager.authToken == null) {
            testToken()
        }

        val data = DataManager.getParsedData()
        assertNotNull(data)

        if(data != null) {
            var noEvents = 0
            for (friend in data) {
                Log.d(TAG, friend.displayName)
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    noEvents++
                }
            }
            assertTrue(data.size > 0)
            assertTrue(noEvents > 0)
        }
    }

    fun testAddEventToCachePrivate(cleanup: Boolean = true) {
        if(DataManager.authToken == null) {
            testToken()
        }
        val name = "A Test Name ${UUID.randomUUID().toString()}"
        val newEvent = EventsDetails(
            "needFromServer",
            name,
            "2020-09-30",
             ArrayList<Reminders>(),
            "",
            hasuraId,
            friendId
        )
       DataManager.addEventToCache(newEvent, friendId)
        Thread.sleep(2000)

        val data = DataManager.getParsedData()
        assertNotNull(data)

        if(data != null) {
            for (friend in data) {
                Log.d(TAG, friend.displayName)
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    if(event.EventName == name && event.displayId == friendId) {
                        assertTrue(true)
                        if(cleanup) {
                            testDeleteFromCachePrivate(newEvent)
                        }
                        return
                    }
                }
            }
        }
        assertTrue(false)
    }

    @Test
    fun testAddEventToCache() {
        testAddEventToCachePrivate(true)
    }

    fun testDeleteFromCachePrivate(event: EventsDetails) {
        DataManager.deleteEventFromCache(event)
    }

    @Test
    fun testDeleteFromCache() {
        testAddEventToCachePrivate(false)
        testToken()
        val name = "A Test Name ${UUID.randomUUID().toString()}"
        val newEvent = EventsDetails(
            "needFromServer",
            name,
            "2020-09-30",
            ArrayList<Reminders>(),
            "",
            hasuraId,
            friendId
        )
        DataManager.addEventToCache(newEvent, friendId)
        Thread.sleep(2000)

        val data = DataManager.getParsedData()
        assertNotNull(data)

        if(data != null) {
            for (friend in data) {
                Log.d(TAG, friend.displayName)
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    if(event.EventName == name && event.displayId == friendId) {
                        assertTrue(true)
                        testDeleteFromCachePrivate(newEvent)
                        Thread.sleep(2000)
                    }
                }
            }
        }

        val data2 = DataManager.getParsedData()
        assertNotNull(data2)

        if(data2 != null) {
            for (friend in data2) {
                Log.d(TAG, friend.displayName)
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    if(event.EventName == name && event.displayId == friendId) {
                        assertTrue(false)
                        return
                    }
                }
            }
        }
        assertTrue(true)
    }


    @Test
    fun testAddEvent() {
        if(DataManager.authToken == null) {
            testToken()
        }
        val name = "A Test Name ${UUID.randomUUID().toString()}"
        val newEvent = EventsDetails(
            "needFromServer",
            name,
            "2020-09-30",
            ArrayList<Reminders>(),
            "",
            hasuraId,
            friendId
        )
        DataManager.addEventToCache(newEvent, friendId)
        Thread.sleep(2000)

        val data = DataManager.getParsedData()
        assertNotNull(data)

        var cacheWorked = false
        if(data != null) {
            for (friend in data) {
                Log.d(TAG, friend.displayName)
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    if(event.EventName == name && event.displayId == friendId) {
                        cacheWorked = true
                    }
                }
            }
        }
        assertTrue(cacheWorked)
        if(cacheWorked) {
            DataManager.syncAddEventContacts(newEvent, friendId)
            Thread.sleep(4000)
            val data2 = DataManager.getParsedData()

            assertNotNull(data2)
            if(data2 != null) {
                for (friend in data2) {
                    Log.d(TAG, friend.displayName)
                    for (event in friend.userEvents) {
                        Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                        if(event.EventName == name && event.displayId == friendId) {
                            assertTrue(true)
                            DataManager.syncDeleteEvent(event)
                            Thread.sleep(4000)
                            return
                        }
                    }
                }
            }
        }
        assertTrue(false)
    }

    @Test
    fun testDeleteEvent() {
        if(DataManager.authToken == null) {
            testToken()
        }
        val name = "A Test Name ${UUID.randomUUID().toString()}"
        val newEvent = EventsDetails(
            "needFromServer",
            name,
            "2020-09-30",
            ArrayList<Reminders>(),
            "",
            hasuraId,
            friendId
        )
        DataManager.addEventToCache(newEvent, friendId)
        Thread.sleep(2000)

        val data = DataManager.getParsedData()
        assertNotNull(data)

        var cacheWorked = false
        if(data != null) {
            for (friend in data) {
                Log.d(TAG, friend.displayName)
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    if(event.EventName == name && event.displayId == friendId) {
                        cacheWorked = true
                    }
                }
            }
        }
        assertTrue(cacheWorked)

        var updatedEvent: EventsDetails? = null
        var syncWorked = false
        if(cacheWorked) {
            DataManager.syncAddEventContacts(newEvent, friendId)
            Thread.sleep(4000)
            val data2 = DataManager.getParsedData()

            assertNotNull(data2)

            if(data2 != null) {
                for (friend in data2) {
                    Log.d(TAG, friend.displayName)
                    for (event in friend.userEvents) {
                        Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                        if(event.EventName == name && event.displayId == friendId) {
                            syncWorked = true
                            updatedEvent = event
                        }
                    }
                }
            }
        }
        assertTrue(syncWorked)

        if(syncWorked && updatedEvent != null) {
            testDeleteFromCachePrivate(updatedEvent)
            Thread.sleep(2000)
            DataManager.syncDeleteEvent(updatedEvent)
            Thread.sleep(4000)

            val data3 = DataManager.getParsedData()
            assertNotNull(data3)

            if(data3 != null) {
                for (friend in data3) {
                    Log.d(TAG, friend.displayName)
                    for (event in friend.userEvents) {
                        Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                        if(event.EventName == name && event.displayId == friendId) {
                            assertTrue(false)
                            return
                        }
                    }
                }
            }
            assertTrue(true)
        }
    }
}