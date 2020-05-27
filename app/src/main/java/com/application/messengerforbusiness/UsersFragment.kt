package com.application.messengerforbusiness

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.messengerforbusiness.adapters.UsersAdapter
import com.application.messengerforbusiness.models.ModelUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class UsersFragment : Fragment() {

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var adapterUsers: UsersAdapter
    protected lateinit var usersList: MutableList<ModelUser>
    protected lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_users, container, false)
        recyclerView = view.findViewById(R.id.usersRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        firebaseAuth = FirebaseAuth.getInstance()

        usersList = mutableListOf()
        getAllUsers()

        return view
    }

    private fun getAllUsers() {
        val fUser = FirebaseAuth.getInstance().currentUser
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.addValueEventListener(object : ValueEventListener {
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
                        adapterUsers =
                            UsersAdapter(
                                activity!!,
                                usersList
                            )
                        recyclerView.adapter = adapterUsers
                    }
                }
            }

        })
    }

    private fun searchUsers(newText: String) {
        val fUser = FirebaseAuth.getInstance().currentUser
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                usersList.clear()
                for (data in p0.children) {
                    val modelUser = data.getValue(ModelUser::class.java)
                    if (!modelUser?.uid.equals(fUser?.uid)) {
                        if (modelUser != null) {
                            if (modelUser.name.toLowerCase(Locale.ROOT).
                                contains(newText.toLowerCase(Locale.ROOT)) ||
                                modelUser.email.toLowerCase(Locale.ROOT).
                                contains(newText.toLowerCase(Locale.ROOT))) {
                                usersList.add(modelUser)
                            }
                        }
                    }
                    if (activity?.applicationContext != null) {
                        adapterUsers =
                            UsersAdapter(
                                activity!!,
                                usersList
                            )
                        adapterUsers.notifyDataSetChanged()
                        recyclerView.adapter = adapterUsers
                    }
                }
            }

        })
    }


    private fun checkUserStatus() {
        val user = firebaseAuth.currentUser
        if (user != null) {

        } else {
            startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        val item = menu.findItem(R.id.action_search)
        val searchView = item.actionView as androidx.appcompat.widget.SearchView
        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!TextUtils.isEmpty(query!!.trim())) {
                    searchUsers(query)
                } else {
                    getAllUsers()
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!TextUtils.isEmpty(newText!!.trim())) {
                    searchUsers(newText)
                } else {
                    getAllUsers()
                }
                return false
            }

        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_logout) {
            firebaseAuth.signOut()
            checkUserStatus()
        }
        return super.onOptionsItemSelected(item)
    }

}
