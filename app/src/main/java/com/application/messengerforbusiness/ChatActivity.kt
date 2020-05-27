package com.application.messengerforbusiness


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.messengerforbusiness.adapters.ChatAdapter
import com.application.messengerforbusiness.models.ModelChat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.util.*

class ChatActivity : AppCompatActivity() {

    lateinit var toolbar: Toolbar
    lateinit var recyclerView: RecyclerView
    lateinit var profileTV: ImageView
    lateinit var nameTV: TextView
    lateinit var statusTV: TextView
    lateinit var messageET: EditText
    lateinit var sendButton: ImageButton
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var usersDbRef: DatabaseReference
    lateinit var hisUid: String
    lateinit var myUid: String
    lateinit var hisImage: String
    lateinit var myImage: String
    lateinit var seenListener: ValueEventListener
    lateinit var userRefForSeen: DatabaseReference
    lateinit var chatList: MutableList<ModelChat>
    lateinit var adapterChat: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitle("")
        recyclerView = findViewById(R.id.chat_recyclerView)
        profileTV = findViewById(R.id.profileIV)
        nameTV = findViewById(R.id.nameTV)
        statusTV = findViewById(R.id.statusTV)
        messageET = findViewById(R.id.messageEt)
        sendButton = findViewById(R.id.sendButton)

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = linearLayoutManager

        val intent = intent
        hisUid = intent.getStringExtra("hisUid")
        myUid = FirebaseAuth.getInstance().currentUser!!.uid

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        usersDbRef = firebaseDatabase.getReference("Users")

        val userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid)
        userQuery.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (ds in p0.children) {
                    val fullName = ds.child("name").value.toString() + " " +
                            ds.child("surname").value.toString()
                    hisImage = ds.child("image").value.toString()
                    nameTV.text = fullName
                    val onlineStatus = ds.child("onlineStatus").value.toString()
                    val typingStatus = ds.child("typingTo").value.toString()
                    if (typingStatus == myUid) {
                        statusTV.text = "typing..."
                    }
                    else if (onlineStatus == "online") {
                        statusTV.text = onlineStatus
                    }
                    else {
                        val cal = Calendar.getInstance(Locale.ENGLISH)
                        if (onlineStatus.toLongOrNull() != null) {
                            cal.timeInMillis = onlineStatus.toLongOrNull()!!
                        }
                        val dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()
                        statusTV.text = "Last seen at: " + dateTime
                    }
                    try {
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_image).into(profileTV)
                    }
                    catch (e: Exception) {
                        Picasso.get().load(R.drawable.ic_default_image).into(profileTV)
                    }
                }
            }
        })

        val thisUserQuery = usersDbRef.orderByChild("uid").equalTo(myUid)
        thisUserQuery.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (ds in p0.children) {
                    myImage = ds.child("image").value.toString()
                }
            }
        })

        sendButton.setOnClickListener {
            val message = messageET.text.toString().trim()
            if (TextUtils.isEmpty(message)) {
                Toast.makeText(this, "Cannot send empty message...",
                    Toast.LENGTH_SHORT).show()
            }
            else {
                sendMessage(message)
            }
        }

        messageET.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().trim().length == 0) {
                    checkTypingStatus("noOne")
                }
                else {
                    checkTypingStatus(hisUid)
                }
            }

        })

        readMessages()
        seenMessages()
    }

    private fun seenMessages() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats")
        seenListener = userRefForSeen.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (ds in p0.children) {
                    val chat = ds.getValue(ModelChat :: class.java)
                    if (chat!!.receiver == myUid && chat.sender == hisUid) {
                        val data = mutableMapOf<String, Any>()
                        data["seen"] = true
                        ds.ref.updateChildren(data)
                    }
                }
            }

        })
    }

    private fun readMessages() {
        chatList = mutableListOf()
        val dbRef = FirebaseDatabase.getInstance().getReference("Chats")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                chatList.clear()
                for (ds in p0.children) {
                    val chat = ds.getValue(ModelChat::class.java)
                    if ((chat!!.receiver == myUid && chat.sender == hisUid) ||
                        (chat.receiver == hisUid && chat.sender == myUid)) {
                        chatList.add(chat)
                    }

                    adapterChat = ChatAdapter(this@ChatActivity, chatList, hisImage, myImage)
                    adapterChat.notifyDataSetChanged()
                    recyclerView.adapter = adapterChat
                }
            }

        })
    }

    private fun sendMessage(message : String) {
        val databaseReference = FirebaseDatabase.getInstance().reference

        val timeStamp = System.currentTimeMillis().toString()
        val data = mutableMapOf<String, Any>()
        data["sender"] = myUid
        data["receiver"] = hisUid
        data["message"] = message
        data["timeStamp"] = timeStamp
        data["seen"] = false
        databaseReference.child("Chats").push().setValue(data)
        messageET.setText("")
    }

    private fun checkUserStatus() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            myUid = user.uid
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun checkOnlineStatus(status: String) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("Users/" + myUid)
        val data = mutableMapOf<String, Any>()
        data["onlineStatus"] = status
        dbRef.updateChildren(data)
    }

    private fun checkTypingStatus(typing: String) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("Users/" + myUid)
        val data = mutableMapOf<String, Any>()
        data["typingTo"] = typing
        dbRef.updateChildren(data)
    }

    override fun onStart() {
        checkUserStatus()
        checkOnlineStatus("online")
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
        val timeStamp = System.currentTimeMillis().toString()
        checkOnlineStatus(timeStamp)
        checkTypingStatus("noOne")
        userRefForSeen.removeEventListener(seenListener)
    }

    override fun onResume() {
        checkOnlineStatus("online")
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.action_search)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
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
