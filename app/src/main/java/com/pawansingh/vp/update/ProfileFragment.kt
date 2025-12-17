package com.pawansingh.vp.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.pawansingh.vp.R

class ProfileFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.folder_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.rateUs -> {
                val uri = Uri.parse("https://play.google.com/store/apps/details?id=" + requireContext().packageName)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }

            R.id.share_app -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Check this app:\nhttps://play.google.com/store/apps/details?id=" + requireContext().packageName
                )
                shareIntent.type = "text/plain"
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
        }
        return true
    }
}