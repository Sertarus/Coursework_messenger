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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.FirebaseStorage.getInstance
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
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
    protected lateinit var progressBar: ProgressBar

    val CAMERA_REQUEST_CODE = 100
    val STORAGE_REQUEST_CODE = 200
    val IMAGE_PICK_GALLERY_CODE = 300
    val IMAGE_PICK_CAMERA_CODE = 400
    protected lateinit var storagePermissions: Array<String>
    protected lateinit var cameraPermissions: Array<String>
    protected lateinit var imageUri: Uri
    protected lateinit var profileOrCoverPhoto: String

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

        fab.setOnClickListener {
            showEditProfileDialog()
        }

        return view
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
        }.addOnFailureListener{
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
            "Edit Position"
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
            }
        }
        builder.create().show()
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
                }
                else if (profileOrCoverPhoto == "cover") {
                    coverIV.setImageResource(android.R.color.transparent)
                }
            }
        }
        builder.create().show()
    }
}
