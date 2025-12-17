package com.pawansingh.vp.update

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pawansingh.vp.R

class ShortsAdapter(
    private val items: List<ShortsModel>,
    private val onInteraction: (Interaction) -> Unit
) : RecyclerView.Adapter<ShortsAdapter.VH>() {

    data class Interaction(val type: Type, val position: Int, val holder: VH) {
        enum class Type { LIKE, COMMENT, SHARE, CLICK }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: ViewGroup = itemView.findViewById(R.id.playerContainer)

        val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        val tvLikes: TextView = itemView.findViewById(R.id.tvLikes)

        val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        val tvComments: TextView = itemView.findViewById(R.id.tvComments)

        val btnShare: ImageView = itemView.findViewById(R.id.btnShare)
        val tvShares: TextView = itemView.findViewById(R.id.tvShares)

        val uploaderImage: ImageView = itemView.findViewById(R.id.uploader_img)
        val uploaderName: TextView = itemView.findViewById(R.id.tvUploader)
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val views: TextView = itemView.findViewById(R.id.tvViews)

        fun setLiked(isLiked: Boolean) {
            if (isLiked) {
                btnLike.setColorFilter(Color.BLUE)
            } else {
                btnLike.setColorFilter(Color.WHITE)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.shorts_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvLikes.text = formatCount(item.likes)
        holder.tvComments.text = formatCount(item.comments)
        holder.tvShares.text = formatCount(item.shares)

        holder.uploaderName.text = "By: ${item.uploaderName}"
        holder.title.text = item.title
        holder.views.text = "${formatCount(item.views)} views"
        holder.setLiked(item.liked)

        holder.btnLike.setOnClickListener {
            onInteraction(Interaction(Interaction.Type.LIKE, position, holder))
        }

        holder.btnComment.setOnClickListener {
            onInteraction(Interaction(Interaction.Type.COMMENT, position,holder))
        }

        holder.btnShare.setOnClickListener {
            onInteraction(Interaction(Interaction.Type.SHARE, position, holder))
        }
    }

    override fun getItemCount(): Int = items.size

    fun formatCount(count: Long): String {
        return when {
            count < 1_000 -> count.toString()
            count < 1_000_000 -> {
                val v = count / 1_000.0
                String.format("%.1fk", v).replace(".0k", "k")
            }
            count < 1_000_000_000 -> {
                val v = count / 1_000_000.0
                String.format("%.1fM", v).replace(".0M", "M")
            }
            else -> {
                val v = count / 1_000_000_000.0
                String.format("%.1fB", v).replace(".0B", "B")
            }
        }
    }
}
