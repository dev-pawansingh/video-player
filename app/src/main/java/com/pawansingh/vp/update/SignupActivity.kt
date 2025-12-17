package com.pawansingh.vp.update

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pawansingh.vp.R

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v: View?, insets: WindowInsetsCompat? ->
            val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            v!!.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val name = findViewById<EditText>(R.id.etName)
        val email = findViewById<EditText>(R.id.etEmail)
        val pass = findViewById<EditText>(R.id.etPassword)
        val btn = findViewById<Button>(R.id.btnSignup)

        btn.setOnClickListener {

            val nameStr = name.text.toString()
            val emailStr = email.text.toString()
            val passStr = pass.text.toString()

            if (nameStr.isEmpty() || emailStr.isEmpty() || passStr.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailStr, passStr)
                .addOnSuccessListener {
                    val uid = it.user!!.uid

                    val user = UserModel(
                        uid = uid,
                        name = nameStr,
                        email = emailStr,
                        profilePic = ""
                    )

                    db.collection("users").document(uid).set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account Created", Toast.LENGTH_SHORT).show()
                            val returnPos = intent.getIntExtra("shorts_return_position", -1)
                            val resultIntent = Intent()
                            resultIntent.putExtra("shorts_return_position", returnPos)
                            setResult(RESULT_OK, resultIntent)
                            finish() // Activity close karo
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }
    }
}