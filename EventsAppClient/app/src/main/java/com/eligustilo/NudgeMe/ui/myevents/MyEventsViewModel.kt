package com.eligustilo.NudgeMe.ui.myevents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.eligustilo.NudgeMe.MyEventDataManager
import com.eligustilo.NudgeMe.MyEventUserDetails

class MyEventsViewModel(application: Application): AndroidViewModel(application), MyEventDataManager.MyEventDataManagerDone {
    val mutablePrivateEventData = MutableLiveData<MyEventUserDetails>()

    init {
        getPrivateUserData()
        MyEventDataManager.addMeToBeNotified(this)
    }

    fun getPrivateUserData (){
        val topLevelUserData = MyEventDataManager.getPrivateUserDetails()

        if(topLevelUserData != null){
            mutablePrivateEventData.postValue(topLevelUserData)
        } else{
            MyEventDataManager.okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
        }
    }

    override fun dataReady(myEventUserDetails: MyEventUserDetails) {
        mutablePrivateEventData.postValue(myEventUserDetails)
    }
}