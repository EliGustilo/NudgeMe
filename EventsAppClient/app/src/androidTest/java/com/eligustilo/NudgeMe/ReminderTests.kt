package com.eligustilo.NudgeMe

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eligustilo.NudgeMe.ui.ReminderManager
import com.eligustilo.NudgeMe.ui.TestReminder
import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.collections.ArrayList

@RunWith(AndroidJUnit4::class)
class ReminderTests {
    private val TAG = "ReminderTests"
    private var hasuraId = ""
    private var eventId = "bebd2106-006a-43ba-b4c7-695840209bf7"

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
    fun addReminderToCache() {
        if(DataManager.authToken == null) {
            testToken()
        }
        val newReminder =
            Reminders(1440, "replace_reminder_id")
        DataManager.addReminderToCache(newReminder, this.eventId)
        Thread.sleep(2000)

        val data = DataManager.getParsedData()
        assertNotNull(data)

        if(data != null) {
            for (friend in data) {
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    if(event.EventId == this.eventId && event.ReminderArray.contains(newReminder)) {
                        assertTrue(true)
                        DataManager.deleteReminderFromCache(newReminder, event.EventId)
                        return
                    }
                }
            }
        }
        assertTrue(false)
    }

    @Test
    fun addReminder() {
        if(DataManager.authToken == null) {
            testToken()
        }
        val newReminder =
            Reminders(2880, "replace_reminder_id")
        DataManager.addReminderToCache(newReminder, this.eventId)
        Thread.sleep(2000)

        val data = DataManager.getParsedData()
        assertNotNull(data)

        var cacheWorked = false
        if(data != null) {
            for (friend in data) {
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    if(event.EventId == this.eventId && event.ReminderArray.contains(newReminder)) {
                        cacheWorked = true
                        break
                    }
                }
            }
        }
        assertTrue(cacheWorked)
        if(cacheWorked) {
            DataManager.syncAddReminder(newReminder, this.eventId)
            Thread.sleep(4000)
            val data2 = DataManager.getParsedData()
            assertNotNull(data2)

            if(data2 != null) {
                for (friend in data2) {
                    for (event in friend.userEvents) {
                        Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                        if(event.EventId == this.eventId) {
                            for(reminder in event.ReminderArray) {
                                if(reminder.reminder == newReminder.reminder) {
                                    assertTrue(true)

                                    return
                                }
                            }
                        }
                    }
                }
            }
        }
        assert(false) {
            "addReminder() failed"
        }
    }

    @Test
    fun deleteReminderFromCache() {
        if(DataManager.authToken == null) {
            testToken()
        }
        val newReminder =
            Reminders(1440, "replace_reminder_id")
        DataManager.addReminderToCache(newReminder, this.eventId)
        Thread.sleep(2000)

        val data = DataManager.getParsedData()
        assertNotNull(data)

        if(data != null) {
            for (friend in data) {
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    if(event.EventId == this.eventId && event.ReminderArray.contains(newReminder)) {
                        assertTrue(true)
                        DataManager.deleteReminderFromCache(newReminder, event.EventId)
                        break
                    }
                }
            }
        }

        Thread.sleep(2000)

        val data2 = DataManager.getParsedData()
        assertNotNull(data2)

        if(data2 != null) {
            for (friend in data2) {
                for (event in friend.userEvents) {
                    Log.d(TAG, "${friend.displayName}: ${event.EventName}")
                    if(event.EventId == this.eventId && event.ReminderArray.contains(newReminder)) {
                        assertTrue(false)
                    }
                }
            }
            assertTrue(true)
            return
        }
        assert(false) {
            "addReminderToCache() failed"
        }
    }

    @Test
    fun deleteReminder() {
        assert(false) {
            "deleteReminder()  NOT implemented"
        }
    }

    @Test
    fun reminderManagerTurnOn() {
        val newEvent = EventsDetails(
            "needFromServer",
            "Test Reminder Name",
            "2020-09-30",
            ArrayList<Reminders>(),
            "",
            hasuraId,
            ""
        )
        val newReminder =
            Reminders(1440, "replace_reminder_id")
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val reminderMgr = ReminderManager(appContext)
        reminderMgr.testReminder = TestReminder(2020, 8, 21, 21, 41, 0)
        reminderMgr.turnOnReminder(newReminder, newEvent)
        Thread.sleep(2000)
        assertTrue(reminderMgr.isReminderEnabled(newReminder))
    }

    @Test
    fun reminderManagerTurnOff() {
        val newEvent = EventsDetails(
            "needFromServer",
            "Test Reminder Name",
            "2020-09-30",
            ArrayList<Reminders>(),
            "",
            hasuraId,
            ""
        )
        val newReminder =
            Reminders(1440, "replace_reminder_id")
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val reminderMgr = ReminderManager(appContext)
        reminderMgr.testReminder = TestReminder(2020, 8, 21, 21, 41, 0)
        reminderMgr.turnOnReminder(newReminder, newEvent)
        Thread.sleep(2000)
        assertTrue(reminderMgr.isReminderEnabled(newReminder))
        reminderMgr.turnOffReminder(newReminder)
        Thread.sleep(2000)
        assertFalse(reminderMgr.isReminderEnabled(newReminder))
    }

    /*@Test
    fun reminderManagerSync() {
        assertTrue(false)
    }*/

    @Test
    fun reminderManagerTestOffset() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val reminderMgr = ReminderManager(appContext)

        // NOTE: This should work most days. May fail on 1/1 or 12/31
        val date1 = "2020-01-01"
        val date2 = "2020-12-31"
        val eventReminderOffset = 60

        val results1 = reminderMgr.getReminderOffset(date1, eventReminderOffset)
        val results2 = reminderMgr.getReminderOffset(date2, eventReminderOffset)

        assertTrue(results1 > results2)
    }
}