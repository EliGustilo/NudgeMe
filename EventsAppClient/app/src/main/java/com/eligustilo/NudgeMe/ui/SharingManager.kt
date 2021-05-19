package com.eligustilo.NudgeMe.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ShareCompat
import com.eligustilo.NudgeMe.DataManager
import com.eligustilo.NudgeMe.R
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.dynamiclinks.ktx.socialMetaTagParameters
import com.google.firebase.ktx.Firebase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*


class SharingManager(var context: Context) {
    private var authToken = DataManager.authToken
    private var TAG = "SharingManager"
    private var todayCalendar: Calendar = Calendar.getInstance()
    private var todayDatePlusOne = todayCalendar.add(Calendar.DAY_OF_YEAR, 2)//todayDatePlusOne is equal to the result of the todayCalendar.add which .add always null
    private var time = todayCalendar.time

    fun inviteFriends(
        firebaseAuthId: String,
        userID: String,
        displayName: String,
        launchingActivity: Activity
    ){
        val builderInvite = AlertDialog.Builder(context)
        val title = launchingActivity.resources.getString(R.string.single_contact_title)
        val msg = launchingActivity.resources.getString(R.string.single_contact_msg)
        builderInvite.setTitle(title)
        builderInvite.setMessage(msg)
        val continueBtnText = launchingActivity.resources.getString(R.string.single_continue)
        builderInvite.setPositiveButton(continueBtnText){ dialog, which2 ->
            Log.d(TAG, "the today time is $time")
            val okHttpClient = OkHttpClient ()
            val url = "{\"query\":\"mutation requestRequest {\\n  insert_requests(objects: {user_id: \\\"$userID\\\", exp_date: \\\"${time}\\\"}) {\\n    returning {\\n      request_id\\n    }\\n  }\\n}\\n\",\"variables\":{}}"
            val body: RequestBody = url.toRequestBody("application/json".toMediaTypeOrNull())
            authToken = DataManager.authToken // prevents a crash because authToken may not exist on startup
            val okHttpRequest = Request.Builder()
                .url("https://reminder-app-server.herokuapp.com/v1/graphql")
                .post(body)
                .addHeader("Authorization", "Bearer " + authToken)
                .build()//This sends the body as a post to the url and gets back GRAPHQL data.

            okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                    val hasuraJSON = response.body?.string()
                    if (hasuraJSON != null) {
                        Log.d(TAG, "hasuraJSON is $hasuraJSON")
                        val jsonObject = JSONObject(hasuraJSON)
                        val topDataObject = jsonObject.getJSONObject("data")
                        val insert_request = topDataObject.getJSONObject("insert_requests")
                        val returning: JSONArray = insert_request.getJSONArray("returning")
                        val requestId = returning[0] as JSONObject
                        val item = requestId.getString("request_id")
                        Log.d(TAG, "the request id is $item")
                        createDynamicLink(userID, displayName, launchingActivity, item)
                    }
                }
            })
        }
        val cancelBtnText = launchingActivity.resources.getString(R.string.single_cancel)
        builderInvite.setNegativeButton(cancelBtnText, null)

        val manyBtnText = launchingActivity.resources.getString(R.string.single_many_friends)
        builderInvite.setNeutralButton(manyBtnText) { dialog, which2 ->
            val url = "mailto:developer.eli.nudgeme@gmail.com?subject=Add%20Support%20For%20Many%20Friends"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            launchingActivity.startActivity(intent)
        }
        val dialog2 = builderInvite.create()
        dialog2.show()
    }

    fun inviteFriendsOnboarding(
        firebaseAuthId: String,
        userID: String,
        displayName: String,
        launchingActivity: Activity,
        authTokenOnBoarding: String
    ){
        val builderInvite = AlertDialog.Builder(context)
        val title = launchingActivity.resources.getString(R.string.single_contact_title)
        val msg = launchingActivity.resources.getString(R.string.single_contact_msg)
        builderInvite.setTitle(title)
        builderInvite.setMessage(msg)
        val continueBtnText = launchingActivity.resources.getString(R.string.single_continue)
        builderInvite.setPositiveButton(continueBtnText){ dialog, which2 ->
            val okHttpClient = OkHttpClient ()
            val url = "{\"query\":\"mutation requestRequest {\\n  insert_requests(objects: {user_id: \\\"$userID\\\", exp_date: \\\"${time}\\\"}) {\\n    returning {\\n      request_id\\n    }\\n  }\\n}\\n\",\"variables\":{}}"
            val body: RequestBody = url.toRequestBody("application/json".toMediaTypeOrNull())

            val okHttpRequest = Request.Builder()
                .url("https://reminder-app-server.herokuapp.com/v1/graphql")
                .post(body)
                .addHeader("Authorization", "Bearer " + authTokenOnBoarding)
                .build()//This sends the body as a post to the url and gets back GRAPHQL data.

            okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                    val hasuraJSON = response.body?.string()
                    if (hasuraJSON != null) {
                        Log.d(TAG, "hasuraJSON is $hasuraJSON")
                        val jsonObject = JSONObject(hasuraJSON)
                        val topDataObject = jsonObject.getJSONObject("data")
                        val insert_request = topDataObject.getJSONObject("insert_requests")
                        val returning: JSONArray = insert_request.getJSONArray("returning")
                        val requestId = returning[0] as JSONObject
                        val item = requestId.getString("request_id")
                        Log.d(TAG, "the request id is $item")
                        createDynamicLink(userID, displayName, launchingActivity, item)
                    }
                }
            })
        }
        val cancelBtnText = launchingActivity.resources.getString(R.string.single_cancel)
        builderInvite.setNegativeButton(cancelBtnText, null)

        val manyBtnText = launchingActivity.resources.getString(R.string.single_many_friends)
        builderInvite.setNeutralButton(manyBtnText) { dialog, which2 ->
            val url = "mailto:developer.eli.nudgeme@gmail.com?subject=Add%20Support%20For%20Many%20Friends"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            launchingActivity.startActivity(intent)
        }
        val dialog2 = builderInvite.create()
        dialog2.show()
    }

    fun inviteFriendsNoAlert(
        firebaseAuthId: String,
        userID: String,
        displayName: String,
        launchingActivity: Activity
    ){
        val okHttpClient = OkHttpClient ()
        val url = "{\"query\":\"mutation requestRequest {\\n  insert_requests(objects: {user_id: \\\"$userID\\\", exp_date: \\\"${time}\\\"}) {\\n    returning {\\n      request_id\\n    }\\n  }\\n}\\n\",\"variables\":{}}"
        val body: RequestBody = url.toRequestBody("application/json".toMediaTypeOrNull())

        val okHttpRequest = Request.Builder()
            .url("https://reminder-app-server.herokuapp.com/v1/graphql")
            .post(body)
            .addHeader("Authorization", "Bearer " + authToken)
            .build()//This sends the body as a post to the url and gets back GRAPHQL data.

        okHttpClient.newCall(okHttpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                //onResponse is ASYNC and the below code is all ASYNC onsecondary thread
                val hasuraJSON = response.body?.string()
                if (hasuraJSON != null) {
                    Log.d(TAG, "hasuraJSON is $hasuraJSON")
                    val jsonObject = JSONObject(hasuraJSON)
                    val topDataObject = jsonObject.getJSONObject("data")
                    val insert_request = topDataObject.getJSONObject("insert_requests")
                    val returning: JSONArray = insert_request.getJSONArray("returning")
                    val requestId = returning[0] as JSONObject
                    val item = requestId.getString("request_id")
                    Log.d(TAG, "the request id is $item")
                    createDynamicLink(userID, displayName, launchingActivity, item)
                }
            }
        })
    }

    fun createDynamicLink(
        userID: String,
        displayName: String,
        launchingActivity: Activity,
        requestId: String
    ){
        Firebase.dynamicLinks.shortLinkAsync {
            link = Uri.parse("https://www.nudgeme.today/?request_id=$requestId&user_id=$userID&name=$displayName")
            domainUriPrefix = "https://nudgeme.page.link"
            // Set parameters

            androidParameters("com.example.android") {
                minimumVersion = 125
            }

            socialMetaTagParameters {
                title = "$displayName wants to know what dates are important to you!"
                description = "NudgeMe's goal is to ensure we never forget about the things that are important to us again"
                //TODO get image drawable. Need sytnax
                val nudgeMeIcon = launchingActivity.resources.getDrawable(R.drawable.icon_main_blue)
                imageUrl = Uri.parse("https://miro.medium.com/max/1200/1*mk1-6aYaf_Bes1E3Imhc0A.jpeg")
            }

        }.addOnSuccessListener { result ->
            // Short link created
            val shortLink = result.shortLink
            //this is creating a share panel
            ShareCompat.IntentBuilder.from(launchingActivity)
                .setType("test/plain")
                .setText(shortLink.toString())
                .startChooser()
        }.addOnFailureListener {
            // Error
        }
    }
}