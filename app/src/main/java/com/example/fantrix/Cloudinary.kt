package com.example.fantrix

import android.app.Application
import com.cloudinary.android.MediaManager

class Cloudinary : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = mapOf(
            "cloud_name" to "dkm3ouqar",
            "api_key" to "116595364332682",
            "api_secret" to "_1KC67GKy44aeHJLQD4hFY4qdBo"
        )

        MediaManager.init(this, config)
    }
}
