package com.application.messengerforbusiness


import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.Image
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.application.messengerforbusiness.adapters.ChatAdapter
import com.application.messengerforbusiness.models.ModelChat
import com.application.messengerforbusiness.models.ModelUser
import com.application.messengerforbusiness.notifications.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.ByteArrayOutputStream
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
    lateinit var requestQueue: RequestQueue
    lateinit var attachButton: ImageButton
    val CAMERA_REQUEST_CODE = 100
    val STORAGE_REQUEST_CODE = 200
    val IMAGE_PICK_GALLERY_CODE = 300
    val IMAGE_PICK_CAMERA_CODE = 400
    protected lateinit var storagePermissions: Array<String>
    protected lateinit var cameraPermissions: Array<String>
    var image_uri: Uri? = null
    var notify = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitle("")
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true);
        recyclerView = findViewById(R.id.chat_recyclerView)
        profileTV = findViewById(R.id.profileIV)
        nameTV = findViewById(R.id.nameTV)
        statusTV = findViewById(R.id.statusTV)
        messageET = findViewById(R.id.messageEt)
        sendButton = findViewById(R.id.sendButton)
        attachButton = findViewById(R.id.attachBtn)

        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        requestQueue = Volley.newRequestQueue(applicationContext)

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
                    } else if (onlineStatus == "online") {
                        statusTV.text = onlineStatus
                    } else {
                        val cal = Calendar.getInstance(Locale.ENGLISH)
                        if (onlineStatus.toLongOrNull() != null) {
                            cal.timeInMillis = onlineStatus.toLongOrNull()!!
                        }
                        val dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()
                        statusTV.text = "Last seen at: " + dateTime
                    }
                    try {
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_image)
                            .into(profileTV)
                    } catch (e: Exception) {

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
            notify = true
            val message = messageET.text.toString().trim()
            if (TextUtils.isEmpty(message)) {
                Toast.makeText(
                    this, "Cannot send empty message...",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sendMessage(message)
            }
            messageET.setText("")
        }

        attachButton.setOnClickListener {
            showImagePicDialog()
        }

        messageET.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().trim().length == 0) {
                    checkTypingStatus("noOne")
                } else {
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
                    val chat = ds.getValue(ModelChat::class.java)
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
                        (chat.receiver == hisUid && chat.sender == myUid)
                    ) {
                        chatList.add(chat)
                    }

                    adapterChat = ChatAdapter(this@ChatActivity, chatList, hisImage, myImage)
                    adapterChat.notifyDataSetChanged()
                    recyclerView.adapter = adapterChat
                }
            }

        })
    }

    private fun sendMessage(message: String) {
        val databaseReference = FirebaseDatabase.getInstance().reference

        val timeStamp = System.currentTimeMillis().toString()
        val data = mutableMapOf<String, Any>()
        data["sender"] = myUid
        data["receiver"] = hisUid
        data["message"] = message
        data["timeStamp"] = timeStamp
        data["seen"] = false
        data["type"] = "text"

        databaseReference.child("Chats").push().setValue(data)

        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid)
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(ModelUser::class.java)
                if (notify) {
                    sendNotification(hisUid, user!!.name, message)
                }
                notify = false
            }

        })

        val chatRef1 =
            FirebaseDatabase.getInstance().getReference("Chatlist").child(myUid).child(hisUid)
        chatRef1.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (!p0.exists()) {
                    chatRef1.child("id").setValue(hisUid)
                }
            }

        })
        val chatRef2 =
            FirebaseDatabase.getInstance().getReference("Chatlist").child(hisUid).child(myUid)
        chatRef2.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if (!p0.exists()) {
                    chatRef2.child("id").setValue(myUid)
                }
            }

        })
    }

    private fun sendImageMessage(imageUri: Uri?) {
        notify = true
        val progressBar = ProgressBar(this)
        progressBar.visibility = ProgressBar.VISIBLE
        val timestamp = System.currentTimeMillis().toString()
        val fileNameAndPath = "ChatImages/post_$timestamp"
        val bitmap = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val source = ImageDecoder.createSource(this.contentResolver, imageUri!!)
                ImageDecoder.decodeBitmap(source)
            }
            else -> {
                MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            }
        }
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()
        val ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath)
        ref.putBytes(data).addOnSuccessListener {
            progressBar.visibility = ProgressBar.INVISIBLE
            val uriTask = it.storage.downloadUrl
            while (!uriTask.isSuccessful) {

            }
            val downloadUri = uriTask.result.toString()
            if (uriTask.isSuccessful) {
                val dbRef = FirebaseDatabase.getInstance().reference
                val dataMap = mutableMapOf<String, Any>()
                dataMap["sender"] = myUid
                dataMap["receiver"] = hisUid
                dataMap["message"] = downloadUri
                dataMap["timeStamp"] = timestamp
                dataMap["type"] = "image"
                dataMap["seen"] = false
                dbRef.child("Chats").push().setValue(dataMap)

                val database = FirebaseDatabase.getInstance().getReference("Users").child(myUid)
                database.addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        val user = p0.getValue(ModelUser :: class.java)
                        if (notify) {
                            sendNotification(hisUid, user!!.name, "Sent you a photo")
                        }
                        notify = false
                    }
                })
                val chatRef1 =
                    FirebaseDatabase.getInstance().getReference("Chatlist").child(myUid).child(hisUid)
                chatRef1.addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (!p0.exists()) {
                            chatRef1.child("id").setValue(hisUid)
                        }
                    }

                })
                val chatRef2 =
                    FirebaseDatabase.getInstance().getReference("Chatlist").child(hisUid).child(myUid)
                chatRef2.addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (!p0.exists()) {
                            chatRef2.child("id").setValue(myUid)
                        }
                    }

                })
            }
        }
    }

    private fun sendNotification(hisUid: String, name: String, message: String) {
        val allTokens = FirebaseDatabase.getInstance().getReference("Tokens")
        val query = allTokens.orderByKey().equalTo(hisUid)
        query.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                for (ds in p0.children) {
                    val token = ds.getValue(Token::class.java)
                    val data = Data(
                        myUid,
                        "$name:$message",
                        "New message",
                        hisUid,
                        R.drawable.ic_default_image
                    )
                    val sender = Sender(data, token!!.token)
                    try {
                        val senderJsonObj = JSONObject(Gson().toJson(sender))
                        val jsonObjectRequest = object : JsonObjectRequest(
                            "https://fcm.googleapis.com/fcm/send",
                            senderJsonObj,
                            Response.Listener {
                                Log.d("JSON_RESPONSE", "onResponse: $it")
                            },
                            Response.ErrorListener {
                                Log.d("JSON_RESPONSE", "onResponse: $it")
                            }) {
                            override fun getHeaders(): MutableMap<String, String> {
                                val headers =  mutableMapOf<String, String>()
                                headers["Content-Type"] = "application/json"
                                headers["Authorization"] = "key="
                                return headers
                            }
                        }
                        requestQueue.add(jsonObjectRequest)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

        })
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
        val dbRef = FirebaseDatabase.getInstance().reference.child("Users/$myUid")
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

    private fun showImagePicDialog() {
        val options = arrayOf("Camera", "Gallery", "Default")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick image from")
        builder.setItems(options) { _, which ->
            if (which == 0) {
                if (!checkCameraPermission()) {
                    requestCameraPermission()
                } else {
                    pickFromCamera()
                }
            } else if (which == 1) {
                if (!checkStoragePermission()) {
                    requestStoragePermission()
                } else {
                    pickFromGallery()
                }
            }
        }
        builder.create().show()
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
           this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            storagePermissions,
            STORAGE_REQUEST_CODE
        )
    }

    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val result2 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return result && result2
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
    }

    private fun pickFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun pickFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp pic")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp description")
        image_uri =
            this.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
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
            val timeStamp = System.currentTimeMillis().toString()
            checkOnlineStatus(timeStamp)
            checkTypingStatus("noOne")
            firebaseAuth.signOut()
            checkUserStatus()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && writeStorageAccepted) {
                        pickFromCamera()
                    } else {
                        Toast.makeText(
                            this,
                            "Please enable camera and storage permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (writeStorageAccepted) {
                        pickFromGallery()
                    } else {
                        Toast.makeText(
                            this,
                            "Please enable storage permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                image_uri = data!!.data!!
                try {
                    sendImageMessage(image_uri)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                try {
                    sendImageMessage(image_uri)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
