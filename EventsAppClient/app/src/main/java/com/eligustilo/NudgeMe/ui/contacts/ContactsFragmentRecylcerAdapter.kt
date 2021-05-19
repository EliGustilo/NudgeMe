package com.eligustilo.NudgeMe.ui.contacts

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.eligustilo.NudgeMe.*
import com.eligustilo.NudgeMe.ui.EventDetailsActivity
import com.eligustilo.NudgeMe.ui.SharingManager
import com.google.firebase.auth.FirebaseAuth


class ViewHolderHEADER(viewHeader: View, contactsFragment: ContactsFragment): RecyclerView.ViewHolder(viewHeader){
    var headerCell: TextView = viewHeader.findViewById(R.id.recyclerViewHeaderCell)
    var addEventButton: TextView = viewHeader.findViewById(R.id.newEventContactsButton)
    var contactsConstraintLayout: ConstraintLayout = viewHeader.findViewById(R.id.contacts_name_constraint_layout)
    var headerAvatar: TextView = viewHeader.findViewById(R.id.contacts_header_avatar)
    private var context : Context = super.itemView.context
    private var TAG = "ContactsFragmentRecyclerAdapter"
    lateinit var friendUserId: String
    lateinit var friendDetail: FriendsDetails

    init {
        addEventButton.isClickable
        // Init context
        context = super.itemView.context
        // OnClick Button Handler
        addEventButton.setOnClickListener{
            Log.d(TAG, "Test for Event Cell button Click")
            val bundle = Bundle()
            bundle.putString("display_id", friendUserId)
            bundle.putString("friend_id", friendDetail.friendID)
            bundle.putString("friend_name", friendDetail.displayName)
            contactsFragment.findNavController().navigate(R.id.action_navigation_contacts_to_contacts_new_event_fragment, bundle)

        }
    }
}

class ViewHolderEVENT(viewHeader: View): RecyclerView.ViewHolder(viewHeader){
    var eventNameCell: TextView = viewHeader.findViewById(R.id.contactsRecyclerViewDetailsCell)
    //var eventDateCell: TextView = viewHeader.findViewById(R.id.contactsEventDate)
//    var reminderButton: Button = viewHeader.findViewById(R.id.setReminderContactsFragmentButton)
    var contactsEventDetailsCell: ConstraintLayout = viewHeader.findViewById(R.id.contactsEventDetailsCell)
    val userName = DataManager.getUserDetails()?.userName
    val userId = DataManager.getUserDetails()?.userId
    lateinit var eventId: String
    lateinit var eventNameForReminderActivity: String
    lateinit var eventDateForReminderActivity: String
    lateinit var eventSelected: EventsDetails
    private var trashButton: ImageView = viewHeader.findViewById(R.id.trashContactsRecyclerView)
    private var context : Context = super.itemView.context
    private var TAG = "ContactsFragmentRecyclerAdapter"
    lateinit var eventsReminderArray: ArrayList<Reminders>
    var eventsRemindersTimes: ArrayList<Int> = arrayListOf(1,2)
    var eventsRemindersId: ArrayList<String> = arrayListOf()
    var ownerName = ""



    init {
        // Init context
        contactsEventDetailsCell.isClickable
        // OnClick Button Handler for EventDetails
        contactsEventDetailsCell.setOnClickListener{
            val intent = Intent(this.context, EventDetailsActivity::class.java)
            intent.putExtra("userName", userName)
            intent.putExtra("userId", userId)
            intent.putExtra("eventName", eventNameForReminderActivity)
            intent.putExtra("eventDate", eventDateForReminderActivity)
            intent.putExtra("eventId", eventId)
            intent.putExtra("ownerName", ownerName)
            intent.putIntegerArrayListExtra("reminderTimesArray", eventsRemindersTimes)
            intent.putStringArrayListExtra("reminderIdArray", eventsRemindersId)
            context.startActivity(intent)
        }
    }

    init {
        trashButton.isClickable
        trashButton.setOnClickListener(){
            Log.d(TAG, "Trash got clicked")
            val builderTrash = AlertDialog.Builder(context)
            builderTrash.setTitle("Are you sure you want to delete?")

            builderTrash.setPositiveButton("Yes"){ dialog, which2 ->
                DataManager.deleteEvent(eventSelected)
            }
            builderTrash.setNegativeButton("No", null)
            val dialog2 = builderTrash.create()
            dialog2.show()
        }
    }
}

class ViewHolderTutorial(viewHeader: View, activity: Activity): RecyclerView.ViewHolder(viewHeader){
    var inviteFriendsButon = viewHeader.findViewById<Button>(R.id.tutorialInviteFriendsButton)
    var currentUserFirebaseAuthID = FirebaseAuth.getInstance().currentUser?.uid
    private var context : Context = super.itemView.context
    val sharingManager = SharingManager(context)

