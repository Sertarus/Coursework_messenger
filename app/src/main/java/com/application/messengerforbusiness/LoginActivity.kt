package com.application.messengerforbusiness

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    protected lateinit var mEmailET: EditText
    protected lateinit var mPasswordET: EditText
    protected lateinit var mLoginButton: Button
    protected lateinit var mAuth: FirebaseAuth
    protected lateinit var progressBar: ProgressBar
    protected lateinit var mRecoverPass: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val actionBar = supportActionBar
        actionBar!!.title = "Login"
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)

        mEmailET = findViewById(R.id.emailET)
        mPasswordET = findViewById(R.id.passwordET)
        mLoginButton = findViewById(R.id.logButton)
        mRecoverPass = findViewById(R.id.recover_password)

        mAuth = FirebaseAuth.getInstance()

        progressBar = ProgressBar(this)

        mLoginButton.setOnClickListener {
            val email = mEmailET.text.toString().trim()
            val password = mPasswordET.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mEmailET.setError("Invalid email")
                mEmailET.isFocusable = true
            } else {
                loginUser(email, password)
            }
        }
        mRecoverPass.setOnClickListener{
            showRecoverPasswordDialog()
        }
    }

    private fun showRecoverPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        val linearLayout = LinearLayout(this)
        val emailET= EditText(this)
        emailET.hint = "Email"
        emailET.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        emailET.minEms = 15
        linearLayout.addView(emailET)
        linearLayout.setPadding(10,10,10,10)
        builder.setView(linearLayout)
        builder.setPositiveButton("Recover") { _, _ ->
            val email = emailET.text.toString().trim()
            beginRecovery(email)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun beginRecovery(email: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(this) {task ->
            if (task.isSuccessful) {
                progressBar.visibility = ProgressBar.INVISIBLE
                Toast.makeText(this, "Email sent", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Failed...", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener{exception ->
            Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun loginUser(email: String, password: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                progressBar.visibility = ProgressBar.INVISIBLE
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                progressBar.visibility = ProgressBar.INVISIBLE
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            progressBar.visibility = ProgressBar.INVISIBLE
            Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
        }
    }
}

