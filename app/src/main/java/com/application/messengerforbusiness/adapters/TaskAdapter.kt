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
import com.application.messengerforbusiness.models.ModelTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TaskAdapter(var context: Context, val tasksList: MutableList<ModelTask>) : RecyclerView.Adapter<TaskAdapter.MyHolder>() {

    class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var creatorTV: TextView = itemView.findViewById(R.id.creatorTV)
        var nameTV: TextView = itemView.findViewById(R.id.nameTV)
        var descriptionTV: TextView = itemView.findViewById(R.id.descriptionTV)
        var deadlineTV: TextView = itemView.findViewById(R.id.deadlineTV)
        var receiverTV: TextView = itemView.findViewById(R.id.receiverTV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_task, parent, false)
        return MyHolder(view)
    }

    override fun getItemCount(): Int {
        return tasksList.size
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val creator = tasksList[position].creator
        val taskName = tasksList[position].taskName
        val description = tasksList[position].description
        val deadline = tasksList[position].deadline
        val receiver = tasksList[position].receiver

        holder.creatorTV.text = "Guarantor: $creator"
        holder.nameTV.text = "Task name: $taskName"
        holder.descriptionTV.text = "Description:\n$description"
        holder.deadlineTV.text = "Deadline: $deadline"
        holder.receiverTV.text = "Performer: $receiver"

        holder.itemView.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Delete")
            builder.setMessage("Are you sure to delete this task?")
            builder.setPositiveButton("Delete"
            ) { _, _ -> deleteTask(position) }
            builder.setNegativeButton("No"
            ) { dialog, _ -> dialog!!.dismiss() }
            builder.create().show()
        }
    }

    private fun deleteTask(position: Int) {
        val myEmail = FirebaseAuth.getInstance().currentUser!!.email
        val taskTimeStamp = tasksList[position].timeStamp
        val dbRef = FirebaseDatabase.getInstance().getReference("Tasks")
        val query = dbRef.orderByChild("timeStamp").equalTo(taskTimeStamp)
        query.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (ds in p0.children) {
                    if (ds.child("creator").value.toString() == myEmail.toString()) {
                        ds.ref.removeValue()
                        Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "You can delete only your tasks",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}