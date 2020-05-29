package com.application.messengerforbusiness

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.application.messengerforbusiness.adapters.ChatlistAdapter
import com.application.messengerforbusiness.models.ModelChat
import com.application.messengerforbusiness.models.ModelChatlist
import com.application.messengerforbusiness.models.ModelUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

/**
 * A simple [Fragment] subclass.
 */
class ChatListFragment : Fragment() {

    lateinit var firebaseAuth: FirebaseAuth
    lateinit var recyclerView: RecyclerView
    private lateinit var chatlistList: MutableList<ModelChatlist>
    lateinit var userList: MutableList<ModelUser>
    lateinit var reference: DatabaseReference
    lateinit var currentUser: FirebaseUser
    lateinit var adapterChatList: ChatlistAdapter
    lateinit var contextForAdapter: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
        firebaseAuth = FirebaseAuth.getInstance()
        currentUser = FirebaseAuth.getInstance().currentUser!!
        recyclerView = view.findViewById(R.id.recyclerView)
        chatlistList = mutableListOf()
        reference = FirebaseDatabase.getInstance().getReference("Chatlist").child(currentUser.uid)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                chatlistList.clear()
                for (ds in p0.children) {
                    val chatlist = ds.getValue(ModelChatlist::class.java)
                    chatlistList.add(chatlist!!)
                }
                loadChats()
            }
        })
        return view
    }

    private fun loadChats() {
        userList = mutableListOf()
        reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                userList.clear()
                for (ds in p0.children) {
                    val user = ds.getValue(ModelUser::class.java)
                    for (chatList in chatlistList) {
                        if (user?.uid == chatList.id) {
                            userList.add(user)
                            break
                        }
                    }
                    adapterChatList = ChatlistAdapter(contextForAdapter, userList)
                    recyclerView.adapter = adapterChatList
                    for (i in 0 until userList.size) {
                        lastMessage(userList[i].uid)
                    }
                }
            }

        })
    }

    private fun lastMessage(uid: String) {
        val reference = FirebaseDatabase.getInstance().getReference("Chats")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                var theLastMessage = "default"
                for (ds in p0.children) {
                    val chat = ds.getValue(ModelChat::class.java) ?: continue
                    if ((chat.receiver == currentUser.uid && chat.sender == uid) ||
                        (chat.receiver == uid && chat.sender == currentUser.uid)
                    ) {
                        if (chat.type == "image") {
                            theLastMessage = "Sent a photo"
                        }
                        else {
                            theLastMessage = chat.message
                        }
                    }
                }
                adapterChatList.setLastMessageMap(uid, theLastMessage)
                adapterChatList.notifyDataSetChanged()
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contextForAdapter = context
    }
}
