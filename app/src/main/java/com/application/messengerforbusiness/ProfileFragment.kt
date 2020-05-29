package com.application.messengerforbusiness

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.application.messengerforbusiness.models.ModelUser
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.EmailAuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.FirebaseStorage.getInstance
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.dialog_update_password.*
import java.lang.Exception

/**
 * A simple [Fragment] subclass.
 */
class ProfileFragment : Fragment() {

    protected lateinit var mAuth: FirebaseAuth
    protected lateinit var user: FirebaseUser
    protected lateinit var database: FirebaseDatabase
    protected lateinit var databaseReference: DatabaseReference
    protected lateinit var storageReference: StorageReference
    val storagePath = "Users_Profile_Cover_Images/"

    protected lateinit var avatarIV: ImageView
    protected lateinit var nameTV: TextView
    protected lateinit var emailTV: TextView
    protected lateinit var phoneTV: TextView
    protected lateinit var positionTV: TextView
    protected lateinit var coverIV: ImageView
    protected lateinit var fab: FloatingActionButton
    protected lateinit var adminFab: FloatingActionButton
    protected lateinit var progressBar: ProgressBar
    protected lateinit var deletedListener: ValueEventListener
    protected lateinit var refForDeleted: DatabaseReference

    val CAMERA_REQUEST_CODE = 100
    val STORAGE_REQUEST_CODE = 200
    val IMAGE_PICK_GALLERY_CODE = 300
    val IMAGE_PICK_CAMERA_CODE = 400
    protected lateinit var storagePermissions: Array<String>
    protected lateinit var cameraPermissions: Array<String>
    protected lateinit var imageUri: Uri
    protected lateinit var profileOrCoverPhoto: String
    protected var hasAdministrativePrivileges = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        mAuth = FirebaseAuth.getInstance()
        user = mAuth.currentUser!!
        database = FirebaseDatabase.getInstance()
        databaseReference = database.getReference("Users")
        storageReference = getInstance().reference

