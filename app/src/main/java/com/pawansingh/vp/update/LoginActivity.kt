package com.pawansingh.vp.update

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.pawansingh.vp.R

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v: View?, insets: WindowInsetsCompat? ->
            val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val email = findViewById<EditText>(R.id.etEmailLogin)
        val pass = findViewById<EditText>(R.id.etPasswordLogin)
        val btn = findViewById<Button>(R.id.btnLogin)

        btn.setOnClickListener {

            val emailStr = email.text.toString()
            val passStr = pass.text.toString()

            auth.signInWithEmailAndPassword(emailStr, passStr)
                .addOnSuccessListener {
                    Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HubActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }
    }
}