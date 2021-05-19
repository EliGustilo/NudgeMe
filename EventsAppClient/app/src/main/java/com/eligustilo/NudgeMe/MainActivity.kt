package com.eligustilo.NudgeMe

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.eligustilo.NudgeMe.onboarding.OnBoardingActivity
import com.eligustilo.NudgeMe.ui.SharingManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(){
    private val TAG = "MainActivity"
    private lateinit var dataManager: DataManager
    lateinit var onBoardingDone: String
    val ON_BOARDING_KEY = "ON_BOARDING_KEY"
    val currentUserFirebaseAuthID = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var currentDate = Calendar.getInstance().time
    private var inviteBetaCount = 0
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var  newLoggedInUserId: String
    lateinit var  newLoggedInAuthId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //sets Google Login Stuf
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // Check for existing Google Sign In account, if the user is already signed in
    // the GoogleSignInAccount will be non-null.
        // Check for existing Google Sign In account, if the user is already signed in
    // the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(this)
        Log.d(TAG, "did they sign in is $account")

        //set top action bar
        val mainActivity = this
        mainActivity?.supportActionBar?.setCustomView(R.layout.action_bar_custom)
        val actionBarTitle = mainActivity?.supportActionBar?.customView?.findViewById<TextView>(R.id.action_bar_title)
        actionBarTitle?.setText("NudgeMe")
        val actionBarImage = mainActivity?.supportActionBar?.customView?.findViewById<ImageView>(R.id.action_bar_image)
        actionBarImage?.setImageResource(R.drawable.icon_main_blue)
        mainActivity?.supportActionBar?.setDisplayShowCustomEnabled(true)
        val colorDrawable = ColorDrawable(Color.parseColor("#0f81c7"))
        mainActivity?.supportActionBar?.setBackgroundDrawable(colorDrawable)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setItemIconSizeRes(R.dimen.bottom_bar_icon_size)

        val navController = findNavController(R.id.nav_host_fragment)//TODO why is this going to activity_main and not mobile_navigation.xml?
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_event, R.id.navigation_contacts, R.id.my_event_fragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // load data manager(s)
        DataManager.initWith(this)
        MyEventDataManager.initWith(this)

//        Onboarding Code
        val userDefaults: SharedPreferences = applicationContext.getSharedPreferences(
            ON_BOARDING_KEY,
            0
        )
        onBoardingDone = userDefaults.getString(ON_BOARDING_KEY, null).toString()
        if (onBoardingDone != "true"){
            onBoardingDone = "true"
            val intent = Intent(this, OnBoardingActivity::class.java)
            this.startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {

            R.id.inviteFriendsMenuItem -> {
                if (inviteBetaCount < 3) {
                    //beta alert has not been seen 3 times
                    inviteBetaCount = inviteBetaCount + 1
                    Log.d(TAG, "Beta Count = $inviteBetaCount")
                    val sharingManager = SharingManager(this)
                    dataManager = DataManager
                    val userDetails = dataManager.getUserDetails()
                    if (userDetails != null) {
                        if (currentUserFirebaseAuthID != null) {
                            sharingManager.inviteFriends(
                                currentUserFirebaseAuthID,
                                userDetails.userId,
                                userDetails.userName,
                                this
                            )
                        }
                    }
                } else {
                    //beta alert has been seen 3 times
                    val sharingManager = SharingManager(this)
                    dataManager = DataManager
                    val userDetails = dataManager.getUserDetails()
                    if (userDetails != null) {
                        if (currentUserFirebaseAuthID != null) {
                            sharingManager.inviteFriendsNoAlert(
                                currentUserFirebaseAuthID,
                                userDetails.userId,
                                userDetails.userName,
                                this
                            )
                        }
                    }
                }
                true
            }

            R.id.tosMenuItem -> {
                val url = "https://www.nudgeme.today/terms-and-conditions"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
                true
            }

            R.id.privacyMenuItem -> {
                val url = "https://www.nudgeme.today/privacy-policy"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
                true
            }

            R.id.feedbackMenuItem -> {
                val url = "mailto:developer.eli.nudgeme@gmail.com?subject=Feedback%20$currentDate"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
                true
            }

            R.id.loginItem -> {
                // Choose authentication providers
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.PhoneBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )

                // Create and launch sign-in intent
                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                    0
                )
                true
            }

            R.id.logoutItem -> {
                FirebaseAuth.getInstance().signOut()
                FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener {
                        Log.d(TAG, "FIREBASE ID: ${it.credential}")
                        DataManager.currentUserFirebaseAuthID ="$it.credential"
                        DataManager.getAuthToken("https://reminder-app-server.herokuapp.com/v1/graphql")
                    }
                    .addOnFailureListener {
                        Log.d(TAG, "FIREBASE Failure: $it")
                    }
                DataManager.getFirebaseId("https://reminder-app-server.herokuapp.com/v1/graphql")
                Log.d(TAG, "logged out firebase ID = $currentUserFirebaseAuthID")
                true
            }

            //overflow. Should never be called.
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //Login into Google for account info, using Firebase.
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            val response = IdpResponse.fromResultIntent(data)
            Log.d(TAG, "Response from login onActivityResult is $response")
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
              queryForExistingFirebaseAuthId()
            } else {
                Log.d(TAG, "Sign In Failed")

                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }


    private fun queryForExistingFirebaseAuthId (){
        Log.d(TAG, "The new logged in FirebaseAuthId = ${FirebaseAuth.getInstance().currentUser?.uid}")
        val queryExistingAuthIdString = "{\"query\":\"query MyQuery {\\n  users(where: {auth_id: {_eq: \\\"${FirebaseAuth.getInstance().currentUser?.uid}\\\"}}) {\\n    user_id\\n    auth_id\\n  }\\n}\\n\",\"variables\":{}}"
        val okHttpClient = OkHttpClient ()
        val body: RequestBody = queryExistingAuthIdString.toRequestBody("application/json".toMediaTypeOrNull())
        var returnFirebaseId = ""

        val okHttpRequest = Request.Builder()
            .url("https://reminder-app-server.herokuapp.com/v1/graphql")
            .post(body)
            .addHeader("Authorization" , "Bearer " + DataManager.authToken)
            .build()//This sends the body as a post to the url and gets back GRAPHQL data.
        okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                val response = response.body?.string()
                Log.d(TAG, "the response for queryForExisitingFirebaseID = $response")
                val jsonObject = JSONObject (response)
                val data = jsonObject.getJSONObject("data")
                val checkUsersArray = data.getJSONArray("users")

                if (checkUsersArray.length() > 0){
                    val existingUserAsJSON: JSONObject = checkUsersArray[0] as JSONObject // first only
                    val existingUserId = existingUserAsJSON.getString("user_id")
                    val currentUserId = DataManager.hasuraId
                    if(currentUserId != null) {
                        if(currentUserId != existingUserId){
                            DataManager.mergeUsersDeleteOldUser(currentUserId, existingUserId) // done
                        }else{
                            val mainHandler = Handler()
                            mainHandler.post(object : Runnable {
                                override fun run() {
//                                    mainHandler.postDelayed(this, 6000)
                                    val toast = Toast.makeText(
                                        applicationContext,
                                        "You are already logged into that account!",
                                        Toast.LENGTH_SHORT
                                    )
                                    toast.show()                                }
                            })
                        }
                    }
                }else{
                    //there is not a user with this FireBase Id being used to Login to Google. Update user with new FirebaseId
                    DataManager.currentUserFirebaseAuthID = FirebaseAuth.getInstance().currentUser?.uid.toString()
                    Log.d(TAG, "new firebase id is ${DataManager.currentUserFirebaseAuthID}")
                    DataManager.newFirebaseIdLoggedIn()
                }

                DataManager.okHTTPDataDownloader("https://reminder-app-server.herokuapp.com/v1/graphql")
            }
        })
    }


    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
//            updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
//            updateUI(null)
        }
    }
}