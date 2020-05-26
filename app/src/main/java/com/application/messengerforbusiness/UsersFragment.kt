package com.application.messengerforbusiness

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class UsersFragment : Fragment() {

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var adapterUsers: UsersAdapter
    protected lateinit var usersList: MutableList<ModelUser>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_users, container, false)
        recyclerView = view.findViewById(R.id.usersRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(activity)

        usersList = mutableListOf()
        getAllUsers()

        return view
    }

    private fun getAllUsers() {
        val fUser = FirebaseAuth.getInstance().currentUser
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.addValueEventListener(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                usersList.clear()
                for (data in p0.children) {
                    val modelUser = data.getValue(ModelUser::class.java)
                    if (!modelUser?.uid.equals(fUser?.uid)) {
                        if (modelUser != null) {
                            usersList.add(modelUser)
                        }
                    }
                    if (activity?.applicationContext != null) {
                        adapterUsers = UsersAdapter(activity!!.applicationContext!!, usersList)
                        recyclerView.adapter = adapterUsers
                    }
                }
            }

        })
    }

}
