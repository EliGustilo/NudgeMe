package com.eligustilo.NudgeMe.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.eligustilo.NudgeMe.DataManager
import com.eligustilo.NudgeMe.FriendsDetails

class EventsViewModel(application: Application) : AndroidViewModel(application), DataManager.DataManagerDone {
    val mutableHomeData = MutableLiveData<ArrayList<FriendsDetails>>()

    init {
        getContactData()
        DataManager.addMeToBeNotified(this)
    }

    fun getContactData(){
        val topLevelDataOrNull = DataManager.getParsedData()
        val context = getApplication<Application>().applicationContext

        if (topLevelDataOrNull != null){
            val parsedData = DataManager.getParsedData()
            mutableHomeData.postValue(parsedData)//TODO what is postValue? its for after async is done
        } else {
            DataManager.addMeToBeNotified(this)
            DataManager.okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
        }
    }

    //DataManager is giving us data here. An ArrayList of FriendDetails is coming from okHTTPDataDownloader Because of async messing up everything
    override fun dataReady(friendsArray: ArrayList<FriendsDetails>) {
        mutableHomeData.postValue(friendsArray)
    }
}