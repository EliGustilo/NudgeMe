package com.eligustilo.NudgeMe.ui.myevents

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.eligustilo.NudgeMe.*
import com.eligustilo.NudgeMe.ui.EventDetailsActivity

class MyViewHolderEventDetails(viewHeader: View): RecyclerView.ViewHolder(viewHeader){
    var eventNameCell: TextView = viewHeader.findViewById(R.id.myRecyclerViewEventNameTextView)
    var datesDetailsCell: ConstraintLayout = viewHeader.findViewById(R.id.dateDetailsCell)
    private var context : Context = super.itemView.context
    private var TAG = "MyViewHolderEventDetails"
    var changeEventUserButton: ImageView = viewHeader.findViewById(R.id.changeEventUserTextView)
    var trashButton: ImageView = viewHeader.findViewById(R.id.trash)
    val userName = DataManager.getUserDetails()?.userName
    val userId = DataManager.getUserDetails()?.userId
    lateinit var eventId: String
    lateinit var eventNameForReminderActivity: String
    lateinit var eventDateForReminderActivity: String
    lateinit var eventDateOriginalForamt: String
    lateinit var eventDetail: EventsDetails
    var eventsRemindersTimes: ArrayList<Int> = arrayListOf(1,2)
    var eventsRemindersId: ArrayList<String> = arrayListOf()
    var ownerName = ""

    init {
        // EventDetailsActivity
        datesDetailsCell.isClickable
        datesDetailsCell .setOnClickListener{
            val intent = Intent(this.context, EventDetailsActivity::class.java)
            intent.putExtra("userName", userName)
            intent.putExtra("userId", userId)
            intent.putExtra("eventName", eventNameForReminderActivity)
            intent.putExtra("eventDate", eventDateOriginalForamt)
            intent.putExtra("eventId", eventId)
            intent.putExtra("ownerName", ownerName)
            intent.putIntegerArrayListExtra("reminderTimesArray", eventsRemindersTimes)
            intent.putStringArrayListExtra("reminderIdArray", eventsRemindersId)
            context.startActivity(intent)
        }
    }

    init {
        //Buttons
        changeEventUserButton.isClickable
        trashButton.isClickable
        context = super.itemView.context

        changeEventUserButton.setOnClickListener{
            //TODO find a builder tutorial. Below code is one use of builders
            val myContactsList = DataManager.getParsedData()
            val builderContactList = ArrayList<String>()
            if (myContactsList != null) {
                for (contact in myContactsList){
                    builderContactList.add(contact.displayName)
                }
            }
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Which contact do you want to transfer this event to?")
            //syntax mean when builder is clicked it gives me the dialog, which is the spinner box that appears after the click. Which is the item inside the
            //dialog box that is selected. Aka it is the arrayOf, contactsList.
            //when statement is for when which item is selected and then does x code.
            //which is the item inside the dialog popup window. The array count position.

            //builder.setSingleChoiceItems builds the list in the dialog box from the builderContactList. The Dialog, which -> is a
            //lambeda and is meant to allow for more precise control???? Find tutorials.
            var itemSelected = 0
            builder.setSingleChoiceItems(builderContactList.toTypedArray(), -1){ dialog, which ->
                // leave empty
                Log.d(TAG, which.toString())
                itemSelected = which
            }

            //this sets the ok button need to tell it what is inside the choices list otherwise cnannot handle clicking on item and then clicking okay.
            builder.setPositiveButton("Confirm"){ dialog, which ->
                val contact = myContactsList?.get(itemSelected)
                Log.d(TAG, contact?.displayName)
                Log.d(TAG, contact?.friendID)
                // TODO: DO mutation here
                if(contact != null && userId != null) {
                    MyEventDataManager.updateEvent(eventDetail, contact.friendID)
                }
            }

            //always backs out of the builder window.
            builder.setNegativeButton("Cancel", null)

            //this instatiates the builder and finalizes it then shows it
            //dialog is a random name and not related to above dialog at all. Unsure why its called dialog. Rename
            val dialog = builder.create()
            dialog.show()
        }

        trashButton.setOnClickListener(){
            Log.d(TAG, "Trash got clicked")
            val builderTrash = AlertDialog.Builder(context)
            builderTrash.setTitle("Are you sure you want to delete?")

            builderTrash.setPositiveButton("Yes"){ dialog, which2 ->
                MyEventDataManager.deleteEvent(eventDetail)
            }
            builderTrash.setNegativeButton("No", null)
            val dialog2 = builderTrash.create()
            dialog2.show()
        }
    }
}

class MyViewHolderEventHeader(viewHeader: View, myEventsFragment: MyEventFragment): RecyclerView.ViewHolder(viewHeader){
    var eventDateCell: TextView = viewHeader.findViewById(R.id.myEventHeaderTextView)
    var eventsConstraintLayout: ConstraintLayout = viewHeader.findViewById(R.id.my_event_date_contraint_layout)
    var userIcon: TextView = viewHeader.findViewById(R.id.myEventsHeaderImage)
    private var TAG = "HOmeFragmentRecyclerAdapter"


}

