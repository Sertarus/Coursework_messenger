package com.application.messengerforbusiness

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import java.lang.Exception

class AnotherProfileActivity : AppCompatActivity() {

    protected lateinit var databaseReference: DatabaseReference
    protected lateinit var avatarIV: ImageView
    protected lateinit var nameTV: TextView
    protected lateinit var emailTV: TextView
    protected lateinit var phoneTV: TextView
    protected lateinit var positionTV: TextView
    protected lateinit var coverIV: ImageView
    protected lateinit var fab: FloatingActionButton
    protected lateinit var admFab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_profile)
        title = "Profile"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        avatarIV = findViewById(R.id.avatarIV)
        coverIV = findViewById(R.id.coverIV)
        nameTV = findViewById(R.id.fullNameTv)
        emailTV = findViewById(R.id.emailTv)
        phoneTV = findViewById(R.id.phoneTv)
        positionTV = findViewById(R.id.positionTV)
        fab = findViewById(R.id.floatingButton)
        fab.hide()
        admFab = findViewById(R.id.floatingAdminButton)
        admFab.hide()
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")
        val query = databaseReference.orderByChild("uid").equalTo(intent.getStringExtra("hisUid"))
        query.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                p0.children.forEach {
                    val name = it.child("name").value
                    val surname = it.child("surname").value
                    val email = it.child("email").value
                    val phone = it.child("phone").value
                    val position = it.child("position").value
                    val image = it.child("image").value
                    val cover = it.child("cover").value
                    val fullName = name.toString() + " " + surname.toString()
                    nameTV.text = fullName
                    emailTV.text = email as CharSequence?
                    phoneTV.text = phone as CharSequence?
                    positionTV.text = position as CharSequence?
                    try {
                        Picasso.get().load(image as String?).into(avatarIV)
                    } catch (e: Exception) {
                        Picasso.get().load(R.drawable.ic_default_image).into(avatarIV)
                    }
                    try {
                        Picasso.get().load(cover as String?).into(coverIV)
                    } catch (e: Exception) {

                    }
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.action_search)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut()
        }
        return super.onOptionsItemSelected(item)
    }
}
