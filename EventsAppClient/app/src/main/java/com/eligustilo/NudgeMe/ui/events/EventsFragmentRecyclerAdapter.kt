package com.eligustilo.NudgeMe.ui.events

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.eligustilo.NudgeMe.DataManager
import com.eligustilo.NudgeMe.EventsDetails
import com.eligustilo.NudgeMe.FriendsDetails
import com.eligustilo.NudgeMe.R
import com.eligustilo.NudgeMe.ui.EventDetailsActivity
import com.eligustilo.NudgeMe.ui.SharingManager
import com.google.firebase.auth.FirebaseAuth
import kotlin.collections.ArrayList
import kotlin.coroutines.coroutineContext


class ViewHolderEventName(viewHeader: View): RecyclerView.ViewHolder(viewHeader){
    var eventNameCell: TextView = viewHeader.findViewById(R.id.homeRecyclerViewEventNameTextView)
    var datesDetailsCell: ConstraintLayout = viewHeader.findViewById(R.id.dateDetailsCell)
    private var trashButton: ImageView = viewHeader.findViewById(R.id.trashEventRecyclerView)
    private var context : Context = super.itemView.context
    private var TAG = "HomeFragmentRecyclerAdapter"
    val userName = DataManager.getUserDetails()?.userName
    val userId = DataManager.getUserDetails()?.userId
    lateinit var eventId: String
    lateinit var eventName: String
    lateinit var eventDate: String
    lateinit var eventDateOriginalForamt: String
    lateinit var eventThatIsSelected: EventsDetails
    var eventsRemindersTimes: ArrayList<Int> = arrayListOf(1,2)
    var eventsRemindersId: ArrayList<String> = arrayListOf()
    var ownerName = ""

    init {
        // Init context
        datesDetailsCell.isClickable
        // OnClick Button Handler
        datesDetailsCell.setOnClickListener{
            val intent = Intent(this.context, EventDetailsActivity::class.java)
            intent.putExtra("userName", userName)
            intent.putExtra("userId", userId)
            intent.putExtra("eventName", eventName)
            intent.putExtra("eventDate", eventDateOriginalForamt)
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
                DataManager.deleteEvent(eventThatIsSelected)
            }
            builderTrash.setNegativeButton("No", null)
            val dialog2 = builderTrash.create()
            dialog2.show()
        }
    }
}

class ViewHolderEventDate(viewHeader: View, eventsFragment: EventsFragment): RecyclerView.ViewHolder(viewHeader){
    var eventDateCell: TextView = viewHeader.findViewById(R.id.homeRecyclerViewEventDateTextView)
    var addEventButton: TextView = viewHeader.findViewById(R.id.newEventsHomeButton)
    var eventsConstraintLayout: ConstraintLayout = viewHeader.findViewById(R.id.my_event_date_contraint_layout)
    var userIcon: TextView = viewHeader.findViewById(R.id.eventsHeaderImage)
    private var context : Context
    private var TAG = "HOmeFragmentRecyclerAdapter"
    lateinit var friendDisplayId: String
    lateinit var friendDetail: FriendsDetails
    lateinit var eventThatIsSelected: EventsDetails

