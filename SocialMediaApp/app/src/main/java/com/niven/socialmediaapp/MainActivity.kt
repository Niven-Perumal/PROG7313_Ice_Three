package com.niven.socialmediaapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        postsRecyclerView.setHasFixedSize(true) // Optimize performance


        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfile::class.java))
        }

        findViewById<Button>(R.id.btnCreatePost).setOnClickListener {
            startActivity(Intent(this, CreatePost::class.java))
        }

        loadPosts()
    }

    private fun loadPosts() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MainActivity", "Error loading posts", error)
                    Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show()
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

                        Toast.makeText(this@MainActivity,
                            "Clicked: ${post.caption}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        loadPosts()
    }
}