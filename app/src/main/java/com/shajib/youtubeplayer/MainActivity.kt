package com.shajib.youtubeplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var youtubeWebView: WebView
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var restartButton: Button
    private lateinit var sizeButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var preview: ImageView
    private lateinit var textViewPreview: TextView
    private lateinit var durationButton: Button
    private lateinit var currentDuration: TextView

    private var isFullScreen = false
    private var isPlaying = false
    private var totalDuration: Double = 0.0
    private val handler = Handler(Looper.getMainLooper())
    private val VIDEO_ID = "ryUxrFUk6MY"
    private val thumbnailUrl = "https://img.youtube.com/vi/$VIDEO_ID/hqdefault.jpg"
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "YouTubePrefs"
    private val KEY_LAST_POSITION = "last_position"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        youtubeWebView = findViewById(R.id.youtubeWebView)
        playButton = findViewById(R.id.btnPlay)
        pauseButton = findViewById(R.id.btnPause)
        restartButton = findViewById(R.id.btnRestart)
        sizeButton = findViewById(R.id.btnSize)
        seekBar = findViewById(R.id.seekBar)
        preview = findViewById(R.id.iv_preview)
        durationButton = findViewById(R.id.btnDuration)
        currentDuration = findViewById(R.id.tv_time)
        textViewPreview = findViewById(R.id.tv_preview)

        loadPreview()

        youtubeWebView.settings.javaScriptEnabled = true
        youtubeWebView.webViewClient = WebViewClient()
        youtubeWebView.settings.mediaPlaybackRequiresUserGesture = false

        youtubeWebView.addJavascriptInterface(WebAppInterface(this), "android")

        val iframeHtml = """
        <html>
        <body style="margin:0;padding:0;">
        <iframe 
            id="player" 
            type="text/html" 
            width="100%"
            height="100%" 
            src="https://www.youtube.com/embed/$VIDEO_ID?enablejsapi=1&controls=0&modestbranding=1&rel=0&autohide=1&autoplay=0" 
            frameborder="0"
            allowfullscreen>
        </iframe>
        <script>
            var tag = document.createElement('script');
            tag.src = "https://www.youtube.com/iframe_api";
            var firstScriptTag = document.getElementsByTagName('script')[0];
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

            var player;
            function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                    events: {
                        'onReady': onPlayerReady,
                        'onStateChange': onPlayerStateChange
                    }
                });
            }
            
            // Get video duration
            function onPlayerReady(event) {
                var duration = player.getDuration();
                if (duration > 0) {
                    window.android.onVideoDuration(duration);

                    // Retrieve last saved position and seek to it
                    var lastPosition = window.android.getLastSavedPosition();
                    if (lastPosition > 0) {
                        player.seekTo(lastPosition, true);
                    }
                    player.pauseVideo();
                }
            }

            // Handle state change
            function onPlayerStateChange(event) {
                if (event.data == YT.PlayerState.ENDED) {
                    window.android.onVideoFinished();
                } else if (event.data == YT.PlayerState.PLAYING) {
                    window.android.onVideoPlay();
                } else if (event.data == YT.PlayerState.PAUSED) {
                    window.android.onVideoPause();
                } else if (event.data == YT.PlayerState.BUFFERING) {
                    window.android.onVideoBuffering();
                }
            }

            // Get current time of video
            function getCurrentTime() {
                return player.getCurrentTime();
            }

            // Control video actions
            function playVideo() {
                player.playVideo();
            }
            
            function pauseVideo() {
                player.pauseVideo();
            }
            
            function seekTo(seconds) {
                player.seekTo(seconds, true);
            }
            
            function restartVideo() {
                player.stopVideo();
            }
        </script>
        </body>
        </html>
    """.trimIndent()

        youtubeWebView.loadDataWithBaseURL("https://www.youtube.com", iframeHtml, "text/html", "utf-8", null)

        playButton.setOnClickListener {
            youtubeWebView.evaluateJavascript("javascript:playVideo()") {
                isPlaying = true
                startSeekBarUpdater()
                preview.visibility = View.GONE
                textViewPreview.visibility = View.GONE
            }
        }

        pauseButton.setOnClickListener {
            youtubeWebView.evaluateJavascript("javascript:pauseVideo()") {
                isPlaying = false
                stopSeekBarUpdater()
                saveCurrentVideoPosition() // Save video position when paused
            }
        }

        restartButton.setOnClickListener {
            youtubeWebView.evaluateJavascript("javascript:restartVideo()") {
                Toast.makeText(this, "Video Stopped", Toast.LENGTH_SHORT).show()
                currentDuration.text = "00:00"
                saveCurrentVideoPosition() // Save video position when stopped
            }
        }

        sizeButton.setOnClickListener { toggleFullScreen() }

        durationButton.setOnClickListener {
            Toast.makeText(this, "Video Duration: $totalDuration seconds", Toast.LENGTH_SHORT).show()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && totalDuration > 0) {
                    val seekToSeconds = (progress * totalDuration).toFloat() / seekBar!!.max
                    youtubeWebView.evaluateJavascript("javascript:seekTo($seekToSeconds)") {}
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // Save the current video time in SharedPreferences
    private fun saveCurrentVideoPosition() {
        youtubeWebView.evaluateJavascript("javascript:getCurrentTime()") { result ->
            val currentTime = result?.toDoubleOrNull() ?: 0.0
            val editor = sharedPreferences.edit()
            editor.putFloat(KEY_LAST_POSITION, currentTime.toFloat())
            editor.apply()
        }
    }

    // Retrieve the last saved video time from SharedPreferences
    fun getLastSavedPosition(): Float {
        return sharedPreferences.getFloat(KEY_LAST_POSITION, 0f)
    }

    private fun startSeekBarUpdater() {
        handler.post(object : Runnable {
            override fun run() {
                if (isPlaying) {
                    youtubeWebView.evaluateJavascript("javascript:getCurrentTime()") { result ->
                        val currentTime = result?.toDoubleOrNull() ?: 0.0
                        if (totalDuration > 0) {
                            seekBar.progress = (currentTime / totalDuration * seekBar.max).toInt()
                            val minutes = (currentTime / 60).toInt()
                            val seconds = (currentTime % 60).toInt()
                            val formattedTime = String.format("%02d:%02d", minutes, seconds)
                            currentDuration.text = formattedTime
                        }
                    }
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun stopSeekBarUpdater() {
        handler.removeCallbacksAndMessages(null)
    }

    fun onDurationAvailable(duration: Double) {
        totalDuration = duration
        seekBar.max = duration.toInt()
    }

    private fun toggleFullScreen() {
        if (isFullScreen) {
            youtubeWebView.layoutParams.height = 600
            youtubeWebView.layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
        } else {
            youtubeWebView.layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
        }
        youtubeWebView.requestLayout()
        isFullScreen = !isFullScreen
    }

    private fun loadPreview() {
        Picasso.get().load(thumbnailUrl).into(preview)
    }

    override fun onBackPressed() {
        if (isFullScreen) {
            toggleFullScreen()
        } else {
            super.onBackPressed()
        }
    }
}