    init {
        inviteFriendsButon.setOnClickListener(){
            val dataManager = DataManager
            val userDetails = dataManager.getUserDetails()
            if (userDetails != null) {
                if (currentUserFirebaseAuthID != null) {
//                    val activity1 = Activity() // why is this null on creation?
                    sharingManager.inviteFriends(currentUserFirebaseAuthID!!, userDetails.userId, userDetails.userName, activity)
                }
            }
        }
    }
}


enum class DataType(val typeKey: Int) {
    HEADER(1),
    EVENT(2),
    TUTORIAL(3),
}

data class SectionDataClass(//this is nasty need to clarify naming on variables
    val type: String,//is it an event or friend. type is either event or friend
    val friendDetail: FriendsDetails?,
    val eventDetail: EventsDetails?,
    var highlighted: Boolean,
    var shadowed: Boolean
)

class ContactsFragmentRecylcerAdapter(
    var friendsDetailsArray: ArrayList<FriendsDetails>,
    contactsFragment: ContactsFragment,
    var context: Context
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var simplifiedSectionArray = ArrayList<SectionDataClass>()
    private var contactsFragment = contactsFragment

    override fun getItemCount(): Int {
        simplifiedSectionArray.clear()
        for (friendDetail in friendsDetailsArray){
            //this is the header
            if(friendDetail.userEvents.size > 0) { // if no events then ignore friend
                val friendSection =
                    SectionDataClass(
                        "Friend",
                        friendDetail,
                        null,
                        false,
                        false
                    )
                simplifiedSectionArray.add(friendSection)
                var index = 0
                var highlight = false
                var shadowed = false

                for(event in friendDetail.userEvents) {
                    //this is the events under the friends
                    if (index % 2 == 0){
                        highlight = false
                    } else {
                        highlight = true
                    }

                    var lastEventCount = friendDetail.userEvents.count() -1 //minus one because .count starts at one not zero

                    if (lastEventCount == index){
                        shadowed = true
                    } else {
                        shadowed = false
                    }

                    val eventSection =
                        SectionDataClass(
                            "Event",
                            null,
                            event,
                            highlight,
                            shadowed
                        )
                    simplifiedSectionArray.add(eventSection)
                    index += 1
                }
            }
        }

    if(simplifiedSectionArray.size == 0) { //add tutorial
        val tutorialData = SectionDataClass(
            "Tutorial",
            null,
            null,
            true,
            false
        )
        simplifiedSectionArray.add(tutorialData)
    }

        return simplifiedSectionArray.size
    }

    override fun getItemViewType(position: Int): Int {//Gets the viewType for onCreateViewHolder
        val simplifiedDataBindViewHolderNeeds = simplifiedSectionArray[position]

        when (simplifiedDataBindViewHolderNeeds.type) {
            "Friend" -> {
                return DataType.HEADER.typeKey
            }
            "Event"-> {
                return DataType.EVENT.typeKey
            }
            "Tutorial"-> {
                return DataType.TUTORIAL.typeKey

            }
        }
        //requried default
        return 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            DataType.HEADER.typeKey -> {
                val headerCellView = LayoutInflater.from(parent.context).inflate(
                    R.layout.contacts_recycler_view_header_cell,
                    parent,
                    false
                )
                return ViewHolderHEADER(
                    headerCellView,
                    contactsFragment
                )
            }
            DataType.EVENT.typeKey -> {
                val eventCellView = LayoutInflater.from(parent.context).inflate(
                    R.layout.contacts_recycler_view_details_cell,
                    parent,
                    false
                )
                return ViewHolderEVENT(
                    eventCellView
                )
            }
            DataType.TUTORIAL.typeKey -> {
                val tutorialCellView = LayoutInflater.from(parent.context).inflate(R.layout.tutorial_cell, parent, false)
                return ViewHolderTutorial(tutorialCellView, contactsFragment.requireActivity())
            }
        }
        //this is a required default that should never happen.
        val eventCellView = LayoutInflater.from(parent.context).inflate(
            R.layout.contacts_recycler_view_details_cell,
            parent,
            false
        )
        return ViewHolderHEADER(eventCellView, contactsFragment)
    }


    // get correct view holder, get data, fill view holder with data
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val simplifiedDataBindViewHolderNeeds = simplifiedSectionArray[position]
        when (simplifiedDataBindViewHolderNeeds.type) {
            "Friend" -> {
                //this checks if its a header or not
                val viewHolder = holder as ViewHolderHEADER
                val friendDetail = simplifiedDataBindViewHolderNeeds.friendDetail
                bindFriend(viewHolder, friendDetail)
            }
            "Event" -> {
                val viewHolder = holder as ViewHolderEVENT
                val eventData = simplifiedDataBindViewHolderNeeds.eventDetail
                bindEvent(viewHolder, eventData, simplifiedDataBindViewHolderNeeds)
                if (simplifiedDataBindViewHolderNeeds.eventDetail?.ReminderArray != null){
                    viewHolder.ownerName = simplifiedDataBindViewHolderNeeds.eventDetail.friendName
                    val eventsReminderArray = simplifiedDataBindViewHolderNeeds.eventDetail.ReminderArray
                    viewHolder.eventsReminderArray = eventsReminderArray
                    viewHolder.eventsRemindersTimes.clear()
                    viewHolder.eventsRemindersId.clear()
                    for (reminder in eventsReminderArray){
                        viewHolder.eventsRemindersTimes.add(reminder.reminder)
                        viewHolder.eventsRemindersId.add(reminder.reminderID)
                    }
                }
            }
            "Tutorial" -> {
                bindTutorial()
                //TODO add custom tutorial stuff
            }
        }
    }

    private fun bindFriend(viewHolder: ViewHolderHEADER, friendDetail: FriendsDetails? ) {
        if (friendDetail != null){
            viewHolder.friendDetail = friendDetail
            viewHolder.friendUserId = friendDetail.friendID
            viewHolder.headerCell.text = friendDetail.displayName
            if(friendDetail.displayName.length > 0) {
                val displayAvatarLetter = friendDetail.displayName.substring(0, 1).capitalize()
                viewHolder.headerAvatar.text = displayAvatarLetter
            } else {
                viewHolder.headerAvatar.text = "?"
            }

            val topCellCurved: Drawable? = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.top_cell_curved_blue,
                null
            )
            viewHolder.contactsConstraintLayout.setBackgroundDrawable(topCellCurved)
        }
    }

    private fun bindEvent(viewHolder: ViewHolderEVENT, eventData: EventsDetails?, simplifiedDataBindViewHolderNeeds: SectionDataClass) {
        //This is a way to clean up code. Put sub code for OnBindViewHolder here and then just call function.
        var sizeInDP = 16

        var marginInPixel = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            sizeInDP.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        if (eventData != null){
            viewHolder.eventId = eventData.EventId
            viewHolder.eventSelected = eventData
            val dateDisplay = DataManager.getEventDateAsString(eventData)
            if (simplifiedDataBindViewHolderNeeds.highlighted == true){
                //checks highlights
                if(eventData.ReminderArray.size > 0) {
                    val bell = String(Character.toChars(0x1F514))
                    viewHolder.eventNameCell.text = "$bell ${eventData.EventName} - ${dateDisplay}"
                } else {
                    viewHolder.eventNameCell.text = "${eventData.EventName} - ${dateDisplay}"
                }
                viewHolder.eventNameForReminderActivity = eventData.EventName
                viewHolder.eventDateForReminderActivity = eventData.EventDate
                viewHolder.contactsEventDetailsCell.setBackgroundColor(
                    context.resources.getColor(
                        R.color.recyclerGrey
                    )
                )
            } else {
                if(eventData.ReminderArray.size > 0) {
                    val bell = String(Character.toChars(0x1F514))
                    viewHolder.eventNameCell.text = "$bell ${eventData.EventName} - ${dateDisplay}"
                } else {
                    viewHolder.eventNameCell.text = "${eventData.EventName} - ${dateDisplay}"
                }

                viewHolder.eventNameForReminderActivity = eventData.EventName
                viewHolder.eventDateForReminderActivity = eventData.EventDate
                viewHolder.contactsEventDetailsCell.setBackgroundColor(
                    context.resources.getColor(
                        R.color.recyclerWhite
                    )
                )
            }
        }

        if (simplifiedDataBindViewHolderNeeds.shadowed == true){
            viewHolder.contactsEventDetailsCell.elevation = 30f
            setMargins(viewHolder.contactsEventDetailsCell, marginInPixel, 0, marginInPixel, marginInPixel)

            if(simplifiedDataBindViewHolderNeeds.highlighted == true){
                //applies highlight or not to end cell
                val endCellGrey: Drawable? = ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.end_cell_curved_grey,
                    null
                )
                viewHolder.contactsEventDetailsCell.setBackgroundDrawable(endCellGrey)
            } else {
                val endCellWhite: Drawable? = ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.end_cell_curved_white,
                    null
                )
                viewHolder.contactsEventDetailsCell.setBackgroundDrawable(endCellWhite)
            }
        } else {
            viewHolder.contactsEventDetailsCell.elevation = 0f
            setMargins(viewHolder.contactsEventDetailsCell, marginInPixel, 0, marginInPixel, 0)
        }
    }

    private fun bindTutorial() {
        //TODO set tutorial stuff
    }

    fun updateData(newFriendsDetailsArray: ArrayList<FriendsDetails>) {
        friendsDetailsArray = newFriendsDetailsArray
        this.notifyDataSetChanged()
    }


    private fun setMargins(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (view.layoutParams is MarginLayoutParams) {
            val p = view.layoutParams as MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }
}