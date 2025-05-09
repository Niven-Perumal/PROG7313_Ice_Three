package com.niven.socialmediaapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.niven.socialmediaapp.MainActivity
import java.io.ByteArrayOutputStream
import java.io.InputStream

class EditProfile : AppCompatActivity() {

    private lateinit var etUpdateName: EditText
    private lateinit var ivProfilePic: ImageView
    private lateinit var btnSelectNewPic: Button
    private lateinit var btnSaveProfile: Button

    private var selectedImageUri: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var postsRecyclerView: RecyclerView


    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isStoragePermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            val isMediaPermissionGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false

            if (isStoragePermissionGranted || isMediaPermissionGranted) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        etUpdateName = findViewById(R.id.etUpdateName)
        ivProfilePic = findViewById(R.id.ivProfilePic)
        btnSelectNewPic = findViewById(R.id.btnSelectNewPic)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        btnSelectNewPic.setOnClickListener {
            checkAndRequestPermissions()
        }

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, EditProfile::class.java))
                    true
                }
                else -> false
            }
        }


        loadUserProfile()


        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)

        loadPosts()

    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            openImagePicker()
        } else {
            requestMultiplePermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 2000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 2000 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            ivProfilePic.setImageURI(selectedImageUri)
        }
    }

    private fun convertUriToBase64(uri: Uri): String? {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveProfile() {
        val newName = etUpdateName.text.toString()
        val userId = auth.currentUser?.uid ?: return

        if (newName.isBlank()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any>("name" to newName)

        if (selectedImageUri != null) {
            val base64Image = convertUriToBase64(selectedImageUri!!)
            if (base64Image != null) {
                updates["profilePicBase64"] = base64Image
            }
        }

        firestore.collection("users").document(userId)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                loadUserProfile()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val base64Image = document.getString("profilePicBase64")

                    etUpdateName.setText(name)

                    if (!base64Image.isNullOrEmpty()) {
                        val bitmap = base64ToBitmap(base64Image)
                        ivProfilePic.setImageBitmap(bitmap)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes: ByteArray = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }



    private fun loadPosts() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("posts")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MainActivity", "Error loading posts", error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject(Post::class.java)?.copy(postId = document.id)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error parsing post", e)
                        null
                    }
                } ?: emptyList()

                postsRecyclerView.adapter = PostsAdapter(posts).apply {
                    onPostClickListener = { post ->

                        Toast.makeText(this@EditProfile,
                            "Clicked: ${post.caption}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

}
