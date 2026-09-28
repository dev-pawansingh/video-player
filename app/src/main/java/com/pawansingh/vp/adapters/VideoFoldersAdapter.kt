package com.pawansingh.vp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pawansingh.vp.R
import com.pawansingh.vp.activities.VideoFilesActivity
import com.pawansingh.vp.models.MediaFiles

class VideoFoldersAdapter(
    private val mediaFiles: ArrayList<MediaFiles>,
    private val folderPath: ArrayList<String?>,
    private val context: Context
) : RecyclerView.Adapter<VideoFoldersAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.folder_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val indexPath = folderPath.get(position)!!.lastIndexOf("/")
        val nameOfFolder = folderPath.get(position)!!.substring(indexPath + 1)
        holder.folderName.setText(nameOfFolder)
        holder.folderPath.setText(folderPath.get(position))
        //holder.noOfFiles.setText(noOfFolders(folderPath.get(position))+" Videos");
        holder.noOfFiles.setText(noOfFolders(folderPath.get(position)!!).toString() + " Videos")

        holder.itemView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                val intent = Intent(context, VideoFilesActivity::class.java)
                intent.putExtra("folderName", nameOfFolder)
                context.startActivity(intent)
            }
        })
    }

    override fun getItemCount(): Int {
        return folderPath.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var folderName: TextView
        var folderPath: TextView
        var noOfFiles: TextView

        init {
            folderName = itemView.findViewById<TextView>(R.id.foldername)
            folderPath = itemView.findViewById<TextView>(R.id.folderpath)
            noOfFiles = itemView.findViewById<TextView>(R.id.noOfFiles)
        }
    }

    fun noOfFolders(folder_name: String): Int {
        var files_no = 0
        for (mediaFiles in mediaFiles) {
            if (mediaFiles.path?.substring(0, mediaFiles.path!!.lastIndexOf("/"))
                ?.endsWith(folder_name) ?: false
            ) {
                files_no++
            }
        }
        return files_no
    }
}