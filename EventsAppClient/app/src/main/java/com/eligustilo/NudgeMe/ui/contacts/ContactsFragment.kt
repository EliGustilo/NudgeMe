package com.eligustilo.NudgeMe.ui.contacts

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eligustilo.NudgeMe.DataManager
import com.eligustilo.NudgeMe.R

class ContactsFragment : Fragment() {
    private var TAG = "ContactsFragment"

    private lateinit var contactsViewModel: ContactsViewModel
    private var contactsFragmentRecyclerAdapter: ContactsFragmentRecylcerAdapter? = null
    private lateinit var contactsRecyclerView: RecyclerView
    lateinit var userID: String
    lateinit var userName: String

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        //inflating view
        contactsViewModel = ViewModelProviders.of(this).get(ContactsViewModel::class.java)
        val root = inflater.inflate(R.layout.contacts_fragment, container, false)
        contactsRecyclerView = root.findViewById(R.id.contacts_recycler_view)

        contactsFragmentRecyclerAdapter = null

        //dealing with ViewModel
        contactsViewModel.mutableContactData.observe(viewLifecycleOwner, Observer {
            val parsedData = it
            val incomingUserDetails = DataManager.getUserDetails()
            if (incomingUserDetails != null){
                userID = incomingUserDetails.userId
                userName = incomingUserDetails.userName
                Log.d(TAG, "data given to ContactsViewModel is: $parsedData")
            }

            //setting the recycler adapter
            contactsRecyclerView.layoutManager = LinearLayoutManager(this.activity)
            if( contactsFragmentRecyclerAdapter == null) {
                contactsFragmentRecyclerAdapter = ContactsFragmentRecylcerAdapter(parsedData, this, this.requireContext())
                contactsRecyclerView.adapter = contactsFragmentRecyclerAdapter
                contactsFragmentRecyclerAdapter!!.notifyDataSetChanged()
            } else {
                contactsFragmentRecyclerAdapter!!.updateData(parsedData)
            }
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //This code sets the top actionBar on the Contacts Fragment
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