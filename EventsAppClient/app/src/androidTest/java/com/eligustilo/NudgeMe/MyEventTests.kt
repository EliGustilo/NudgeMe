package com.eligustilo.NudgeMe

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class MyEventTests: MyEventDataManager.MyEventDataManagerDone {

    private var lock2: CountDownLatch = CountDownLatch(2)
    private var friendName = ""
    private val displayId = "fcf2961d-95fe-4845-8bb1-5e982cda84c6"
    private var hasuraId = ""

    fun getEvent(): EventsDetails {
        val emptyRemindersArray = ArrayList<Reminders>()
        return EventsDetails(
            "testEvent 1",
            "AAAAAA TestEvent",
            "01/01/2020",
            emptyRemindersArray,
            friendName,
            hasuraId,
            ""
        )
    }

    fun startMyEventDataManager() {
        lock2 = CountDownLatch(2)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        DataManager.initWith(appContext)
        DataManager.okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")

        lock2.await(6000, TimeUnit.MILLISECONDS)
        MyEventDataManager.initWith(appContext)
        MyEventDataManager.addMeToBeNotified(this)
        MyEventDataManager.okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
        Thread.sleep(4000)

        val updateFriendName = DataManager.getUserDetails()?.userName
        if(updateFriendName != null) {
            friendName = updateFriendName
        }

        val userId = DataManager.getUserDetails()?.userId
        if(userId != null) {
            hasuraId = userId
        }
    }

    // MyEventDataManager
    @Test
    fun testAddToCache() {
        addToCachePrivate(true)
    }

    private fun addToCachePrivate(cleanup: Boolean = true) {
        startMyEventDataManager()
        val eventDetails = getEvent()

        MyEventDataManager.addToCache(eventDetails)

        var cacheWorked = false
        val updateData = MyEventDataManager.getPrivateUserDetails()

        if(updateData != null) {
            for(currentEvent in updateData.userEvents) {
                if(currentEvent.EventId == eventDetails.EventId && currentEvent.EventName == eventDetails.EventName) {
                    cacheWorked = true
                }
            }
        }
        if(cacheWorked) {
            assertTrue(cacheWorked) // added to cache
            if(cleanup) {
                MyEventDataManager.deleteFromCache(eventDetails)
            }
            return
        }
        assertTrue(false)
    }

    @Test
    fun testAddMyEvent() {
        testAddMyEventPrivate(true)
    }

    private fun testAddMyEventPrivate(cleanup: Boolean = true) {
        startMyEventDataManager()
        val eventDetails = getEvent()

        MyEventDataManager.addToCache(eventDetails)
        var cacheWorked = false
        val updateData = MyEventDataManager.getPrivateUserDetails()

        if(updateData != null) {
            for(currentEvent in updateData.userEvents) {
                if(currentEvent.EventId == eventDetails.EventId && currentEvent.EventName == eventDetails.EventName) {
                    cacheWorked = true
                }
            }
        }
        if(cacheWorked) {
            assertTrue(cacheWorked) // added to cache

            MyEventDataManager.syncAddEvent(eventDetails)
            Thread.sleep(4000)
            startMyEventDataManager()
            val updateData2 = MyEventDataManager.getPrivateUserDetails()
            cacheWorked = false
            if(updateData2 != null) {
                for(currentEvent in updateData2.userEvents) {
                    if(currentEvent.EventName == eventDetails.EventName) {
                        cacheWorked = true
                        break
                    }
                }
            }
            if(cleanup && updateData2 != null) {
                var newEvent: EventsDetails? = null
                for(currentEvent in updateData2.userEvents) {
                    if(currentEvent.EventName == eventDetails.EventName) {
                        newEvent = currentEvent
                        break
                    }
                }

                if(newEvent != null) {
                    MyEventDataManager.syncDeleteEvent(newEvent)
                    Thread.sleep(6000)
                } else {
                    assertTrue(false)
                }
            }
        }
        assertTrue(cacheWorked)
    }

    @Test
    fun testUpdateCache() {
        addToCachePrivate(false)
        val eventDetails = getEvent()
        val updatedEvent = EventsDetails(
            eventDetails.EventId,
            eventDetails.EventName,
            eventDetails.EventDate,
            eventDetails.ReminderArray,
            friendName,
            hasuraId,
            displayId
        )
        MyEventDataManager.updateCache(updatedEvent)
        Thread.sleep(4000)

        val updateData = MyEventDataManager.getPrivateUserDetails()
        if(updateData != null) {
            for(currentEvent in updateData.userEvents) {
                if(currentEvent.EventName == updatedEvent.EventName && currentEvent.displayId == updatedEvent.displayId) {
                    assertTrue(true)
                    MyEventDataManager.deleteFromCache(updatedEvent)
                    return
                }
            }
        }
        assertTrue(false)
    }

    @Test
    fun testUpdateEvent() {
        testAddMyEventPrivate(false)
        Thread.sleep(2000)
        val data = MyEventDataManager.getPrivateUserDetails()
        var newEvent: EventsDetails? = null
        if(data != null) {
            val eventDetails = getEvent()
            for(currentEvent in data.userEvents) {
                if(currentEvent.EventName == eventDetails.EventName) {
                    newEvent = currentEvent
                    Log.d("MyEventTest", "newEvent WORKED!")
                    break
                }
            }
        }

        if(newEvent != null) {
            val updatedEvent = EventsDetails(
                newEvent.EventId,
                newEvent.EventName,
                newEvent.EventDate,
                newEvent.ReminderArray,
                newEvent.friendName,
                hasuraId,
                displayId
            )
            MyEventDataManager.updateCache(updatedEvent)
            Thread.sleep(4000)

            var cacheWorked = false
            val updateData = MyEventDataManager.getPrivateUserDetails()
            if(updateData != null) {
                for(currentEvent in updateData.userEvents) {
                    if(currentEvent.EventName == updatedEvent.EventName && currentEvent.displayId == updatedEvent.displayId) {
                        cacheWorked = true
                        break
                    }
                }
            }

            if(cacheWorked) {
                assertTrue(cacheWorked)
                MyEventDataManager.syncUpdateEvent(updatedEvent, displayId)
                Thread.sleep(4000)
                startMyEventDataManager()
                val updateData2 = MyEventDataManager.getPrivateUserDetails()
                if(updateData2 != null) {
                    for(currentEvent in updateData2.userEvents) {
                        if(currentEvent.EventId == updatedEvent.EventId) {
                            assertTrue(false)
                        }
                    }
                }
                assertTrue(true)
                MyEventDataManager.deleteFromCache(updatedEvent)
                MyEventDataManager.syncDeleteEvent(updatedEvent)
                Thread.sleep(4000)
                return
            }
        }
        assertTrue(false)
    }


    @Test
    fun testDeleteFromCache() {
        startMyEventDataManager()
        val eventDetails = getEvent()

        MyEventDataManager.addToCache(eventDetails)

        var cacheWorked = false
        val updateData = MyEventDataManager.getPrivateUserDetails()

        if(updateData != null) {
            for(currentEvent in updateData.userEvents) {
                if(currentEvent.EventId == eventDetails.EventId && currentEvent.EventName == eventDetails.EventName) {
                    cacheWorked = true
                }
            }
        }

        // Delete check
        if(cacheWorked) {
            MyEventDataManager.deleteFromCache(eventDetails)
            val updateData2 = MyEventDataManager.getPrivateUserDetails()

            if(updateData2 != null) {
                for(currentEvent in updateData2.userEvents) {
                    if(currentEvent.EventId == eventDetails.EventId && currentEvent.EventName == eventDetails.EventName) {
                        assertTrue(false)
                    }
                }
                assertTrue(true)
                return
            }
        } else {
            assertTrue(false)
        }
    }

    @Test
    fun testDeleteMyEvent() {
        testAddMyEventPrivate(false)
        val eventDetails = getEvent()
        val data = MyEventDataManager.getPrivateUserDetails()
        var newEvent: EventsDetails? = null
        if(data != null) {
            for(currentEvent in data.userEvents) {
                if(currentEvent.EventName == eventDetails.EventName) {
                    newEvent = currentEvent
                    break
                }
            }
        }
        var cacheWorked = true
        if(newEvent != null) {
            MyEventDataManager.deleteFromCache(newEvent)
        }
        val updateData = MyEventDataManager.getPrivateUserDetails()
        if(updateData != null && newEvent != null) {
            for(currentEvent in updateData.userEvents) {
                if(currentEvent.EventName == eventDetails.EventName) {
                    cacheWorked = false
                }
            }
        }
        assertTrue(cacheWorked)
        if(cacheWorked && newEvent != null) { //delete sync
            MyEventDataManager.syncDeleteEvent(newEvent)
            Thread.sleep(4000)
            startMyEventDataManager()
            val updateData2 = MyEventDataManager.getPrivateUserDetails()
            if(updateData2 != null) {
                for(currentEvent in updateData2.userEvents) {
                    if(currentEvent.EventName == eventDetails.EventName) {
                        assertTrue(false) // added to cache
                        return
                    }
                }
                assertTrue(true)
                return
            }
        }
        assertTrue(false)
    }

    override fun dataReady(eventDetails: MyEventUserDetails) {
        lock2.countDown()
    }

}