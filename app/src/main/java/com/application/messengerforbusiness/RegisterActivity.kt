package com.application.messengerforbusiness

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    protected lateinit var mNameET: EditText
    protected lateinit var mSurnameET: EditText
    protected lateinit var mPositionET: EditText
    protected lateinit var mEmailET: EditText
    protected lateinit var mPasswordET: EditText
    protected lateinit var mPasswordCheckET: EditText
    protected lateinit var mAddUserButton: Button
    protected lateinit var progressBar: ProgressBar
    protected lateinit var mDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val actionBar = supportActionBar
        actionBar!!.title = "Add user"
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)

        mNameET = findViewById(R.id.nameET)
        mSurnameET = findViewById(R.id.surnameET)
        mPositionET = findViewById(R.id.positionET)
        mEmailET = findViewById(R.id.emailET)
        mPasswordET = findViewById(R.id.passwordET)
        mPasswordCheckET = findViewById(R.id.passwordCheckET)
        mAddUserButton = findViewById(R.id.addUserButton)

        mDatabase = FirebaseDatabase.getInstance()

        progressBar = ProgressBar(this)

        mAddUserButton.setOnClickListener {
            val name = mNameET.text.toString().trim()
            val surname = mSurnameET.text.toString().trim()
            val positon = mPositionET.text.toString().trim()
            val eMail = mEmailET.text.toString().trim()
            val password = mPasswordET.text.toString().trim()
            val passwordCheck = mPasswordCheckET.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(eMail).matches()) {
                mEmailET.setError("Invalid email")
                mEmailET.isFocusable = true
            }
            else if (password != passwordCheck) {
                passwordET.setError("Password mismatch")
                passwordET.isFocusable = true
            }
            else if (password.length < 6) {
                passwordET.setError("Password length should be at least 6 characters")
                passwordET.isFocusable = true
            }
            else {
                registerUser(name, surname, positon, eMail, password)
            }
        }
    }

    private fun registerUser(name: String, surname: String, positon: String, eMail: String, password: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        val mAuth = FirebaseAuth.getInstance()
        mAuth.createUserWithEmailAndPassword(eMail, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    val user = mAuth.currentUser
                    val uid = user!!.uid
                    val data = mutableMapOf<String, Any>()
                    data["email"] = eMail
                    data["uid"] = uid
                    data["name"] = name
                    data["surname"] = surname
                    data["position"] = positon
                    data["phone"] = ""
                    data["image"] = ""
                    data["cover"] = ""
                    val database = FirebaseDatabase.getInstance()
                    val reference = database.getReference("Users")
                    reference.child(uid).setValue(data)

                    Toast.makeText(this, "Registered...\n" + user.email, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener{ exception ->
                progressBar.visibility = ProgressBar.INVISIBLE
                Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}
