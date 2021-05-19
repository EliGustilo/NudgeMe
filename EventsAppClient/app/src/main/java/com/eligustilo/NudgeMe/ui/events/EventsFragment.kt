package com.eligustilo.NudgeMe.ui.events


import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eligustilo.NudgeMe.*

class EventsFragment : Fragment() {
    private val TAG = "HomeFragment"
    private lateinit var eventsViewModel: EventsViewModel
    private var eventsFragmentRecyclerAdapter: DateFragmentRecyclerAdapter? = null
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var buttonOnboarding: Button
    private lateinit var inviteButton: Button
    private lateinit var newEventButton: Button
    private lateinit var userID: String
    private lateinit var userName: String
    val ON_BOARDING_KEY = "ON_BOARDING_KEY"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        eventsViewModel = ViewModelProviders.of(this).get(EventsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_event, container, false)

        eventsRecyclerView = root.findViewById(R.id.homeFragmentRecyclerView)

        eventsFragmentRecyclerAdapter = null
        eventsViewModel.mutableHomeData.observe(viewLifecycleOwner, Observer {
            val incomingUserDetails = DataManager.getUserDetails()
            if (incomingUserDetails != null){
                userID = incomingUserDetails.userId
                userName = incomingUserDetails.userName
            }

            val friendsDetailsArray = it
            var eventsArray = ArrayList<EventsDetails>()
            for (friends in friendsDetailsArray){
                val events = friends.userEvents
                eventsArray.addAll(events)
            }

            //TODO how does this work exactly
            eventsArray.sortWith(object: Comparator<EventsDetails>{
                override fun compare(event1: EventsDetails, event2: EventsDetails): Int = when {
                    DataManager.getEventInMilliseconds(event1) > DataManager.getEventInMilliseconds(event2) -> 1
                    DataManager.getEventInMilliseconds(event1) == DataManager.getEventInMilliseconds(event2) -> 0
                    else -> -1
                }
            })

            eventsRecyclerView.layoutManager = LinearLayoutManager(this.activity)
            if( eventsFragmentRecyclerAdapter == null) {
                eventsFragmentRecyclerAdapter = DateFragmentRecyclerAdapter(
                    eventsArray,
                    this,
                    this.requireContext()
                )
                eventsRecyclerView.adapter = eventsFragmentRecyclerAdapter
                eventsFragmentRecyclerAdapter!!.notifyDataSetChanged()
            } else {
                eventsFragmentRecyclerAdapter!!.updateDataHome(eventsArray)
            }
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Sets top ActionBar
        val mainActivity = activity as AppCompatActivity?
        mainActivity?.supportActionBar?.setCustomView(R.layout.action_bar_custom)
        val actionBarTitle = mainActivity?.supportActionBar?.customView?.findViewById<TextView>(R.id.action_bar_title)
        actionBarTitle?.setText("NudgeMe")
        val actionBarImage = mainActivity?.supportActionBar?.customView?.findViewById<ImageView>(R.id.action_bar_image)
        actionBarImage?.setImageResource(R.drawable.icon_main_blue)
        mainActivity?.supportActionBar?.setDisplayShowCustomEnabled(true)
        val colorDrawable = ColorDrawable(Color.parseColor("#0f81c7"))
        mainActivity?.supportActionBar?.setBackgroundDrawable(colorDrawable)

    }
}