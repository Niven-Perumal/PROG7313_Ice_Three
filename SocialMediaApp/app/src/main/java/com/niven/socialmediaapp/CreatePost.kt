package com.niven.socialmediaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class CreatePost : AppCompatActivity() {

    private lateinit var postImageView: ImageView
    private lateinit var captionEditText: EditText
    private lateinit var selectImageButton: Button
    private lateinit var postButton: Button

    private var selectedImageUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                postImageView.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        postImageView = findViewById(R.id.postImageView)
        captionEditText = findViewById(R.id.captionEditText)
        selectImageButton = findViewById(R.id.selectImageButton)
        postButton = findViewById(R.id.postButton)

        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        postButton.setOnClickListener {
            createPost()
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


    }

    private fun convertUriToBase64(uri: Uri): String? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
        }
    }

    private fun createPost() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val caption = captionEditText.text.toString().trim()

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }


        postButton.isEnabled = false
        postButton.text = "Posting..."

        // Convert image
        val base64Image = try {
            convertUriToBase64(selectedImageUri!!) ?: run {
                Toast.makeText(this, "Invalid image format", Toast.LENGTH_SHORT).show()
                postButton.isEnabled = true
                postButton.text = "Post"
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show()
            postButton.isEnabled = true
            postButton.text = "Post"
            return
        }

        // Get user data
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val userName = document.getString("name") ?: "Anonymous"

                val post = Post(
                    userId = user.uid,
                    userName = userName,  // Add author name
                    imageBase64 = base64Image,
                    caption = caption
                )

                firestore.collection("posts")
                    .add(post)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Posted successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        postButton.isEnabled = true
                        postButton.text = "Post"
                        Toast.makeText(this, "Post failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                postButton.isEnabled = true
                postButton.text = "Post"
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }
}