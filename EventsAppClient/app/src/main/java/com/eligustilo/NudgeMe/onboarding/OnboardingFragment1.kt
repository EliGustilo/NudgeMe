package com.eligustilo.NudgeMe.onboarding

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.eligustilo.NudgeMe.R
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


class OnboardingFragment1: Fragment() {
    private val TAG = "OnBoardingFragment1"
    lateinit var nextScreenButton: Button
    lateinit var nameTextView: TextView
    private var NAME_OBJECT_KEY = "OnboardingFragment1"
    private lateinit var OnboardingViewModel1: OnBoardingFrag1ViewModel
    var authId = "authId"
    var displayName = "displayname"
    var userId = "userID"
    val bundle = Bundle ()
    val ON_BOARDING_KEY = "ON_BOARDING_KEY"

    override fun onCreateView(//TODO what is this onCreateView doing exactly
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        OnboardingViewModel1 = ViewModelProviders.of(this).get(OnBoardingFrag1ViewModel::class.java)
        val root = inflater.inflate(R.layout.onboard_fragment_1, container, false)

        OnboardingViewModel1.testMutableData.observe(viewLifecycleOwner, Observer {
            var testMutableData1 = it
            Log.d(TAG, testMutableData1)
        })
        return root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nameTextView = view.findViewById(R.id.nameInputTextView)

        //Sets the top action bar, already set in other frags but ensures compliance.
        val mainActivity = activity as AppCompatActivity?
        mainActivity?.supportActionBar?.setCustomView(R.layout.action_bar_custom)
        val actionBarTitle = mainActivity?.supportActionBar?.customView?.findViewById<TextView>(R.id.action_bar_title)
        actionBarTitle?.setText("NudgeMe")
        val actionBarImage = mainActivity?.supportActionBar?.customView?.findViewById<ImageView>(R.id.action_bar_image)
        actionBarImage?.setImageResource(R.drawable.icon_main_blue)
        mainActivity?.supportActionBar?.setDisplayShowCustomEnabled(true)
        val colorDrawable = ColorDrawable(Color.parseColor("#0f81c7"))
        mainActivity?.supportActionBar?.setBackgroundDrawable(colorDrawable)

        nameTextView.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.ACTION_DOWN ) {
                Log.d(TAG, v.text.toString())
                nameTextView.text = v.text.trim() // remove the return
                // closes the keyboard
                val imm: InputMethodManager? = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                if(imm != null) {
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
                // makes it so the nameTextView is not focused object
                OnboardingViewModel1.updateData(nameTextView.text.toString())
                saveName(this.requireContext())
                v.clearFocus()
                true //done
            }
            false
        }

        nextScreenButton = view.findViewById(R.id.test_onboarding_forward)//TODO why does this not work properly without a viewModel?

        nextScreenButton.setOnClickListener{
            createUser( this.requireContext())
        }
    }

    fun saveName(context: Context){
        val userDefaults: SharedPreferences = context.getSharedPreferences(NAME_OBJECT_KEY, 0)
        val editor = userDefaults.edit()
        editor.putString(NAME_OBJECT_KEY, nameTextView.text.toString())
        editor.commit()
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

    private fun createUser(context: Context){
        val okHttpClient = OkHttpClient ()
        val currentUserFirebaseAuthID = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserFirebaseAuthID != null) {
            Log.d(TAG, currentUserFirebaseAuthID)
        } else {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener {
                    Log.d(TAG, "FIREBASE ID: ${it.credential}")
                }
                .addOnFailureListener {
                    Log.d(TAG, "FIREBASE Failure: $it")
                }
        }

        //time to get token security data
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
                Log.d(TAG, "authorizationToken = $authorizationToken")
                if (authorizationToken != null) {
                    if (currentUserFirebaseAuthID != null) {
                        val userName = getUserName(context)
                        mutateNewUser(userName, currentUserFirebaseAuthID, authorizationToken)
                        Log.d(TAG, "The new user name is: $userName, the new firebase auth id is: $currentUserFirebaseAuthID, the new token is: $authorizationToken")
                    }
                }
            }
        })
    }

    fun mutateNewUser(userName: String, firebaseAuthId: String, authToken: String){
        val mutationUrl = "{\"query\":\"mutation NewUserMutation {\\n  insert_users_one(object: {auth_id: \\\"$firebaseAuthId\\\", display_name: \\\"$userName\\\"}) {\\n    auth_id\\n    display_name\\n    user_login_name\\n    user_login_password\\n    user_id\\n  }\\n}\",\"variables\":{}}"
//        val mutationUrl = "{\"query\":\"mutation NewUserMutation {\\n  insert_users_one(object: {auth_id: \\\"$firebaseAuthId\\\", display_name: \\\"$userName\\\"}) {\\n    auth_id\\n    display_name\\n    user_login_name\\n    user_login_password\\n  }\\n}\",\"variables\":{}}"
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
                val userDefaults: SharedPreferences = requireContext().getSharedPreferences(ON_BOARDING_KEY, 0)
                val editor = userDefaults.edit()
                editor.putString(ON_BOARDING_KEY, "true")
                editor.commit()

                val response = response.body?.string()
                Log.d(TAG, "Response From OurServer: = $response")
                val jsonObject = JSONObject (response)
                val data = jsonObject.getJSONObject("data")
                val insertUsersOne = data.getJSONObject("insert_users_one")
                authId = insertUsersOne.getString("auth_id")
                displayName = insertUsersOne.getString("display_name")
                userId = insertUsersOne.getString("user_id")
                bundle.putString("authId", authId)
                bundle.putString("displayName", displayName)
                bundle.putString("userId", userId)
                bundle.putString("authToken", authToken)

                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post(object : Runnable {
                    override fun run() {
                        val navController = findNavController()
                        navController.navigate(R.id.action_onboard1_to_onboard_2, bundle)
                    }
                })
            }
        })
    }
}