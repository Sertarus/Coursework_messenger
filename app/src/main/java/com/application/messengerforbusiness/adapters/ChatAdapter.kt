package com.application.messengerforbusiness.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.application.messengerforbusiness.R
import com.application.messengerforbusiness.models.ModelChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.util.*

class ChatAdapter(
    val context: Context, val chatList: MutableList<ModelChat>,
    val imageURL: String, val myImageURL: String
) : RecyclerView.Adapter<ChatAdapter.MyHolder>() {

    lateinit var fUser: FirebaseUser

    class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var profileIV: ImageView = itemView.findViewById(R.id.profileIV)
        var messageTV: TextView = itemView.findViewById(R.id.messageTV)
        var timeTV: TextView = itemView.findViewById(R.id.timeTV)
        var isSeenTV: TextView = itemView.findViewById(R.id.isSeenTV)
        var messageLayout: LinearLayout = itemView.findViewById(R.id.messageLayout)
        var messageIV: ImageView = itemView.findViewById(R.id.messageIV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        if (viewType == MESSAGE_TYPE_RIGHT) {
            val view = LayoutInflater.from(context).inflate(
                R.layout.row_chat_rigth,
                parent, false
            )
            return MyHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(
                R.layout.row_chat_left,
                parent, false
            )
            return MyHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun getItemViewType(position: Int): Int {
        fUser = FirebaseAuth.getInstance().currentUser!!
        return if (chatList[position].sender == (fUser.uid)) {
            MESSAGE_TYPE_LEFT
        } else {
            MESSAGE_TYPE_RIGHT
        }
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val message = chatList[position].message
        val timeStamp = chatList[position].timeStamp
        val type = chatList[position].type
        val cal = Calendar.getInstance(Locale.ENGLISH)
        if (timeStamp.toLongOrNull() != null) {
            cal.timeInMillis = timeStamp.toLongOrNull()!!
        }
        val dateTime = android.text.format.DateFormat
            .format("dd/MM/yyyy hh:mm aa", cal).toString()

        if (type == "text") {
            holder.messageTV.visibility = View.VISIBLE
            holder.messageIV.visibility = View.GONE
        }
        else {
            holder.messageTV.visibility = View.GONE
            holder.messageIV.visibility = View.VISIBLE
            Picasso.get().load(message).placeholder(R.drawable.ic_image).into(holder.messageIV)
        }

        holder.messageTV.text = message
        holder.timeTV.text = dateTime
        try {
            if (MESSAGE_TYPE_LEFT == getItemViewType(position)) {
                Picasso.get().load(myImageURL).into(holder.profileIV)
            } else {
                Picasso.get().load(imageURL).into(holder.profileIV)
            }
        } catch (e: Exception) {

        }
        holder.messageLayout.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Delete")
            builder.setMessage("Are you sure to delete this message?")
            builder.setPositiveButton("Delete", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    deleteMessage(position)
                }
            })
            builder.setNegativeButton("No", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog!!.dismiss()
                }
            })
            builder.create().show()
        }
        if (chatList[position].seen) {
            holder.isSeenTV.text = "Seen"
        } else {
            holder.isSeenTV.text = "Delivered"
        }
    }

    private fun deleteMessage(position: Int) {
        val myUid = FirebaseAuth.getInstance().currentUser!!.uid

        val msgTimeStamp = chatList[position].timeStamp
        val dbRef = FirebaseDatabase.getInstance().getReference("Chats")
        val query = dbRef.orderByChild("timeStamp").equalTo(msgTimeStamp)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (ds in p0.children) {
                    if (ds.child("sender").value == myUid) {
                        ds.ref.removeValue()
                        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "You can delete only your messages",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }


    companion object {
        const val MESSAGE_TYPE_LEFT = 0
        const val MESSAGE_TYPE_RIGHT = 1
    }
}