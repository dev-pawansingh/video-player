package com.pawansingh.vp.models

import java.util.Locale

object Utility {
    fun timeConversion(millie: Long): String {
        val seconds = (millie / 1000)
        val sec = seconds % 60
        val min = (seconds / 60) % 60
        val hrs = (seconds / (60 * 60)) % 24

        if (hrs == 0L) {
            return min.toString() + (":" + String.format(Locale.UK, "%02d", sec))
        } else {
            return hrs.toString() + (":" + (String.format(
                Locale.UK,
                "%02d",
                min
            ) + (":" + String.format(
                Locale.UK, "%02d", sec
            ))))
        }
    }
}
