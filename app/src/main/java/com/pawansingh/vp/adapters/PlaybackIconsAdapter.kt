package com.pawansingh.vp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pawansingh.vp.R
import com.pawansingh.vp.models.IconModel

class PlaybackIconsAdapter(
    private val iconModelsList: ArrayList<IconModel?>,
    private val context: Context
) : RecyclerView.Adapter<PlaybackIconsAdapter.ViewHolder>() {

    private var mListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.icons_layout, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val iconModel = iconModelsList[position]

        iconModel?.let {
            holder.icon.setImageResource(it.imageView)
            holder.iconName.setText(it.iconTitle)
        }
    }

    override fun getItemCount(): Int {
        return iconModelsList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val icon: ImageView = itemView.findViewById(R.id.playback_icon)
        val iconName: TextView = itemView.findViewById(R.id.icon_title)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    mListener?.onItemClick(position)
                }
            }
        }
    }
}