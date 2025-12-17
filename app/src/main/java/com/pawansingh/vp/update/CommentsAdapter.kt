package com.pawansingh.vp.update

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pawansingh.vp.R
import de.hdodenhof.circleimageview.CircleImageView

class CommentsAdapter : RecyclerView.Adapter<CommentsAdapter.Holder>() {

    private val data = mutableListOf<Comment>()

    fun submitList(newList: List<Comment>) {
        data.clear()
        data.addAll(newList)
        notifyDataSetChanged()
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProfile = itemView.findViewById<CircleImageView>(R.id.imgProfile)
        val tvUser = itemView.findViewById<TextView>(R.id.tvUser)
        val tvComment = itemView.findViewById<TextView>(R.id.tvComment)
        val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val c = data[position]
        holder.tvUser.text = c.uid
        holder.tvComment.text = c.text
        holder.tvTime.text = formatTime(c.timestamp)
    }
    private fun formatTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    override fun getItemCount() = data.size
}