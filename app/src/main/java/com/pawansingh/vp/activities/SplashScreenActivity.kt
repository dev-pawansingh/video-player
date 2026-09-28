package com.pawansingh.vp.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.pawansingh.vp.R

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val duration = 2500L
        val progressBar = findViewById<ProgressBar>(R.id.splash_progress)
        ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
            finish()
        }, duration)
    }
}