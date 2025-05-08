package com.niven.socialmediaapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(private val posts: List<Post>) :
    RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {


    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.postImage)
        val postCaption: TextView = itemView.findViewById(R.id.postCaption)
        val postTimestamp: TextView = itemView.findViewById(R.id.postTimestamp)
        val postAuthor: TextView = itemView.findViewById(R.id.postAuthor)
        val authorAvatar: ImageView = itemView.findViewById(R.id.authorAvatar)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.postCaption.text = post.caption
        holder.postAuthor.text = post.userName

        // Load post image
        if (post.imageBase64.isNotEmpty()) {
            try {
                val bitmap = base64ToBitmap(post.imageBase64)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.outline_image_24)
            }
        }

        // timestamp
        post.timestamp?.let {
            holder.postTimestamp.text = formatTimestamp(it)
        }

        // Load author avatar
        loadAuthorAvatar(post.userId, holder.authorAvatar)
    }

    override fun getItemCount() = posts.size


    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun formatTimestamp(timestamp: Timestamp): String {
        return SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            .format(timestamp.toDate())
    }

    private fun loadAuthorAvatar(userId: String, imageView: ImageView) {
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val base64Avatar = document.getString("profilePicBase64")
                if (!base64Avatar.isNullOrEmpty()) {
                    try {
                        val bitmap = base64ToBitmap(base64Avatar)
                        imageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        imageView.setImageResource(R.drawable.outline_account_circle_24)
                    }
                }
            }
    }

    // to do later? Click listener for posts
    var onPostClickListener: ((Post) -> Unit)? = null
}