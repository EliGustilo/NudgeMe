package com.eligustilo.NudgeMe.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.eligustilo.NudgeMe.DataManager
import com.eligustilo.NudgeMe.FriendsDetails

class ContactsViewModel(application: Application) : AndroidViewModel(application), DataManager.DataManagerDone {
    private var TAG = "ContactsViewModel"
    val mutableContactData = MutableLiveData<ArrayList<FriendsDetails>>()

    init {
        getContactData()
        DataManager.addMeToBeNotified(this)
    }

    fun getContactData(){
        if (DataManager.getParsedData()!= null){
            val parsedData = DataManager.getParsedData()
            mutableContactData.postValue(parsedData)
        } else {
            DataManager.addMeToBeNotified(this)
            DataManager.okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
        }
    }

    override fun dataReady(friendsArray: ArrayList<FriendsDetails>) {
        mutableContactData.postValue(friendsArray)
    }
}