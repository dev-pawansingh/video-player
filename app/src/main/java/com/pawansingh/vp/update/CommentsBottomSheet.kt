package com.pawansingh.vp.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pawansingh.vp.R

class CommentsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var videoId: String
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: CommentsAdapter
    private lateinit var emptyView: TextView
    private var onCommentAdded: (() -> Unit)? = null

    companion object {
        fun newInstance(videoId: String): CommentsBottomSheet {
            val sheet = CommentsBottomSheet()
            val args = Bundle()
            args.putString("videoId", videoId)
            sheet.arguments = args
            return sheet
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoId = arguments?.getString("videoId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.bottom_comments, container, false)

        val recycler = view.findViewById<RecyclerView>(R.id.commentsRecycler)
        emptyView = view.findViewById(R.id.emptyView)
        val addComment = view.findViewById<TextView>(R.id.addComment)

        adapter = CommentsAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
        loadComments()

        addComment.setOnClickListener {
            showInputDialog()
        }

        return view
    }
    private fun loadComments() {
        db.collection("videos")
            .document(videoId)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    Comment(
                        uid = it.getString("uid") ?: "",
                        text = it.getString("text") ?: "",
                        timestamp = it.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()

                adapter.submitList(list)

                emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }
    private fun showInputDialog() {
        val input = EditText(requireContext())
        input.hint = "Write a comment..."

        AlertDialog.Builder(requireContext())
            .setTitle("Add Comment")
            .setView(input)
            .setPositiveButton("Post") { _, _ ->
                val text = input.text.toString().trim()
                val uid = FirebaseAuth.getInstance().uid ?: return@setPositiveButton

                if (text.isNotEmpty()) addComment(text, uid)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addComment(text: String, uid: String) {
        val comment = hashMapOf(
            "uid" to uid,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )
        val videoRef = db.collection("videos").document(videoId)

        db.runTransaction { txn ->
            txn.set(videoRef.collection("comments").document(), comment)
            txn.update(videoRef, "commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
        }.addOnSuccessListener {
            onCommentAdded?.invoke()
        }
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        val height = (resources.displayMetrics.heightPixels * 0.7).toInt()
        bottomSheet.layoutParams.height = height

        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    fun setOnCommentAddedListener(listener: () -> Unit) {
        onCommentAdded = listener
    }
}
