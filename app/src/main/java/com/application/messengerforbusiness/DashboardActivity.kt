package com.application.messengerforbusiness

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    protected lateinit var firebaseAuth: FirebaseAuth
    protected lateinit var actionBar: ActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        actionBar = this.supportActionBar!!
        actionBar.title = "Profile"

        firebaseAuth = FirebaseAuth.getInstance()

        val navigationView = findViewById<BottomNavigationView>(R.id.nav_view)

        actionBar.title = "Home"
        val fragment1 = HomeFragment()
        val ft1 = supportFragmentManager.beginTransaction()
        ft1.replace(R.id.content, fragment1, "")
        ft1.commit()

        navigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigator_home -> {
                    actionBar.title = "Home"
                    val fragment1 = HomeFragment()
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
                else -> return@setOnNavigationItemSelectedListener true
            }
        }
    }

    private fun checkUserStatus() {
        val user = firebaseAuth.currentUser
        if (user != null) {

        }
        else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onStart() {
        checkUserStatus()
        super.onStart()
    }
}