        avatarIV = view.findViewById(R.id.avatarIV)
        coverIV = view.findViewById(R.id.coverIV)
        nameTV = view.findViewById(R.id.fullNameTv)
        emailTV = view.findViewById(R.id.emailTv)
        phoneTV = view.findViewById(R.id.phoneTv)
        positionTV = view.findViewById(R.id.positionTV)
        fab = view.findViewById(R.id.floatingButton)
        adminFab = view.findViewById(R.id.floatingAdminButton)
        progressBar = ProgressBar(activity as Context)
        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val query = databaseReference.orderByChild("email").equalTo(user.email)
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
                    if (it.child("hasAdministrativePrivileges").value != null) {
                        hasAdministrativePrivileges =
                            it.child("hasAdministrativePrivileges").value as Boolean
                    }
                    val fullName = name.toString() + " " + surname.toString()
                    nameTV.text = fullName
                    emailTV.text = email as CharSequence?
                    phoneTV.text = phone as CharSequence?
                    positionTV.text = position as CharSequence?
                    try {
                        Picasso.get().load(image as String?).placeholder(R.drawable.ic_default_image).into(avatarIV)
                    } catch (e: Exception) {

                    }
                    try {
                        Picasso.get().load(cover as String?).into(coverIV)
                    } catch (e: Exception) {

                    }
                }
            }
        })

        fab.setOnClickListener {
            showEditProfileDialog()
        }

        adminFab.setOnClickListener {
            if (hasAdministrativePrivileges) {
                showAdminOptionsDialog()
            } else {
                Toast.makeText(
                    activity,
                    "Only users with administrator privileges can handle this",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return view
    }

    private fun showAdminOptionsDialog() {
        val options = arrayOf(
            "Add user",
            "Delete user",
            "Add task"
        )
        val builder = AlertDialog.Builder(activity as Context)
        builder.setTitle("Choose action")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    startActivity(Intent(activity, RegisterActivity::class.java))
                }
                1 -> {
                    showDeleteUserDialog()
                }
                2 -> {
                    showCreateTaskDialog()
                }
            }
        }
        builder.create().show()
    }

    private fun showCreateTaskDialog() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_create_task, null)
        val receiverET: EditText = view.findViewById(R.id.receiverEmailET)
        val taskNameET: EditText = view.findViewById(R.id.taskNameET)
        val descriptionET: EditText = view.findViewById(R.id.descriptionET)
        val deadlineET: EditText = view.findViewById(R.id.deadlineET)
        val button: Button = view.findViewById(R.id.createTaskButton)
        val builder = AlertDialog.Builder(activity as Context)
        builder.setView(view)
        val ad = builder.create()
        ad.show()
        button.setOnClickListener {
            val data = mutableMapOf<String, Any>()
            val dbRef = FirebaseDatabase.getInstance().reference
            data["creator"] = user.email.toString()
            data["receiver"] = receiverET.text.toString()
            data["taskName"] = taskNameET.text.toString()
            data["description"] = descriptionET.text.toString()
            data["deadline"] = deadlineET.text.toString()
            data["timeStamp"] = System.currentTimeMillis().toString()
            dbRef.child("Tasks").push().setValue(data)
            Toast.makeText(activity, "Task created", Toast.LENGTH_SHORT).show()
            ad.dismiss()
        }
    }

    private fun showDeleteUserDialog() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_delete_user, null)
        val emailET: EditText = view.findViewById(R.id.emailET)
        val button: Button = view.findViewById(R.id.deleteUserButton)
        val builder = AlertDialog.Builder(activity as Context)
        builder.setView(view)
        builder.create().show()
        button.setOnClickListener {
            refForDeleted = FirebaseDatabase.getInstance().getReference("Users")
            deletedListener = refForDeleted.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    for (ds in p0.children) {
                        val user = ds.getValue(ModelUser :: class.java)
                        if (user!!.email == emailET.text.toString()) {
                            val data = mutableMapOf<String, Any>()
                            data["deleted"] = true
                            ds.ref.updateChildren(data)
                            Toast.makeText(activity, "User deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            })
        }
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            activity as Activity,
            storagePermissions,
            STORAGE_REQUEST_CODE
        )
    }

    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            activity as Context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val result2 = ContextCompat.checkSelfPermission(
            activity as Context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return result && result2
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity as Activity,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
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
                            activity as Context,
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
                            activity,
                            "Please enable storage permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                imageUri = data!!.data!!
                uploadProfileCoverPhoto(imageUri)
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                uploadProfileCoverPhoto(imageUri)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun uploadProfileCoverPhoto(imageUri: Uri) {
        progressBar.visibility = ProgressBar.VISIBLE
        val filePathAndName = storagePath + profileOrCoverPhoto + "_" + user.uid
        val secondStorageReference = storageReference.child(filePathAndName)
        secondStorageReference.putFile(imageUri).addOnSuccessListener {
            val uriTask = it.storage.downloadUrl
            while (!uriTask.isSuccessful) {

            }
            val downloadUri = uriTask.result
            if (uriTask.isSuccessful) {
                val results = mutableMapOf<String, Any>()
                results[profileOrCoverPhoto] = downloadUri.toString()
                databaseReference.child(user.uid).updateChildren(results).addOnSuccessListener {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    Toast.makeText(activity, "Image updated...", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    Toast.makeText(activity, "Update image failed...", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                progressBar.visibility = ProgressBar.INVISIBLE
                Toast.makeText(activity, "Some error occured", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            progressBar.visibility = ProgressBar.INVISIBLE
            Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteCurrentImage() {
        progressBar.visibility = ProgressBar.VISIBLE
        val imageRef =
            storageReference.storage.getReference(storagePath + profileOrCoverPhoto + "_" + user.uid)
        imageRef.delete().addOnSuccessListener {
            val results = mutableMapOf<String, Any>()
            results[profileOrCoverPhoto] = ""
            databaseReference.child(user.uid).updateChildren(results).addOnSuccessListener {
                progressBar.visibility = ProgressBar.INVISIBLE
                Toast.makeText(activity, "Image deleted...", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                progressBar.visibility = ProgressBar.INVISIBLE
                Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            progressBar.visibility = ProgressBar.INVISIBLE
            Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show()
        }
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
        imageUri =
            activity!!.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE)
    }

    private fun showEditProfileDialog() {
        val options = arrayOf(
            "Edit profile picture",
            "Edit cover photo",
            "Edit name",
            "Edit surname",
            "Edit phone",
            "Edit Position",
            "Change password"
        )
        val builder = AlertDialog.Builder(activity as Context)
        builder.setTitle("Choose action")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    profileOrCoverPhoto = "image"
                    showImagePicDialog()
                }
                1 -> {
                    profileOrCoverPhoto = "cover"
                    showImagePicDialog()
                }
                2 -> {
                    showTextUpdateDialog("name")
                }
                3 -> {
                    showTextUpdateDialog("surname")
                }
                4 -> {
                    showTextUpdateDialog("phone")
                }
                5 -> {
                    showTextUpdateDialog("position")
                }
                6 -> {
                    showChangePasswordDialog()
                }
            }
        }
        builder.create().show()
    }

    private fun showChangePasswordDialog() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_update_password, null)
        val passwordET: EditText = view.findViewById(R.id.passwordET)
        val newPasswordET: EditText = view.findViewById(R.id.cPasswordET)
        val button: Button = view.findViewById(R.id.updatePasswordButton)
        val builder = AlertDialog.Builder(activity as Context)
        builder.setView(view)
        builder.create().show()
        button.setOnClickListener {
            val oldPassword = passwordET.text.toString().trim()
            val newPassword = newPasswordET.text.toString().trim()
            if (TextUtils.isEmpty(oldPassword)) {
                Toast.makeText(activity, "Enter your current password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.length < 6) {
                Toast.makeText(
                    activity,
                    "Password length must atleast 6 characters",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            updatePassword(oldPassword, newPassword)
        }
    }

    private fun updatePassword(oldPassword: String, newPassword: String) {
        val user = mAuth.currentUser

        val authCredential = EmailAuthProvider.getCredential(user!!.email!!, oldPassword)
        user.reauthenticate(authCredential).addOnSuccessListener {
            user.updatePassword(newPassword).addOnSuccessListener {
                Toast.makeText(activity, "Password updated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTextUpdateDialog(s: String) {
        val builder = android.app.AlertDialog.Builder(activity)
        builder.setTitle("Update $s")
        val linearLayout = LinearLayout(activity)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(10, 10, 10, 10)
        val editText = EditText(activity)
        editText.hint = "Enter $s"
        linearLayout.addView(editText)
        builder.setView(linearLayout)

        builder.setPositiveButton("Update") { _, _ ->
            val value = editText.text.toString().trim()
            if (!TextUtils.isEmpty(value)) {
                progressBar.visibility = ProgressBar.VISIBLE
                val result = mutableMapOf<String, Any>()
                result[s] = value
                databaseReference.child(user.uid).updateChildren(result).addOnSuccessListener {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    Toast.makeText(activity, "Updated...", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    Toast.makeText(activity, it.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(activity, "Please enter $s", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()

    }

    private fun showImagePicDialog() {
        val options = arrayOf("Camera", "Gallery", "Default")
        val builder = AlertDialog.Builder(activity as Context)
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
            } else if (which == 2) {
                deleteCurrentImage()
                if (profileOrCoverPhoto == "image") {
                    Picasso.get().load(R.drawable.ic_default_image).into(avatarIV)
                } else if (profileOrCoverPhoto == "cover") {
                    coverIV.setImageResource(android.R.color.transparent)
                }
            }
        }
        builder.create().show()
    }

    private fun checkUserStatus() {
        val user = mAuth.currentUser
        if (user != null) {

        } else {
            startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        val item = menu.findItem(R.id.action_search)
        if (item != null) {
            item.isVisible = false
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_logout) {
            mAuth.signOut()
            checkUserStatus()
        }
        return super.onOptionsItemSelected(item)
    }
}
