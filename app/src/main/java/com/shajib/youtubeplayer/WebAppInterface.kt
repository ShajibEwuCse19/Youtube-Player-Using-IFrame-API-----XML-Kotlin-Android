package com.shajib.youtubeplayer

import android.webkit.JavascriptInterface
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.time.toDuration

/**
 * @author Shajib
 * @since Sept 14, 2024
 **/
class WebAppInterface(private val activity: MainActivity) {

    @JavascriptInterface
    fun onVideoDuration(duration: Double) {
        activity.onDurationAvailable(duration)
    }

    @JavascriptInterface
    fun onVideoFinished() {
        activity.runOnUiThread {
            Toast.makeText(activity, "Video Finished", Toast.LENGTH_SHORT).show()
        }
    }

    /*@JavascriptInterface
    fun onVideoPause() {
        activity.runOnUiThread {
            Toast.makeText(activity, "Video Playing", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun onVideoPlay() {
        activity.runOnUiThread {
            Toast.makeText(activity, "Video Paused", Toast.LENGTH_SHORT).show()
        }
    }*/

    @JavascriptInterface
    fun onVideoBuffering() {
        activity.runOnUiThread {
            Toast.makeText(activity, "Buffering...", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun getLastSavedPosition(): Float {
        return activity.getLastSavedPosition()
    }

}

