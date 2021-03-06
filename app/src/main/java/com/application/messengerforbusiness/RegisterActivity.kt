package com.application.messengerforbusiness

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
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
    lateinit var spinner : Spinner
    var hasAdministrativePrivileges: Boolean = false

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
        spinner = findViewById(R.id.spinner)
        val items = arrayOf("No", "Yes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        spinner.adapter = adapter

        mDatabase = FirebaseDatabase.getInstance()

        progressBar = ProgressBar(this)

        mAddUserButton.setOnClickListener {
            val name = mNameET.text.toString().trim()
            val surname = mSurnameET.text.toString().trim()
            val position = mPositionET.text.toString().trim()
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
                registerUser(name, surname, position, eMail, password)
            }
        }
        spinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> {
                        hasAdministrativePrivileges = false
                    }
                    1 -> {
                        hasAdministrativePrivileges = true
                    }
                }
            }

        })
    }

    private fun registerUser(name: String, surname: String, position: String, eMail: String, password: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        val mAuth = FirebaseAuth.getInstance()
        val firebaseOptionsBuilder = FirebaseOptions.Builder()
        firebaseOptionsBuilder.setApiKey("")
        firebaseOptionsBuilder.setDatabaseUrl("")
        firebaseOptionsBuilder.setProjectId("")
        firebaseOptionsBuilder.setApplicationId("")
        val firebaseOptions = firebaseOptionsBuilder.build()

        val newAuth = FirebaseApp.initializeApp(this, firebaseOptions, "second_auth_" + System.currentTimeMillis().toString())
        FirebaseAuth.getInstance(newAuth).createUserWithEmailAndPassword(eMail, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    val user = task.result!!.user
                    val uid = user!!.uid
                    val data = mutableMapOf<String, Any>()
                    data["email"] = eMail
                    data["uid"] = uid
                    data["name"] = name
                    data["surname"] = surname
                    data["position"] = position
                    data["onlineStatus"] = "none"
                    data["typingTo"] = "noOne"
                    data["phone"] = ""
                    data["image"] = ""
                    data["cover"] = ""
                    data["hasAdministrativePrivileges"] = hasAdministrativePrivileges
                    data["deleted"] = false
                    val database = FirebaseDatabase.getInstance()
                    val reference = database.getReference("Users")
                    reference.child(uid).setValue(data)

                    Toast.makeText(this, "Registered...\n" + user.email, Toast.LENGTH_SHORT).show()
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
