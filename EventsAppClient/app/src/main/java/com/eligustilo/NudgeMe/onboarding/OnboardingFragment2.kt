package com.eligustilo.NudgeMe.onboarding

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eligustilo.NudgeMe.*
import com.eligustilo.NudgeMe.ui.SharingManager

class OnboardingFragment2: Fragment(), DataManager.DataManagerDone {
    private lateinit var backButton: Button
    private lateinit var inviteFriendsButton: Button
    private lateinit var tempDoneButton: Button
    private var NAME_OBJECT_KEY = "OnboardingFragment1"
    private val TAG = "OnboardingFragment2"
    private lateinit var sharingDescriptionTextView: TextView
    private lateinit var newUserName: String
    private lateinit var userAvatar: TextView
    val ON_BOARDING_KEY = "ON_BOARDING_KEY"
    var authId = "authId"
    var displayName = "displayname"
    var userId = "userID"
    var authToken = "AuthToken"


    override fun onCreateView(//TODO what is this onCreateView doing exactly
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.onboard_fragment_2, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Sets the top action bar, already set in other frags but ensures compliance.
        val mainActivity = activity as AppCompatActivity?
        mainActivity?.supportActionBar?.setCustomView(R.layout.action_bar_custom)
        val actionBarTitle = mainActivity?.supportActionBar?.customView?.findViewById<TextView>(R.id.action_bar_title)
        actionBarTitle?.setText("NudgeMe")
        val actionBarImage = mainActivity?.supportActionBar?.customView?.findViewById<ImageView>(R.id.action_bar_image)
        actionBarImage?.setImageResource(R.drawable.icon_main_blue)
        mainActivity?.supportActionBar?.setDisplayShowCustomEnabled(true)

        //setting xml variables
        backButton = view.findViewById(R.id.onboarding_previous_button)
        inviteFriendsButton = view.findViewById(R.id.inviteFriendsButton)
        tempDoneButton = view.findViewById(R.id.temporaryOnboardingToMainScreen)
        sharingDescriptionTextView = view.findViewById(R.id.sharingOnboardingDescriptionTextView)

        //setting misc things
        DataManager.okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
        DataManager.addMeToBeNotified(this)
        Log.d(TAG, "The DataManager.getUserDetails in the OnboardFrag 2 is: ${DataManager.getUserDetails()}")
        userId = arguments?.getString("userId").toString()
        authId = arguments?.getString("authId").toString()
        authToken = arguments?.getString("authToken").toString()
        val userDetails = DataManager.getUserDetails()
        val sharingManager = SharingManager(this.requireContext())


        //Setting user name in description
        newUserName = getUserName(this.requireContext())
        sharingDescriptionTextView.text = "Thanks for that $newUserName! Now we can request important dates from your friends and family! just click the invite button below and NudgeMe will take care of the rest!"


        //Setting avatar with initals
        userAvatar = view.findViewById(R.id.onboardingFrag2ImageViewAvatar)
        if (newUserName != null){
            if(newUserName.length > 0) {
                val displayAvatarLetter = newUserName.substring(0, 1).capitalize()
                userAvatar.text = displayAvatarLetter
            } else {
                userAvatar.text = "?"
            }
        }

        backButton.setOnClickListener{
            val navController = findNavController()
            navController.navigate(R.id.action_onboard2_to_onboard_1)
        }


        tempDoneButton.setOnClickListener{
            DataManager.removeMeFromNotifyArray(this)
            val userDefaults: SharedPreferences = this.requireContext().getSharedPreferences(ON_BOARDING_KEY, 0)
            val editor = userDefaults.edit()
            editor.putString(ON_BOARDING_KEY, "true")
            editor.commit()
            val intent = Intent(context, MainActivity::class.java)
            this.startActivity(intent)
        }

        inviteFriendsButton.setOnClickListener{
            if (arguments != null){
                Log.d(TAG, "User details not null")
                sharingManager.inviteFriendsOnboarding(authId, userId, newUserName, this.requireActivity(), authToken)
            }else{
                Log.d(TAG, "User details null")
                //TODO handle null
            }
        }
    }

    fun getUserName(context: Context): String{
        var userNameString = ""
        val userDefaults: SharedPreferences = context.applicationContext.getSharedPreferences(NAME_OBJECT_KEY, 0)
        if (userDefaults.contains(NAME_OBJECT_KEY)){
            val userName = userDefaults.getString(NAME_OBJECT_KEY, null)
            if (userName != null) {
                userNameString = userName
            }
            Log.d(TAG, "the given user name test for onboarding is $userNameString")
        }
        return userNameString
    }

    override fun dataReady(friendsArray: ArrayList<FriendsDetails>) {
        //this is supposed to be a call back to DataManager to get the user details from them
        //from the async nature of the call. Not working. Brute force it is.
    }
}