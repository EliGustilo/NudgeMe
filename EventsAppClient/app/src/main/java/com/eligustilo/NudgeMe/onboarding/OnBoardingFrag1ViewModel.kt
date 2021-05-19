package com.eligustilo.NudgeMe.onboarding

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation

class OnBoardingFrag1ViewModel(application: Application) : AndroidViewModel(application) {
    var testMutableData = MutableLiveData<String>()
    private val TAG = "OnBoardingFrag1ViewMode"
    private var JSON_OBJECT_KEY = "OnboardingFragmentViewmodel"

    fun updateData(userName: String) {
        testMutableData.postValue(userName)
        val preferences: SharedPreferences = getApplication<Application>().applicationContext.getSharedPreferences(JSON_OBJECT_KEY, 0)
        val editor = preferences.edit()
        editor.putString(JSON_OBJECT_KEY, userName)
        editor.commit()
        Log.d(TAG, userName)
    }
}