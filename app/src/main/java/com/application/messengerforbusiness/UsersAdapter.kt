package com.application.messengerforbusiness

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.lang.Exception

class UsersAdapter(var context: Context, usersList: MutableList<ModelUser>):
    RecyclerView.Adapter<UsersAdapter.MyHolder>() {

    var userList = usersList

    class MyHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var mAvatarIV: ImageView
        var mNameTV: TextView
        var mEmailTV: TextView

        init {
            mAvatarIV = itemView.findViewById(R.id.imageView)
            mNameTV = itemView.findViewById(R.id.nameTV)
            mEmailTV = itemView.findViewById(R.id.emailTV)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_users, parent, false)
        return  MyHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val userImage = userList[position].image
        val userName = userList[position].name + " " + userList[position].surname
        val usersEmail = userList[position].email

        holder.mNameTV.text = userName
        holder.mEmailTV.text = usersEmail
        try {
            Picasso.get().load(userImage).
            placeholder(R.drawable.ic_default_image).into(holder.mAvatarIV)
        }
        catch (e: Exception) {

        }

        holder.itemView.setOnClickListener{
            Toast.makeText(context, usersEmail, Toast.LENGTH_SHORT).show()
        }

    }
}