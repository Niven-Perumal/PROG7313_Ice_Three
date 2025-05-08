package com.niven.socialmediaapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(private val posts: List<Post>) :
    RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.postImage)
        val postCaption: TextView = itemView.findViewById(R.id.postCaption)
        val postTimestamp: TextView = itemView.findViewById(R.id.postTimestamp)
        val postAuthor: TextView = itemView.findViewById(R.id.postAuthor)
        val authorAvatar: ImageView = itemView.findViewById(R.id.authorAvatar)

        // Likes
        val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)

        // Comments
        val etComment: EditText = itemView.findViewById(R.id.etComment)
        val btnPostComment: Button = itemView.findViewById(R.id.btnPostComment)
        val rvComments: RecyclerView = itemView.findViewById(R.id.rvComments)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        //  post info
        holder.postCaption.text = post.caption
        holder.postAuthor.text = post.userName
        holder.tvLikeCount.text = post.likes.toString()

        // Like button
        holder.btnLike.setImageResource(
            if (post.likedBy.contains(auth.currentUser?.uid)) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )

        // Post image
        if (post.imageBase64.isNotEmpty()) {
            try {
                holder.postImage.setImageBitmap(base64ToBitmap(post.imageBase64))
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.outline_image_24)
            }
        }

        // Timestamp
        post.timestamp?.let {
            holder.postTimestamp.text = formatTimestamp(it)
        }

        // Author profile pic
        loadAuthorAvatar(post.userId, holder.authorAvatar)

        // Like functionality
        holder.btnLike.setOnClickListener {
            val updates = hashMapOf<String, Any>(
                "likes" to if (post.likedBy.contains(auth.currentUser?.uid))
                    post.likes - 1
                else post.likes + 1,
                "likedBy" to if (post.likedBy.contains(auth.currentUser?.uid))
                    FieldValue.arrayRemove(auth.currentUser?.uid)
                else FieldValue.arrayUnion(auth.currentUser?.uid)
            )
            firestore.collection("posts").document(post.postId).update(updates)
        }

        // Comment functionality
        holder.rvComments.layoutManager = LinearLayoutManager(holder.itemView.context)
        loadComments(post.postId, holder.rvComments)

        holder.btnPostComment.setOnClickListener {
            val commentText = holder.etComment.text.toString()
            if (commentText.isBlank()) return@setOnClickListener

            firestore.collection("users").document(auth.currentUser?.uid ?: "").get()
                .addOnSuccessListener { userDoc ->
                    val comment = Comment(
                        userId = auth.currentUser?.uid ?: "",
                        userName = userDoc.getString("name") ?: "Anonymous",
                        text = commentText,
                        userAvatar = userDoc.getString("profilePicBase64") ?: ""
                    )

                    firestore.collection("posts").document(post.postId)
                        .collection("comments").add(comment)
                        .addOnSuccessListener { holder.etComment.text.clear() }
                }
        }
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
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val base64Avatar = document.getString("profilePicBase64")
                if (!base64Avatar.isNullOrEmpty()) {
                    try {
                        imageView.setImageBitmap(base64ToBitmap(base64Avatar))
                    } catch (e: Exception) {
                        imageView.setImageResource(R.drawable.outline_account_circle_24)
                    }
                }
            }
    }

    private fun loadComments(postId: String, recyclerView: RecyclerView) {
        firestore.collection("posts").document(postId)
            .collection("comments").orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                recyclerView.adapter = CommentsAdapter(comments)
            }
    }

    private inner class CommentsAdapter(private val comments: List<Comment>) :
        RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

        inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvAuthor: TextView = itemView.findViewById(R.id.tvCommentAuthor)
            val tvText: TextView = itemView.findViewById(R.id.tvCommentText)
            val ivAvatar: ImageView = itemView.findViewById(R.id.ivCommentAvatar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comment, parent, false)
            return CommentViewHolder(view)
        }

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            val comment = comments[position]
            holder.tvAuthor.text = comment.userName
            holder.tvText.text = comment.text

            if (comment.userAvatar.isNotEmpty()) {
                try {
                    holder.ivAvatar.setImageBitmap(base64ToBitmap(comment.userAvatar))
                } catch (e: Exception) {
                    holder.ivAvatar.setImageResource(R.drawable.outline_account_circle_24)
                }
            }
        }

        override fun getItemCount() = comments.size
    }

    var onPostClickListener: ((Post) -> Unit)? = null
}