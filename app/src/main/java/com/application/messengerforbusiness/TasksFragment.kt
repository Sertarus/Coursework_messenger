package com.application.messengerforbusiness

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.messengerforbusiness.adapters.TaskAdapter
import com.application.messengerforbusiness.adapters.UsersAdapter
import com.application.messengerforbusiness.models.ModelTask
import com.application.messengerforbusiness.models.ModelUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * A simple [Fragment] subclass.
 */
class TasksFragment : Fragment() {

    protected lateinit var firebaseAuth: FirebaseAuth
    protected lateinit var tasksList: MutableList<ModelTask>
    protected lateinit var myEmail: String
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var adapterTasks: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firebaseAuth = FirebaseAuth.getInstance()
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        myEmail = firebaseAuth.currentUser?.email.toString()
        recyclerView = view.findViewById(R.id.tasksRecyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        tasksList = mutableListOf()
        getTasks()
        return view
    }

    private fun getTasks() {
        val fUser = FirebaseAuth.getInstance().currentUser
        val ref = FirebaseDatabase.getInstance().getReference("Tasks")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                tasksList.clear()
                for (ds in p0.children) {
                    val modelTask = ds.getValue(ModelTask :: class.java)
                    if (modelTask!!.creator ==  myEmail || modelTask.receiver == myEmail) {
                        tasksList.add(modelTask)
                    }
                    if (activity?.applicationContext != null) {
                        adapterTasks =
                            TaskAdapter(
                                activity!!,
                                tasksList
                            )
                        recyclerView.adapter = adapterTasks
                    }
                }

            }

        })
    }

    private fun checkUserStatus() {
        val user = firebaseAuth.currentUser
        if (user != null) {

        }
        else {
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
        if (item != null) {
            item.isVisible = false
        }
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
