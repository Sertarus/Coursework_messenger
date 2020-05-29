package com.application.messengerforbusiness.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.application.messengerforbusiness.ChatActivity
import com.application.messengerforbusiness.R
import com.application.messengerforbusiness.models.ModelUser
import com.squareup.picasso.Picasso
import java.lang.Exception

class ChatlistAdapter(var context: Context, var userList: MutableList<ModelUser>
): RecyclerView.Adapter<ChatlistAdapter.MyHolder>() {

    private var lastMessageMap: MutableMap<String, String> = mutableMapOf()

    class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var profileIV: ImageView = itemView.findViewById(R.id.profileIV)
        var onlineStatusIV: ImageView = itemView.findViewById(R.id.onlineStatusIV)
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var lastMessageTV: TextView = itemView.findViewById(R.id.lastMessageTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_chatlist, parent, false)
        return  MyHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val hisUid = userList[position].uid
        val userImage = userList[position].image
        val userName = userList[position].name + " " + userList[position].surname
        val lastMessage = lastMessageMap[hisUid]

        holder.nameTV.text = userName
        if (lastMessage == null || lastMessage == "default") {
            holder.lastMessageTV.visibility = View.GONE
        }
        else {
            holder.lastMessageTV.visibility = View.VISIBLE
            holder.lastMessageTV.text = lastMessage
        }
        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_default_image).into(holder.profileIV)
        }
        catch (e : Exception) {

        }
        if (userList[position].onlineStatus == "online") {
            holder.onlineStatusIV.setImageResource(R.drawable.circle_online)
        }
        else {
            holder.onlineStatusIV.setImageResource(R.drawable.circle_offline)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity :: class.java)
            intent.putExtra("hisUid", hisUid)
            context.startActivity(intent)
        }
    }

    public fun setLastMessageMap(user: String, lastMessage: String) {
        lastMessageMap.put(user, lastMessage)
    }
}