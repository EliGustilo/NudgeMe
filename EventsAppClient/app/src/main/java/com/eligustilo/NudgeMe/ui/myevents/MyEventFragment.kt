package com.eligustilo.NudgeMe.ui.myevents

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eligustilo.NudgeMe.DataManager
import com.eligustilo.NudgeMe.MyEventUserDetails
import com.eligustilo.NudgeMe.R


class MyEventFragment: Fragment() {
    private var TAG = "MyEvent"
    private lateinit var myEventViewModel: MyEventsViewModel
    private lateinit var myEventsRecyclerView: RecyclerView
    private var myEventFragmentRecyclerAdapter: MyEventsRecyclerAdapater? = null
    private lateinit var myEventUserDetails: MyEventUserDetails
    private lateinit var myAvatar: TextView
    private lateinit var userAvatarNameTextView: TextView
    private lateinit var newPersonalEventButton: Button
    private var userDetails = DataManager.getUserDetails()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        myEventViewModel = ViewModelProviders.of(this).get(MyEventsViewModel::class.java)
        //root is the top level of the xml. In this case the constraint view.
        val root = inflater.inflate(R.layout.my_events_layout, container, false)
        myEventsRecyclerView = root.findViewById(R.id.myEventRecyclerAdapter)
        Log.d(TAG, "myEventFragment is Called")

        myEventFragmentRecyclerAdapter = null
        myEventViewModel.mutablePrivateEventData.observe(viewLifecycleOwner, Observer {
            myEventUserDetails = it
            myEventsRecyclerView.layoutManager = LinearLayoutManager(this.activity)
            if (myEventFragmentRecyclerAdapter == null) {
                //if this has not been created then create it.
                myEventFragmentRecyclerAdapter = MyEventsRecyclerAdapater(
                    myEventUserDetails,
                    this.requireContext(),
                    this
                )
                Log.d(TAG, "details to be shown are $myEventUserDetails")
                myEventsRecyclerView.adapter = myEventFragmentRecyclerAdapter
                myEventFragmentRecyclerAdapter!!.notifyDataSetChanged()
            } else {
                //it is created and we need to update or sync data.
                myEventFragmentRecyclerAdapter?.updateData(myEventUserDetails)
            }
        })
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myEventFragmentRecyclerAdapter?.notifyDataSetChanged()

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


        //this sets the avatar for the personal events. Makes the circle drawable and the makes the center text
        myAvatar = view.findViewById(R.id.myEventsAvatarImageView)
        userAvatarNameTextView = view.findViewById(R.id.userAvatarNameTextView)
        myAvatar.setBackgroundResource(R.drawable.test_drawable)
        val friendDetail = DataManager.getUserDetails()
        if (friendDetail != null){
            myAvatar.text = friendDetail.userName
            if(friendDetail.userName.length > 0) {
                val displayAvatarLetter = friendDetail.userName.substring(0, 1).capitalize()
                userAvatarNameTextView.text = displayAvatarLetter
            } else {
                userAvatarNameTextView.text = "?"
            }

            val avatarCircle: Drawable? = context?.resources?.let {
                ResourcesCompat.getDrawable(
                    it,
                    R.drawable.avatar_circle_medium,
                    null
                )
            }
            myAvatar.setBackgroundDrawable(avatarCircle)
        }


        //this sets up the newEvent Button
        newPersonalEventButton = view.findViewById(R.id.myPersonalEventNewEventButton)
        newPersonalEventButton.setOnClickListener{
            this.findNavController().navigate(R.id.my_event_fragment_to_my_new_event_fragment)
        }
    }
}