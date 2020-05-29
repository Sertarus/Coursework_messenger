package com.application.messengerforbusiness

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.application.messengerforbusiness.models.ModelUser
import com.application.messengerforbusiness.notifications.Token
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId

class DashboardActivity : AppCompatActivity() {

    protected lateinit var firebaseAuth: FirebaseAuth
    protected lateinit var actionBar: ActionBar
    protected lateinit var mUid: String
    protected lateinit var deletedListener: ValueEventListener
    protected lateinit var refForDeleted: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        actionBar = this.supportActionBar!!
        actionBar.title = "Profile"

        firebaseAuth = FirebaseAuth.getInstance()

        val navigationView = findViewById<BottomNavigationView>(R.id.nav_view)

        actionBar.title = "Home"
        val fragment1 = TasksFragment()
        val ft1 = supportFragmentManager.beginTransaction()
        ft1.replace(R.id.content, fragment1, "")
        ft1.commit()

        navigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigator_home -> {
                    actionBar.title = "Home"
                    val fragment1 = TasksFragment()
                    val ft1 = supportFragmentManager.beginTransaction()
                    ft1.replace(R.id.content, fragment1, "")
                    ft1.commit()
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigator_profile -> {
                    actionBar.title = "Profile"
                    val fragment2 = ProfileFragment()
                    val ft2 = supportFragmentManager.beginTransaction()
                    ft2.replace(R.id.content, fragment2, "")
                    ft2.commit()
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigator_users -> {
                    actionBar.title = "Users"
                    val fragment3 = UsersFragment()
                    val ft3 = supportFragmentManager.beginTransaction()
                    ft3.replace(R.id.content, fragment3, "")
                    ft3.commit()
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigator_chat -> {
                    actionBar.title = "Chats"
                    val fragment4 = ChatListFragment()
                    val ft4 = supportFragmentManager.beginTransaction()
                    ft4.replace(R.id.content, fragment4, "")
                    ft4.commit()
                    return@setOnNavigationItemSelectedListener true
                }

                else -> return@setOnNavigationItemSelectedListener true
            }
        }
        refForDeleted = FirebaseDatabase.getInstance().getReference("Users")
        deletedListener = refForDeleted.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                val user = firebaseAuth.currentUser
                for (ds in p0.children) {
                    val currentUser = ds.getValue(ModelUser :: class.java)
                    if (currentUser!!.deleted && currentUser.uid == user!!.uid) {
                        val ref = FirebaseDatabase.getInstance().reference
                        ref.child("Users/" + user.uid).removeValue()
                        user.delete().addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(this@DashboardActivity, "This user was deleted", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this@DashboardActivity, it.message, Toast.LENGTH_SHORT).show()
                        }
                        startActivity(Intent(this@DashboardActivity, MainActivity :: class.java))
                    }
                }
            }

        })
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
           updateToken(it.token)
        }
    }

    override fun onResume() {
        checkUserStatus()
        super.onResume()
    }

    fun updateToken(token: String) {
        if (firebaseAuth.currentUser != null) {
            val ref = FirebaseDatabase.getInstance().getReference("Tokens")
            val mToken = Token(token)
            ref.child(mUid).setValue(mToken)
        }
    }

    private fun checkUserStatus() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            mUid = user.uid
            val sp = getSharedPreferences("SP_USER", Context.MODE_PRIVATE)
            val editor = sp.edit()
            editor.putString("Current_USERID", mUid)
            editor.apply()
        }
        else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        refForDeleted.removeEventListener(deletedListener)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onStart() {
        checkUserStatus()
        super.onStart()
    }
}