data class MyEventDetailsDataClass (
    val type: String?,
    val eventDetail: EventsDetails?,
    val eventName: String?,
    val eventDate: String?,
    val eventId: String,
    val highlighted: Boolean,
    val shadowed: Boolean
//    val eventsOwner: String?
)

enum class DataType (val typeKey: Int) {
    NAME (1),
    DATE (2)
}

class MyEventsRecyclerAdapater(var myEventUserDetails: MyEventUserDetails, var context: Context, myEventsFragment: MyEventFragment): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var myFlatenedSectionArray = ArrayList<MyEventDetailsDataClass>()
    private var TAG = "MyEventsRecyclerAdapater"
    private var dataManager = DataManager
    var myEventFragment = myEventsFragment
    var shouldReloadData = true

    init {
        notifyDataSetChanged()
    }

    //First we need to get the item count. Aka how many events
    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount Called")
        var currentEventDate: String? = null
        var index = 0
        var highlight = false
        var shadowed = false
        var previousEventTracked: MyEventDetailsDataClass? = null

        if(!this.shouldReloadData) {
            this.shouldReloadData = true
            return myFlatenedSectionArray.size
        }

        myFlatenedSectionArray.clear()

        for (event in myEventUserDetails.userEvents){
            val displayDate = dataManager.getEventDateAsString(event)//TODO make this into the MyEventDataManager

            //below code exists to group events under common dates.

            if (currentEventDate != null){
                if (currentEventDate == displayDate){//already have a date //Do not create header and just create detail
                    highlight = !highlight //only works for boolean and means switch value. from true to false and versa
                    val eventName =
                        MyEventDetailsDataClass(
                            "EventName",
                            event,
                            "${event.EventName}",
                            "$displayDate",
                            event.EventId,
                            highlight,
                            shadowed
                        )
                    index += 1
                    previousEventTracked = eventName
                    myFlatenedSectionArray.add(eventName)
                } else { //create both header and details
                    //todo update shadowed event name replace in simplified section array
                    if (previousEventTracked != null) {
                        val updatedShadowCell =
                            MyEventDetailsDataClass(
                                "EventName",
                                event,
                                "${previousEventTracked.eventName}",
                                "${previousEventTracked.eventDate}",
                                "${previousEventTracked.eventId}",
                                previousEventTracked.highlighted,
                                true
                            )
                        myFlatenedSectionArray.remove(previousEventTracked)
                        myFlatenedSectionArray.add(updatedShadowCell)
                    }

                    currentEventDate = displayDate
                    highlight = false

                    //This is the header
                    val eventDate =
                        MyEventDetailsDataClass(
                            "EventDate",
                            event,
                            "${event.EventName}",
                            "$displayDate",
                            event.EventId,
                            false,
                            false
                        )
                    //This is the detail and we handle if it is even or odd, highlighted, or the end, shadowed and curved.

                    val eventName =
                        MyEventDetailsDataClass(
                            "EventName",
                            event,
                            "${event.EventName}",
                            "$displayDate",
                            event.EventId,
                            highlight,
                            false
                        )
                    index += 1
                    previousEventTracked = eventName
                    myFlatenedSectionArray.add(eventDate)
                    myFlatenedSectionArray.add(eventName)
                }
            } else { //create both header and details
                currentEventDate = displayDate
                highlight = false

                //This is the header
                val eventDate =
                    MyEventDetailsDataClass(
                        "EventDate",
                        event,
                        "${event.EventName}",
                        "$displayDate",
                        event.EventId,
                        false,
                        false
                    )
                val eventName =
                    MyEventDetailsDataClass(
                        "EventName",
                        event,
                        "${event.EventName}",
                        "$displayDate",
                        event.EventId,
                        highlight,
                        false
                    )
                index += 1
                previousEventTracked = eventName
                myFlatenedSectionArray.add(eventDate)
                myFlatenedSectionArray.add(eventName)
            }
        }
        if(previousEventTracked != null) {
            val updatedShadowCell =
                MyEventDetailsDataClass(
                    "EventName",
                    previousEventTracked.eventDetail,
                    "${previousEventTracked.eventName}",
                    "${previousEventTracked.eventDate}",
                    "${previousEventTracked.eventId}",
                    previousEventTracked.highlighted,
                    true
                )
            myFlatenedSectionArray.remove(previousEventTracked)
            myFlatenedSectionArray.add(updatedShadowCell)
        }
        Log.d(TAG, "the size of the myEvents adapter is ${myFlatenedSectionArray.size}")
        return myFlatenedSectionArray.size
    }

    override fun getItemViewType(position: Int): Int {
        val simplifiedDataBindViewHolderNeeds = myFlatenedSectionArray[position]
        when (simplifiedDataBindViewHolderNeeds.type){
            "EventName"->return DataType.NAME.typeKey
            "EventDate"->return com.eligustilo.NudgeMe.ui.myevents.DataType.DATE.typeKey //what it used to look like. Why the full path? Learn more about this.
        }
        //never used default???
        return 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if(myEventUserDetails.userEvents[0].EventId == ""){//goal is to handle when there are no events. aka new user
            //TODO what is the default event id when there are no events? blank I assume?
            //handle new user new event onboarding
        }else{
            when (viewType) {
                com.eligustilo.NudgeMe.ui.events.DataType.NAME.typeKey -> {
                    val eventCellView = LayoutInflater.from(parent.context).inflate(R.layout.my_events_detail_cell, parent, false)
                    return com.eligustilo.NudgeMe.ui.myevents.MyViewHolderEventDetails(eventCellView)
                }
                com.eligustilo.NudgeMe.ui.events.DataType.DATE.typeKey -> {
                    val eventCellView = LayoutInflater.from(parent.context).inflate(R.layout.my_events_header_cell, parent, false)
                    return com.eligustilo.NudgeMe.ui.myevents.MyViewHolderEventHeader(eventCellView, myEventFragment)
                }
            }
        }
        //required default
        val eventHeaderCellView = LayoutInflater.from(parent.context).inflate(R.layout.event_recylcer_view_details_cell, parent, false)
        return MyViewHolderEventHeader(eventHeaderCellView, myEventFragment)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val simplifiedDataBindViewHolderNeeds = myFlatenedSectionArray[position]

        if (simplifiedDataBindViewHolderNeeds.type == "EventDate"){
            //Checks if event type is a header
            val viewHolder = holder as MyViewHolderEventHeader

            val eventDate = simplifiedDataBindViewHolderNeeds.eventDate
            val eventDateDayAvatar = simplifiedDataBindViewHolderNeeds.eventDetail?.let {
                DataManager.getEventDateAsDayOfWeek(it)//TODO what is it??
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
        } else {
            //else it must be a type detail
            val viewHolder = holder as com.eligustilo.NudgeMe.ui.myevents.MyViewHolderEventDetails
            val eventName = simplifiedDataBindViewHolderNeeds.eventName
            viewHolder.eventsRemindersTimes.clear()
            viewHolder.eventsRemindersId.clear()
            if (simplifiedDataBindViewHolderNeeds.eventDetail != null){
                viewHolder.eventDateOriginalForamt = simplifiedDataBindViewHolderNeeds.eventDetail.EventDate
                viewHolder.eventDetail = simplifiedDataBindViewHolderNeeds.eventDetail
                viewHolder.ownerName = simplifiedDataBindViewHolderNeeds.eventDetail.friendName
                viewHolder.eventDateOriginalForamt = simplifiedDataBindViewHolderNeeds.eventDetail.EventDate
                val remindersArray =  simplifiedDataBindViewHolderNeeds.eventDetail.ReminderArray
                for (reminder in remindersArray){
                    viewHolder.eventsRemindersTimes.add(reminder.reminder)
                    viewHolder.eventsRemindersId.add(reminder.reminderID)
                }
            }

            if (eventName != null){
                if (simplifiedDataBindViewHolderNeeds.highlighted == true){
                    //checks to see if it is odd
                    if(simplifiedDataBindViewHolderNeeds.eventDetail != null && simplifiedDataBindViewHolderNeeds.eventDetail.ReminderArray.size > 0) {
                        Log.d(TAG, "The reminder array size is ${simplifiedDataBindViewHolderNeeds.eventDetail.ReminderArray.size}")
                        val bell = String(Character.toChars(0x1F514))
                        viewHolder.eventNameCell.text = "$bell ${eventName}"
                    } else {
                        viewHolder.eventNameCell.text = "${eventName}"
                    }

                    viewHolder.eventNameForReminderActivity = eventName
                    viewHolder.eventDateForReminderActivity = simplifiedDataBindViewHolderNeeds.eventDate.toString()
                    viewHolder.eventId = simplifiedDataBindViewHolderNeeds.eventId
                    viewHolder.datesDetailsCell.setBackgroundColor(context.resources.getColor(R.color.recyclerGrey))
                } else {
                    //it is even and not highlighted
                    if(simplifiedDataBindViewHolderNeeds.eventDetail != null && simplifiedDataBindViewHolderNeeds.eventDetail.ReminderArray.size > 0) {
                        val bell = String(Character.toChars(0x1F514))
                        viewHolder.eventNameCell.text = "$bell ${eventName}"
                    } else {
                        viewHolder.eventNameCell.text = "${eventName}"
                    }
                    viewHolder.eventNameForReminderActivity = eventName
                    viewHolder.eventDateForReminderActivity = simplifiedDataBindViewHolderNeeds.eventDate.toString()
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

    fun updateData(newMyEventsUserDetails: MyEventUserDetails) {
        myEventUserDetails = newMyEventsUserDetails
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