    init {
        addEventButton.isClickable
        // Init context
        context = super.itemView.context
        // OnClick Button Handler
        addEventButton.setOnClickListener{
            val bundle = Bundle()
            bundle.putString("display_id", friendDisplayId)
            bundle.putString("event_date", eventThatIsSelected.EventDate)
            eventsFragment.findNavController().navigate(R.id.action_navigation_event_to_event_new_event_fragment, bundle)
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

data class EventDetailsDataClass (//this is nasty need to clarify naming on variables
    val type: String,//is it an event or friend. type is either event or friend
    val eventDetail: EventsDetails?,
    val eventName: String?,
    val eventDate: String?,
    val eventId: String,
    val highlighted: Boolean,
    val shadowed: Boolean,
    val eventOwner: String
//    val eventsOwner: String?
)

enum class DataType (val typeKey: Int) {
    NAME (1),
    DATE (2),
    TUTORIAL (3)
}

class DateFragmentRecyclerAdapter(var eventDetailsArray: ArrayList<EventsDetails>, eventsFragment: EventsFragment, var context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var simplifiedSectionArray = ArrayList<EventDetailsDataClass>()
    private var TAG = "HomeFragmentRecyclerAdapter"
    var homeFragment = eventsFragment

    override fun getItemCount(): Int {
        val dataManager = DataManager
        simplifiedSectionArray.clear()
        var currentEventDate: String? = null
        var index = 0
        var highlight = false
        var shadowed = false
        var previousEventTracked: EventDetailsDataClass? = null

        for (event in eventDetailsArray){
            val displayDate = dataManager.getEventDateAsString(event)

            //below code exists to group events under common dates.
            if (currentEventDate != null){
                if (currentEventDate == displayDate){//already have a date //Do not create header and just create detail
                    highlight = !highlight //only works for boolean and means switch value. from true to false and versa
                    val eventName =
                        EventDetailsDataClass(
                            "EventName",
                            event,
                            "${event.EventName}",
                            "$displayDate",
                            event.EventId,
                            highlight,
                            shadowed,
                            event.friendName
                        )
                    index += 1
                    previousEventTracked = eventName
                    simplifiedSectionArray.add(eventName)
                } else { //create both header and details
                    //todo update shadowed event name replace in simplified section array
                    if (previousEventTracked != null) {
                        val updatedShadowCell =
                            EventDetailsDataClass(
                                "EventName",
                                previousEventTracked?.eventDetail,
                                "${previousEventTracked.eventName}",
                                "${previousEventTracked.eventDate}",
                                "${previousEventTracked.eventId}",
                                previousEventTracked.highlighted,
                                true,
                                event.friendName
                            )
                        simplifiedSectionArray.remove(previousEventTracked)
                        simplifiedSectionArray.add(updatedShadowCell)
                    }

                    currentEventDate = displayDate
                    highlight = false

                    //This is the header
                    val eventDate =
                        EventDetailsDataClass(
                            "EventDate",
                            event,
                            "${event.EventName}",
                            "$displayDate",
                            event.EventId,
                            false,
                            false,
                            event.friendName
                        )
                    //This is the detail and we handle if it is even or odd, highlighted, or the end, shadowed and curved.

                    val eventName =
                        EventDetailsDataClass(
                            "EventName",
                            event,
                            "${event.EventName}",
                            "$displayDate",
                            event.EventId,
                            highlight,
                            false,
                            event.friendName
                        )
                    index += 1
                    previousEventTracked = eventName
                    simplifiedSectionArray.add(eventDate)
                    simplifiedSectionArray.add(eventName)
                }
            } else { //create both header and details
                currentEventDate = displayDate
                highlight = false

                //This is the header
                val eventDate =
                    EventDetailsDataClass(
                        "EventDate",
                        event,
                        "${event.EventName}",
                        "$displayDate",
                        event.EventId,
                        false,
                        false,
                        event.friendName
                    )
                val eventName =
                    EventDetailsDataClass(
                        "EventName",
                        event,
                        "${event.EventName}",
                        "$displayDate",
                        event.EventId,
                        highlight,
                        false,
                        event.friendName
                    )
                index += 1
                previousEventTracked = eventName
                simplifiedSectionArray.add(eventDate)
                simplifiedSectionArray.add(eventName)
            }
        }
        if(previousEventTracked != null) {
            val updatedShadowCell =
                EventDetailsDataClass(
                    "EventName",
                    previousEventTracked?.eventDetail,
                    "${previousEventTracked.eventName}",
                    "${previousEventTracked.eventDate}",
                    "${previousEventTracked.eventId}",
                    previousEventTracked.highlighted,
                    true,
                    previousEventTracked.eventOwner
                )
            simplifiedSectionArray.remove(previousEventTracked)
            simplifiedSectionArray.add(updatedShadowCell)
        }
        Log.d(TAG, "the size of the homefragments adapter is ${simplifiedSectionArray.size}")

        if(simplifiedSectionArray.size == 0) { //add tutorial
            val tutorialData = EventDetailsDataClass(
                "Tutorial",
                null,
                "tutorial",
                "tutorial",
                "tutorial",
                false,
                true,
                "tutorial"
            )
            simplifiedSectionArray.add(tutorialData)
        }
        return simplifiedSectionArray.size
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            when (viewType) {
                DataType.NAME.typeKey -> {
                    val nameCellView = LayoutInflater.from(parent.context).inflate(R.layout.event_recylcer_view_details_cell, parent, false)
                    return ViewHolderEventName(nameCellView)
                }
                DataType.DATE.typeKey -> {
                    val eventCellView = LayoutInflater.from(parent.context).inflate(R.layout.event_recycler_view_header_cell, parent, false)
                    return ViewHolderEventDate(eventCellView, homeFragment)
                }
                DataType.TUTORIAL.typeKey -> {
                    val tutorialCellView = LayoutInflater.from(parent.context).inflate(R.layout.tutorial_cell, parent, false)
                    return ViewHolderTutorial(tutorialCellView, homeFragment.requireActivity())
                }
            }
        //}
        //this is a required default that should never happen.
        val eventCellView = LayoutInflater.from(parent.context).inflate(R.layout.event_recycler_view_header_cell, parent, false)
        return ViewHolderEventDate(eventCellView, homeFragment)
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val simplifiedDataBindViewHolderNeeds = simplifiedSectionArray[position]
        if (simplifiedDataBindViewHolderNeeds.type == "EventDate"){
            //Checks if event type is a header
            val viewHolder = holder as ViewHolderEventDate

            val eventDate = simplifiedDataBindViewHolderNeeds.eventDate
            if (simplifiedDataBindViewHolderNeeds.eventDetail != null){
                viewHolder.friendDisplayId = simplifiedDataBindViewHolderNeeds.eventDetail.displayId
                viewHolder.eventThatIsSelected = simplifiedDataBindViewHolderNeeds.eventDetail
            }

            val eventDateDayAvatar = simplifiedDataBindViewHolderNeeds.eventDetail?.let {
                DataManager.getEventDateAsDayOfWeek(
                    it
                )
            }
            //Sets the avatar for the date to the day of the week
            if(eventDate?.length!! > 0) {
                val displayAvatarLetter = eventDateDayAvatar?.substring(0, 2)?.capitalize()
                viewHolder.userIcon.text = displayAvatarLetter
            } else {
                viewHolder.userIcon.text = "?"
            }

            if (eventDate != null){
                viewHolder.eventDateCell.text = eventDate
                val topCellCurved: Drawable? = ResourcesCompat.getDrawable(context.resources, R.drawable.top_cell_curved_blue, null)
                viewHolder.eventsConstraintLayout.setBackgroundDrawable(topCellCurved)
            }
        } else if(simplifiedDataBindViewHolderNeeds.type == "Tutorial") {
            // TODO add string, actions etc.
        } else {
            //else it must be a type detail
            val viewHolder = holder as ViewHolderEventName
            val eventName = simplifiedDataBindViewHolderNeeds.eventName
            val eventOwner = simplifiedDataBindViewHolderNeeds.eventOwner
            viewHolder.eventsRemindersTimes.clear()
            viewHolder.eventsRemindersId.clear()
            if (simplifiedDataBindViewHolderNeeds.eventDetail != null){
                viewHolder.ownerName = simplifiedDataBindViewHolderNeeds.eventDetail.friendName
                viewHolder.eventDateOriginalForamt = simplifiedDataBindViewHolderNeeds.eventDetail.EventDate
                viewHolder.eventThatIsSelected = simplifiedDataBindViewHolderNeeds.eventDetail
                val remindersArray =  simplifiedDataBindViewHolderNeeds.eventDetail.ReminderArray
                for (reminder in remindersArray){
                    viewHolder.eventsRemindersTimes.add(reminder.reminder)
                    viewHolder.eventsRemindersId.add(reminder.reminderID)
                }
            }

            if (eventName != null){
                if (simplifiedDataBindViewHolderNeeds.highlighted == true){
                    //checks to see if it is odd
                    val eventDetail = simplifiedDataBindViewHolderNeeds.eventDetail
                    if(eventDetail != null && eventDetail.ReminderArray.size > 0) {
                        val bell = String(Character.toChars(0x1F514))
                        viewHolder.eventNameCell.text = "$bell ${eventName} - ${eventOwner}"
                    }
                    else {
                        viewHolder.eventNameCell.text = "${eventName} - ${eventOwner}"
                    }
                    viewHolder.eventNameCell.text = "${eventName} - ${eventOwner}"
                    viewHolder.eventName = eventName
                    viewHolder.eventDate = simplifiedDataBindViewHolderNeeds.eventDate.toString()
                    viewHolder.eventId = simplifiedDataBindViewHolderNeeds.eventId
                    viewHolder.datesDetailsCell.setBackgroundColor(context.resources.getColor(R.color.recyclerGrey))
                } else {
                    //it is even and not highlighted
                    val eventDetail = simplifiedDataBindViewHolderNeeds.eventDetail
                    if(eventDetail != null && eventDetail.ReminderArray.size > 0) {
                        val bell = String(Character.toChars(0x1F514))
                        viewHolder.eventNameCell.text = "$bell ${eventName} - ${eventOwner}"
                    }
                    else {
                        viewHolder.eventNameCell.text = "${eventName} - ${eventOwner}"
                    }
                    viewHolder.eventName = eventName
                    viewHolder.eventDate = simplifiedDataBindViewHolderNeeds.eventDate.toString()
                    viewHolder.eventId = simplifiedDataBindViewHolderNeeds.eventId
                    viewHolder.datesDetailsCell.setBackgroundColor(context.resources.getColor(R.color.recyclerWhite))
                }
            }
            if (simplifiedDataBindViewHolderNeeds.shadowed == true){
                //checks to see if its the last cell
                var sizeInDP = 16
                var marginInDp = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeInDP.toFloat(),
                    context.resources.displayMetrics
                ).toInt()
                setMargins(viewHolder.datesDetailsCell, marginInDp, 0, marginInDp, marginInDp)
                viewHolder.datesDetailsCell.elevation = 30f
                if(simplifiedDataBindViewHolderNeeds.highlighted == true){
                    //applies highlight or not to end cell
                    val endCellGrey: Drawable? = ResourcesCompat.getDrawable(context.resources, R.drawable.end_cell_curved_grey, null)
                    viewHolder.datesDetailsCell.setBackgroundDrawable(endCellGrey)
                } else {
                    val endCellWhite: Drawable? = ResourcesCompat.getDrawable(context.resources, R.drawable.end_cell_curved_white, null)
                    viewHolder.datesDetailsCell.setBackgroundDrawable(endCellWhite)
                }
            } else {
                viewHolder.datesDetailsCell.elevation = 0f
                var sizeInDP = 16
                var marginInDp = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    sizeInDP.toFloat(),
                    context.resources.displayMetrics
                ).toInt()
                setMargins(viewHolder.datesDetailsCell, marginInDp, 0, marginInDp, 0)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val simplifiedDataBindViewHolderNeeds = simplifiedSectionArray[position]

        when (simplifiedDataBindViewHolderNeeds.type){
//            "Event"->return DataType.EVENT.typeKey
            "EventName"->return DataType.NAME.typeKey
            "EventDate"->return DataType.DATE.typeKey
            "Tutorial"->return DataType.TUTORIAL.typeKey
        }
        return 1 //how do I handle this? I need an if, else, else/else statement.
    }

    fun updateDataHome(eventDetailsArrayData: ArrayList<EventsDetails>) {
        eventDetailsArray = eventDetailsArrayData
        this.notifyDataSetChanged()
    }

    private fun setMargins(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }
}