package com.niven.socialmediaapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfile::class.java))
        }

        findViewById<Button>(R.id.btnCreatePost).setOnClickListener {
            startActivity(Intent(this, CreatePost::class.java))
        }

        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)

        loadPosts()
    }

    private fun loadPosts() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                val adapter = PostsAdapter(posts).apply {
                    // to do?: Handle item clicks
                    onPostClickListener = { post ->
                        // will go here Handle post click
                    }
                }
                postsRecyclerView.adapter = adapter
            }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }








}