package com.pawansingh.vp.update

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import com.pawansingh.vp.R

class HubActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hub)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val bottomNav = findViewById<ChipNavigationBar>(R.id.bottomNav)

        bottomNav.setItemSelected(R.id.nav_home,true)
        bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.white))

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.container, HomeFragment())
            commit()
        }

        bottomNav.setOnItemSelectedListener { id ->
            when (id) {
                R.id.nav_home -> {
                    bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                    supportFragmentManager.beginTransaction().replace(R.id.container, HomeFragment()).commit()
                }
                R.id.nav_shorts -> {
                    bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.black))
                    supportFragmentManager.beginTransaction().replace(R.id.container, ShortsFragment()).commit()
                }
                R.id.nav_profile -> {
                    bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                    supportFragmentManager.beginTransaction().replace(R.id.container,ProfileFragment()).commit()
                }
            }
        }
    }
}