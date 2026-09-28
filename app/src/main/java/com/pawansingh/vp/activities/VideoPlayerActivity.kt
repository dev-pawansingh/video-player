package com.pawansingh.vp.activities

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.ContentUris
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.util.Rational
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bullhead.equalizer.EqualizerFragment
import com.developer.filepicker.controller.DialogSelectionListener
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import com.developer.filepicker.view.FilePickerDialog
import com.pawansingh.vp.R
import com.pawansingh.vp.adapters.PlaybackIconsAdapter
import com.pawansingh.vp.adapters.VideoFilesAdapter
import com.pawansingh.vp.models.BrightnessDialog
import com.pawansingh.vp.models.IconModel
import com.pawansingh.vp.models.MediaFiles
import com.pawansingh.vp.models.OnSwipeTouchListener
import com.pawansingh.vp.models.PlaylistDialog
import com.pawansingh.vp.models.VolumeDialog
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@UnstableApi
class VideoPlayerActivity : AppCompatActivity(), View.OnClickListener {

    private data class TrackOption(
        val trackGroup: TrackGroup,
        val trackIndex: Int,
        val name: String,
        val format: Format
    )

    private data class SubtitleOption(
        val type: Int,
        val track: TrackOption?,
        val name: String
    ) {
        companion object {
            const val TYPE_NONE = 0
            const val TYPE_EMBEDDED = 1
            const val TYPE_EXTERNAL = 2
        }
    }

    var mVideoFiles: ArrayList<MediaFiles?>? = ArrayList<MediaFiles?>()
    var playerView: PlayerView? = null
    var player: ExoPlayer? = null
    var position: Int = 0
    var videoTitle: String? = null
    var title: TextView? = null
    var videoWindow: Window? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var scaleFactor = 1f
    private var translationX = 0f
    private var translationY = 0f
    private var isZoomed = false
    private var activePointerCount = 0
    private var videoSurfaceView: View? = null

    private val handler = Handler()
    private val hideControlsRunnable: Runnable = object : Runnable {
        override fun run() {
            if (playerView!!.isControllerFullyVisible) {
                animateControlsOut()
                hideSystemUI()
            }
        }
    }

    var videoBack: ImageView? = null
    var lock: ImageView? = null
    var unlock: ImageView? = null
    var scaling: ImageView? = null
    var videoList: ImageView? = null
    var videoMore: ImageView? = null
    var rotate: ImageView? = null
    var audio_track: ImageView? = null
    var videoFilesAdapter: VideoFilesAdapter? = null
    var root: RelativeLayout? = null
    var zoomContainer: LinearLayout? = null
    var zoomLevelText: TextView? = null
    var resetZoomBtn: ImageButton? = null
    var nextButton: ImageView? = null
    var previousButton: ImageView? = null
    var playButton: ImageView? = null
    var pauseButton: ImageView? = null
    var concatenatingMediaSource: ConcatenatingMediaSource? = null
    var videoContainer: FrameLayout? = null

    // Horizontal recyclerview variables
    private val iconModelsArrayList = ArrayList<IconModel?>()
    var playbackIconsAdapter: PlaybackIconsAdapter? = null
    var recyclerViewIcons: RecyclerView? = null
    var expand: Boolean = false
    var nightMode: View? = null
    var dark: Boolean = false
    var mute: Boolean = false
    var parameters: PlaybackParameters? = null
    var speed: Float = 0f
    var dialogProperties: DialogProperties? = null
    var filePickerDialog: FilePickerDialog? = null
    var uriSubtitle: Uri? = null
    var pictureInPicture: PictureInPictureParams.Builder? = null
    var isCrossChecked: Boolean = false
    var eqContainer: FrameLayout? = null

    // Swipe and zoom Variables
    private var device_height = 0
    private var device_width = 0
    private var brightness = 0
    private var mediaVolume = 0
    var start: Boolean = false
    var left: Boolean = false
    var right: Boolean = false
    private var baseX = 0f
    private var baseY = 0f
    var swipe_move: Boolean = false
    var diffX: Long = 0
    var diffY: Long = 0
    var success: Boolean = false
    private var writeSettingsPromptShown = false
    var vol_text: TextView? = null
    var brt_text: TextView? = null
    var vol_progress: ProgressBar? = null
    var brt_progress: ProgressBar? = null
    var vol_progress_container: LinearLayout? = null
    var vol_text_container: LinearLayout? = null
    var brt_progress_container: LinearLayout? = null
    var brt_text_container: LinearLayout? = null
    var vol_icon: ImageView? = null
    var brt_icon: ImageView? = null
    var audioManager: AudioManager? = null
    var singleTap: Boolean = false
    private val prevScaleFactor = 1f
    var double_tap_playpause: RelativeLayout? = null
    private var isLongPressSpeedActive = false
    private var speedBeforeLongPress = 1f
    private var speedBadge: TextView? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        videoWindow = window
        WindowCompat.setDecorFitsSystemWindows(videoWindow!!, false)
        videoWindow!!.navigationBarColor = Color.TRANSPARENT
        videoWindow!!.statusBarColor = Color.TRANSPARENT
        setFullScreenImmediately()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        val rootLayout = findViewById<View>(R.id.root_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            rootLayout,
            OnApplyWindowInsetsListener { view: View?, windowInsets: WindowInsetsCompat? ->
                val sys = windowInsets!!.getInsets(WindowInsetsCompat.Type.systemBars())
                val toolbar = view!!.findViewById<View?>(R.id.toolbar)
                if (toolbar != null) {
                    val lp = toolbar.layoutParams as MarginLayoutParams
                    lp.topMargin = sys.top
                    toolbar.layoutParams = lp
                }
                val controls = view.findViewById<View?>(R.id.custom_controls_container)
                if (controls != null) {
                    val lp = controls.layoutParams as MarginLayoutParams
                    lp.bottomMargin = sys.bottom
                    controls.layoutParams = lp
                }
                val videoContainer = view.findViewById<View?>(R.id.video_container)
                videoContainer?.setPadding(0, 0, 0, 0)
                WindowInsetsCompat.CONSUMED
            })

        playerView = findViewById(R.id.exoplayer_view)
        playerView!!.controllerHideOnTouch = false
        playerView!!.controllerAutoShow = false
        playerView!!.controllerShowTimeoutMs = 0
        playerView!!.useController = true

        rotate = findViewById(R.id.rotate)
        audio_track = findViewById(R.id.audio_track)
        position = intent.getIntExtra("position", 0)
        videoTitle = intent.getStringExtra("video_title")
        initViews()
        setupOrientationAwareControls()
        hideSystemUI()
        videoSurfaceView = playerView!!.videoSurfaceView
        val videoUri = intent.data
        if (videoUri != null) {
            playVideoFromUri(videoUri)
        } else {
            val extras = intent.extras
            if (extras != null) {
                mVideoFiles = extras.getParcelableArrayList("videoArrayList")
                if (mVideoFiles != null) {
                    playVideoFromPath()
                }
            }
        }
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        device_width = displayMetrics.widthPixels
        device_height = displayMetrics.heightPixels

        audio_track!!.setOnClickListener { showAudioTrackSelector() }
        audio_track!!.visibility = View.GONE

        rotate!!.setOnClickListener {
            val currentOrientation = resources.configuration.orientation
            requestedOrientation = if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        playerView!!.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onTouch(view: View?, event: MotionEvent): Boolean {
                activePointerCount = event.pointerCount
                scaleGestureDetector!!.onTouchEvent(event)
                gestureDetector!!.onTouchEvent(event)

                if (activePointerCount >= 2) {
                    return true
                }
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        start = true
                        if (event.x < (device_width / 2)) {
                            left = true
                            right = false
                        } else {
                            left = false
                            right = true
                        }
                        baseX = event.x
                        baseY = event.y
                    }

                    MotionEvent.ACTION_MOVE -> {
                        swipe_move = true
                        diffX = ceil((event.x - baseX).toDouble()).toLong()
                        diffY = ceil((event.y - baseY).toDouble()).toLong()
                        val brightnessSpeed = 0.01
                        if (abs(diffY) > MINIMUM_DISTANCE && abs(diffY) > abs(diffX)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (Settings.System.canWrite(applicationContext)) {
                                    if (left) {
                                        videoWindow = window
                                        try {
                                            Settings.System.putInt(
                                                contentResolver,
                                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                                            )
                                            brightness = Settings.System.getInt(
                                                contentResolver,
                                                Settings.System.SCREEN_BRIGHTNESS
                                            )
                                        } catch (e: SettingNotFoundException) {
                                            e.printStackTrace()
                                        }
                                        var new_brightness =
                                            (brightness - (diffY * brightnessSpeed)).toInt()
                                        new_brightness = max(1, min(250, new_brightness))
                                        val brt_percentage =
                                            ceil(((new_brightness.toDouble() / 250.0) * 100.0))
                                        brt_progress_container!!.visibility = View.VISIBLE
                                        brt_text_container!!.visibility = View.VISIBLE
                                        brt_progress!!.progress = brt_percentage.toInt()
                                        brt_icon!!.setImageResource(
                                            when {
                                                brt_percentage < 30 -> R.drawable.ic_brightness_low
                                                brt_percentage < 80 -> R.drawable.ic_brightness_moderate
                                                else -> R.drawable.ic_brightness
                                            }
                                        )
                                        brt_text!!.text = " " + brt_percentage.toInt() + "%"
                                        Settings.System.putInt(
                                            contentResolver,
                                            Settings.System.SCREEN_BRIGHTNESS,
                                            new_brightness
                                        )
                                        val layoutParams = videoWindow!!.attributes
                                        layoutParams.screenBrightness = brightness / 255f
                                        videoWindow!!.attributes = layoutParams
                                    } else {
                                        vol_text_container!!.visibility = View.VISIBLE
                                        mediaVolume =
                                            audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val maxVol =
                                            audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val newMediaVolume = getNewMediaVolume(maxVol, brightnessSpeed)
                                        audioManager!!.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            newMediaVolume,
                                            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                                        )
                                        val volPercentage =
                                            ceil(((newMediaVolume.toDouble() / maxVol.toDouble()) * 100.0))
                                        vol_text!!.text = " " + volPercentage.toInt() + "%"
                                        if (volPercentage < 1) {
                                            vol_icon!!.setImageResource(R.drawable.ic_volume_off)
                                            vol_text!!.text = "OFF"
                                        } else {
                                            vol_icon!!.setImageResource(R.drawable.ic_volume)
                                        }
                                        vol_progress_container!!.visibility = View.VISIBLE
                                        vol_progress!!.progress = volPercentage.toInt()
                                    }
                                    success = true
                                } else if (!writeSettingsPromptShown) {
                                    writeSettingsPromptShown = true
                                    Toast.makeText(
                                        this@VideoPlayerActivity,
                                        "Allow write settings for swipe controls",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                    intent.data = Uri.parse("package:$packageName")
                                    startActivityForResult(intent, 111)
                                }
                            }
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        swipe_move = false
                        start = false
                        deactivateLongPressSpeedIfNeeded()
                        vol_progress_container!!.visibility = View.GONE
                        brt_progress_container!!.visibility = View.GONE
                        vol_text_container!!.visibility = View.GONE
                        brt_text_container!!.visibility = View.GONE
                    }
                }
                return true
            }

            fun getNewMediaVolume(maxVol: Int, brightnessSpeed: Double): Int {
                val cal =
                    diffY.toDouble() * (maxVol.toDouble() / ((device_height * 2).toDouble() - brightnessSpeed))
                var newMediaVolume = mediaVolume - cal.toInt()
                if (newMediaVolume > maxVol) newMediaVolume = maxVol
                else if (newMediaVolume < 1) newMediaVolume = 0
                return newMediaVolume
            }

            override fun onDoubleTouch(event: MotionEvent?) {
                if (player == null) return
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels.toFloat()
                val oneThird = screenWidth / 3
                val x = event?.x

                if (x != null) {
                    when {
                        x < oneThird -> seekBackward()
                        x > oneThird * 2 -> seekForward()
                        else -> togglePlayPause()
                    }
                }
                super.onDoubleTouch(event)
            }

            override fun onSingleTouch() {
                super.onSingleTouch()
                handler.removeCallbacks(hideControlsRunnable)
                if (playerView!!.isControllerFullyVisible) {
                    animateControlsOut()
                    hideSystemUI()
                } else {
                    animateControlsIn()
                    showSystemUI()
                    handler.postDelayed(hideControlsRunnable, 3000)
                }
                if (double_tap_playpause!!.visibility == View.VISIBLE) {
                    double_tap_playpause!!.visibility = View.GONE
                }
            }
        })
        horizontalIconList()
    }

    // ---------------------------------------------------------------
    // Shared audio/subtitle track helpers (Media3 TrackGroup-based)
    // ---------------------------------------------------------------

    private fun collectTracks(trackType: Int, includeTechnicalDetail: Boolean): List<TrackOption> {
        val trackSelector = player?.trackSelector as? DefaultTrackSelector ?: return emptyList()
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return emptyList()
        val tracks = mutableListOf<TrackOption>()

        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            if (mappedTrackInfo.getRendererType(rendererIndex) != trackType) continue
            val trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
            for (groupIndex in 0 until trackGroups.length) {
                val trackGroup = trackGroups[groupIndex]
                for (trackIndex in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(trackIndex)
                    tracks.add(
                        TrackOption(
                            trackGroup,
                            trackIndex,
                            buildTrackName(format, includeTechnicalDetail),
                            format
                        )
                    )
                }
            }
        }
        return tracks
    }

    private fun buildTrackName(format: Format, includeTechnicalDetail: Boolean): String {
        val builder = StringBuilder()
        builder.append(if (format.language != null) languageName(format.language) else "Unknown Language")
        if (!format.label.isNullOrEmpty()) {
            builder.append(" (").append(format.label).append(")")
        }
        if (includeTechnicalDetail) {
            if (format.codecs != null) builder.append(" - ").append(format.codecs)
            if (format.channelCount != Format.NO_VALUE) builder.append(" - ").append(format.channelCount).append("ch")
        }
        return builder.toString()
    }

    private fun languageName(languageCode: String?): String {
        if (languageCode == null) return "Unknown"
        return when (languageCode.lowercase(Locale.getDefault())) {
            "en" -> "English"
            "hi" -> "Hindi"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "zh" -> "Chinese"
            "ar" -> "Arabic"
            "ru" -> "Russian"
            "pt" -> "Portuguese"
            "it" -> "Italian"
            "ta" -> "Tamil"
            "te" -> "Telugu"
            "kn" -> "Kannada"
            "ml" -> "Malayalam"
            "bn" -> "Bengali"
            else -> languageCode.uppercase(Locale.getDefault())
        }
    }

    private fun isTrackSelected(trackSelector: DefaultTrackSelector, track: TrackOption): Boolean {
        val override = trackSelector.parameters.overrides[track.trackGroup] ?: return false
        return override.trackIndices.contains(track.trackIndex)
    }

    private fun selectTrack(trackSelector: DefaultTrackSelector, track: TrackOption, trackType: Int) {
        val newParams = trackSelector.parameters.buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(TrackSelectionOverride(track.trackGroup, track.trackIndex))
            .build()
        trackSelector.parameters = newParams
    }

    private fun disableTrackType(trackSelector: DefaultTrackSelector, trackType: Int) {
        trackSelector.parameters = trackSelector.parameters.buildUpon()
            .setTrackTypeDisabled(trackType, true)
            .build()
    }

    // ---------------------------------------------------------------
    // Audio track selection
    // ---------------------------------------------------------------

    private fun showAudioTrackSelector() {
        val trackSelector = player?.trackSelector as? DefaultTrackSelector
        if (player == null || trackSelector == null) {
            Toast.makeText(this@VideoPlayerActivity, "Player not ready", Toast.LENGTH_SHORT).show()
            return
        }
        val audioTracks = collectTracks(C.TRACK_TYPE_AUDIO, includeTechnicalDetail = true)
        when {
            audioTracks.isEmpty() -> Toast.makeText(this, "No audio tracks found", Toast.LENGTH_SHORT).show()
            audioTracks.size == 1 -> Toast.makeText(this, "Only one audio track available", Toast.LENGTH_SHORT).show()
            else -> showAudioTrackDialog(audioTracks, trackSelector)
        }
    }

    private fun showAudioTrackDialog(audioTracks: List<TrackOption>, trackSelector: DefaultTrackSelector) {
        val trackNames = audioTracks.map { it.name }.toTypedArray()
        val currentSelected = audioTracks.indexOfFirst { isTrackSelected(trackSelector, it) }.coerceAtLeast(0)

        AlertDialog.Builder(this@VideoPlayerActivity)
            .setTitle("Select Audio Language")
            .setSingleChoiceItems(trackNames, currentSelected) { dialog, which ->
                selectTrack(trackSelector, audioTracks[which], C.TRACK_TYPE_AUDIO)
                dialog.dismiss()
                Toast.makeText(this@VideoPlayerActivity, "Switched to: " + audioTracks[which].name, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetZoom() {
        val videoSurface = playerView!!.videoSurfaceView ?: return
        videoSurface.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f).setDuration(200).start()
        scaleFactor = 1f
        translationX = 0f
        translationY = 0f
        isZoomed = false
        updateZoomUI()
    }

    private fun updateZoomUI() {
        if (zoomContainer == null || zoomLevelText == null || resetZoomBtn == null) return
        if (isZoomed) {
            zoomLevelText!!.text = String.format(Locale.US, "%.1fx", scaleFactor)
            zoomContainer!!.visibility = View.VISIBLE
            resetZoomBtn!!.visibility = View.VISIBLE
        } else {
            zoomContainer!!.visibility = View.GONE
            resetZoomBtn!!.visibility = View.GONE
        }
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        if (value < min) return min
        if (value > max) return max
        return value
    }

    private fun showSeekFeedback(text: String?, iconRes: Int) {
        removeSeekFeedback()
        val feedbackLayout = FrameLayout(this@VideoPlayerActivity)
        feedbackLayout.id = View.generateViewId()
        feedbackLayout.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        )
        feedbackLayout.setBackgroundResource(R.drawable.seek_feedback_bg)
        val icon = ImageView(this@VideoPlayerActivity)
        icon.setImageResource(iconRes)
        icon.setColorFilter(Color.WHITE)
        val textView = TextView(this@VideoPlayerActivity)
        textView.text = text
        textView.setTextColor(Color.WHITE)
        textView.textSize = 14f
        textView.gravity = Gravity.CENTER
        val contentLayout = LinearLayout(this@VideoPlayerActivity)
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.gravity = Gravity.CENTER
        contentLayout.addView(icon, LinearLayout.LayoutParams(80, 80))
        contentLayout.addView(textView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        feedbackLayout.addView(contentLayout)
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(feedbackLayout)
        feedbackLayout.alpha = 0f
        feedbackLayout.animate().alpha(1f).setDuration(200).withEndAction {
            Handler().postDelayed({
                feedbackLayout.animate().alpha(0f).setDuration(300).withEndAction { removeSeekFeedback() }.start()
            }, 800)
        }.start()
    }

    // ---------------------------------------------------------------
    // Double-tap ripple, control fade/slide, and long-press 2x speed
    // ---------------------------------------------------------------

    private fun showRippleAt(x: Float, y: Float) {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val sizePx = (140 * resources.displayMetrics.density).toInt()
        val ripple = View(this@VideoPlayerActivity)
        ripple.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(0x33FFFFFF)
        }
        val lp = FrameLayout.LayoutParams(sizePx, sizePx)
        lp.leftMargin = (x - sizePx / 2f).toInt()
        lp.topMargin = (y - sizePx / 2f).toInt()
        ripple.layoutParams = lp
        ripple.scaleX = 0.3f
        ripple.scaleY = 0.3f
        ripple.alpha = 0.9f
        rootView.addView(ripple)
        ripple.animate()
            .scaleX(1.6f)
            .scaleY(1.6f)
            .alpha(0f)
            .setDuration(450)
            .withEndAction { (ripple.parent as? ViewGroup)?.removeView(ripple) }
            .start()
    }

    private fun animateControlsIn() {
        playerView!!.showController()
        val toolbar = findViewById<LinearLayout?>(R.id.toolbar)
        val bottomControls = findViewById<LinearLayout?>(R.id.custom_controls_container)
        toolbar?.let {
            it.alpha = 0f
            it.translationY = -(if (it.height > 0) it.height.toFloat() else 150f)
            it.animate().alpha(1f).translationY(0f).setDuration(220).start()
        }
        bottomControls?.let {
            it.alpha = 0f
            it.translationY = if (it.height > 0) it.height.toFloat() else 200f
            it.animate().alpha(1f).translationY(0f).setDuration(220).start()
        }
    }

    private fun animateControlsOut() {
        val toolbar = findViewById<LinearLayout?>(R.id.toolbar)
        val bottomControls = findViewById<LinearLayout?>(R.id.custom_controls_container)
        toolbar?.animate()
            ?.alpha(0f)
            ?.translationY(-(if (toolbar.height > 0) toolbar.height.toFloat() else 150f))
            ?.setDuration(200)
            ?.start()
        bottomControls?.animate()
            ?.alpha(0f)
            ?.translationY(if (bottomControls.height > 0) bottomControls.height.toFloat() else 200f)
            ?.setDuration(200)
            ?.withEndAction { playerView!!.hideController() }
            ?.start()
        if (bottomControls == null) playerView!!.hideController()
    }

    private fun activateLongPressSpeed() {
        if (player == null || isLongPressSpeedActive) return
        isLongPressSpeedActive = true
        speedBeforeLongPress = player!!.playbackParameters.speed
        player!!.playbackParameters = PlaybackParameters(2f)
        showLongPressSpeedBadge(true)
    }

    private fun deactivateLongPressSpeedIfNeeded() {
        if (!isLongPressSpeedActive) return
        isLongPressSpeedActive = false
        player?.playbackParameters = PlaybackParameters(speedBeforeLongPress)
        showLongPressSpeedBadge(false)
    }

    private fun showLongPressSpeedBadge(show: Boolean) {
        if (speedBadge == null) {
            val badge = TextView(this@VideoPlayerActivity)
            badge.text = "2x speed"
            badge.setTextColor(Color.WHITE)
            badge.textSize = 14f
            badge.setPadding(24, 12, 24, 12)
            badge.setBackgroundResource(R.drawable.seek_feedback_bg)
            val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            lp.topMargin = (90 * resources.displayMetrics.density).toInt()
            badge.layoutParams = lp
            badge.visibility = View.GONE
            findViewById<ViewGroup>(android.R.id.content).addView(badge)
            speedBadge = badge
        }
        speedBadge!!.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun seekBackward() {
        val currentPosition = player!!.currentPosition
        var newPosition = currentPosition - 10000
        if (newPosition < 0) newPosition = 0
        player!!.seekTo(newPosition)
        showSeekFeedback("-10s", R.drawable.ic_rewind)
    }

    fun seekForward() {
        val currentPosition = player!!.currentPosition
        val duration = player!!.duration
        var newPosition = currentPosition + 10000
        if (newPosition > duration) newPosition = duration
        player!!.seekTo(newPosition)
        showSeekFeedback("+10s", R.drawable.ic_forward)
    }

    private fun handlePlayPauseState(isPlaying: Boolean) {
        playButton?.visibility = if (isPlaying) View.GONE else View.VISIBLE
        pauseButton?.visibility = if (isPlaying) View.VISIBLE else View.GONE
        if (!isPlaying) {
            animateControlsIn()
            showSystemUI()
            handler.removeCallbacks(hideControlsRunnable)
        } else {
            handler.postDelayed(hideControlsRunnable, 3000)
        }
    }

    fun togglePlayPause() {
        val isPlaying = player!!.playWhenReady
        player!!.playWhenReady = !isPlaying
        handlePlayPauseState(!isPlaying)
        showPlayPauseFeedback(!isPlaying)
    }

    private fun showPlayPauseFeedback(isPlaying: Boolean) {
        removeSeekFeedback()
        val feedbackLayout = FrameLayout(this@VideoPlayerActivity)
        feedbackLayout.id = View.generateViewId()
        feedbackLayout.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        )
        feedbackLayout.setBackgroundResource(R.drawable.seek_feedback_bg)

        val icon = ImageView(this@VideoPlayerActivity)
        icon.setImageResource(if (isPlaying) R.drawable.ic_play else R.drawable.ic_pause)
        icon.setColorFilter(Color.WHITE)
        icon.layoutParams = FrameLayout.LayoutParams(120, 120, Gravity.CENTER)

        val textView = TextView(this@VideoPlayerActivity)
        textView.text = if (isPlaying) "Playing" else "Paused"
        textView.setTextColor(Color.WHITE)
        textView.textSize = 16f
        textView.gravity = Gravity.CENTER
        textView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        )

        val contentLayout = LinearLayout(this@VideoPlayerActivity)
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.gravity = Gravity.CENTER
        contentLayout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        contentLayout.addView(icon)
        contentLayout.addView(textView)
        feedbackLayout.addView(contentLayout)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(feedbackLayout)
        feedbackLayout.alpha = 0f
        feedbackLayout.animate().alpha(1f).setDuration(200).withEndAction {
            Handler().postDelayed({
                feedbackLayout.animate().alpha(0f).setDuration(300).withEndAction {
                    (feedbackLayout.parent as? ViewGroup)?.removeView(feedbackLayout)
                }.start()
            }, 800)
        }.start()
    }

    private fun removeSeekFeedback() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        for (i in 0 until rootView.childCount) {
            val child = rootView.getChildAt(i)
            if (child is FrameLayout && child.id != R.id.controller_root) {
                rootView.removeView(child)
            }
        }
    }

    private fun hideSystemUI() {
        val orientation = resources.configuration.orientation
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun showSystemUI() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.show(WindowInsetsCompat.Type.navigationBars())
    }

    private fun horizontalIconList() {
        iconModelsArrayList.add(IconModel(R.drawable.ic_subtitle, "Subtitle"))
        iconModelsArrayList.add(IconModel(R.drawable.baseline_audiotrack_24, "Audio"))
        iconModelsArrayList.add(IconModel(R.drawable.ic_speed, "Speed"))
        iconModelsArrayList.add(IconModel(R.drawable.ic_equalizer, "Equalizer"))
        iconModelsArrayList.add(IconModel(R.drawable.ic_right, ""))

        playbackIconsAdapter = PlaybackIconsAdapter(iconModelsArrayList, this)
        val layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        recyclerViewIcons!!.layoutManager = layoutManager
        recyclerViewIcons!!.adapter = playbackIconsAdapter
        playbackIconsAdapter!!.notifyDataSetChanged()
        playbackIconsAdapter!!.setOnItemClickListener(object : PlaybackIconsAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                handler.removeCallbacks(hideControlsRunnable)

                if ((!expand && position == 4) || (expand && position == 10)) {
                    if (expand) {
                        iconModelsArrayList.clear()
                        iconModelsArrayList.add(IconModel(R.drawable.ic_subtitle, "Subtitle"))
                        iconModelsArrayList.add(IconModel(R.drawable.baseline_audiotrack_24, "Audio"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_speed, "Speed"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_equalizer, "Equalizer"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_right, ""))
                        playbackIconsAdapter!!.notifyDataSetChanged()
                        expand = false
                        handler.postDelayed(hideControlsRunnable, 3000)
                    } else {
                        iconModelsArrayList.clear()
                        iconModelsArrayList.add(IconModel(R.drawable.ic_subtitle, "Subtitle"))
                        iconModelsArrayList.add(IconModel(R.drawable.baseline_audiotrack_24, "Audio"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_speed, "Speed"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_equalizer, "Equalizer"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_pip_mode, "Pop-Up"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_night_mode, "Night"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_rotate, "Rotate"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_volume_off, "Mute"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_volume, "Volume"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_brightness, "Brightness"))
                        iconModelsArrayList.add(IconModel(R.drawable.ic_left, ""))
                        playbackIconsAdapter!!.notifyDataSetChanged()
                        expand = true
                    }
                } else if (position == 0) {
                    showUnifiedSubtitleDialog()
                } else if (position == 1) {
                    showAudioTrackSelector()
                } else if (position == 2) {
                    showSpeedDialog()
                } else if (position == 3) {
                    showEqualizer()
                } else if (expand && position == 4) {
                    enterPipMode()
                } else if (expand && position == 5) {
                    toggleNightMode()
                } else if (expand && position == 6) {
                    toggleRotation()
                } else if (expand && position == 7) {
                    toggleMute()
                } else if (expand && position == 8) {
                    showVolumeDialog()
                } else if (expand && position == 9) {
                    showBrightnessDialog()
                }
            }
        })
    }

    private fun showSpeedDialog() {
        val alertDialog = AlertDialog.Builder(this@VideoPlayerActivity)
        alertDialog.setTitle("Select Playback Speed").setPositiveButton("Ok", null)
        val items = arrayOf("0.5x", "1x", "1.25x", "1.5x", "2x")
        alertDialog.setSingleChoiceItems(items, -1) { _, i ->
            speed = when (i) {
                0 -> 0.5f
                1 -> 1f
                2 -> 1.25f
                3 -> 1.5f
                4 -> 2f
                else -> speed
            }
            parameters = PlaybackParameters(speed)
            player!!.playbackParameters = parameters!!
            handler.postDelayed(hideControlsRunnable, 3000)
        }
        alertDialog.show()
    }

    private fun showEqualizer() {
        if (eqContainer!!.visibility == View.GONE) {
            eqContainer!!.visibility = View.VISIBLE
        }
        val sessionId = player!!.audioSessionId
        com.bullhead.equalizer.Settings.isEditing = false
        val equalizerFragment = EqualizerFragment.newBuilder()
            .setAccentColor(Color.parseColor("#1A78F2"))
            .setAudioSessionId(sessionId)
            .build()
        supportFragmentManager.beginTransaction().replace(R.id.eqFrame, equalizerFragment).commit()
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                var aspectRatio = Rational(16, 9)
                val videoFormat = player?.videoFormat
                if (videoFormat != null && videoFormat.width > 0 && videoFormat.height > 0) {
                    aspectRatio = Rational(videoFormat.width, videoFormat.height)
                }
                pictureInPicture!!.setAspectRatio(aspectRatio)

                val actions = ArrayList<RemoteAction?>()
                val actionIntent = Intent(this, VideoPlayerActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE)
                val icon = Icon.createWithResource(this, R.drawable.ic_pip_mode)
                val remoteAction = RemoteAction(icon, "Play/Pause", "Toggle play/pause", pendingIntent)
                actions.add(remoteAction)

                pictureInPicture!!.setActions(actions)
                enterPictureInPictureMode(pictureInPicture!!.build())
            } catch (e: Exception) {
                Toast.makeText(this, "PIP not supported on this device", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "PIP requires Android 8.0+", Toast.LENGTH_SHORT).show()
        }
        handler.postDelayed(hideControlsRunnable, 3000)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isCrossChecked = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            animateControlsOut()
            hideSystemUI()
        } else {
            animateControlsIn()
            showSystemUI()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode) {
            enterPipMode()
        }
    }

    private fun toggleNightMode() {
        if (dark) {
            nightMode!!.visibility = View.GONE
            iconModelsArrayList[5] = IconModel(R.drawable.ic_night_mode, "Night")
            dark = false
        } else {
            nightMode!!.visibility = View.VISIBLE
            iconModelsArrayList[5] = IconModel(R.drawable.ic_day, "Day")
            dark = true
        }
        playbackIconsAdapter!!.notifyDataSetChanged()
        handler.postDelayed(hideControlsRunnable, 3000)
    }

    private fun toggleRotation() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        handler.postDelayed(hideControlsRunnable, 3000)
    }

    private fun toggleMute() {
        if (mute) {
            player!!.volume = 1f
            iconModelsArrayList[7] = IconModel(R.drawable.ic_volume_off, "Mute")
            mute = false
        } else {
            player!!.volume = 0f
            iconModelsArrayList[7] = IconModel(R.drawable.ic_volume, "Un-mute")
            mute = true
        }
        playbackIconsAdapter!!.notifyDataSetChanged()
        handler.postDelayed(hideControlsRunnable, 3000)
    }

    private fun showVolumeDialog() {
        val volumeDialog = VolumeDialog()
        volumeDialog.show(supportFragmentManager, "dialog")
        handler.postDelayed(hideControlsRunnable, 3000)
    }

    private fun showBrightnessDialog() {
        val brightnessDialog = BrightnessDialog()
        brightnessDialog.show(supportFragmentManager, "dialog")
        handler.postDelayed(hideControlsRunnable, 3000)
    }

    private fun initViews() {
        nextButton = findViewById(R.id.exo_next)
        previousButton = findViewById(R.id.exo_prev)
        playButton = findViewById(R.id.exo_play)
        pauseButton = findViewById(R.id.exo_pause)
        playButton!!.setOnClickListener { togglePlayPause() }
        pauseButton!!.setOnClickListener { togglePlayPause() }
        // Start in a known state; handlePlayPauseState() takes over once playback begins.
        playButton!!.visibility = View.VISIBLE
        pauseButton!!.visibility = View.GONE
        videoBack = findViewById(R.id.video_back)
        lock = findViewById(R.id.lock)
        unlock = findViewById(R.id.unlock)
        scaling = findViewById(R.id.scaling)
        root = findViewById(R.id.controller_root)
        nightMode = findViewById(R.id.night_mode)
        videoList = findViewById(R.id.video_list)
        videoMore = findViewById(R.id.video_more)
        recyclerViewIcons = findViewById(R.id.recyclerview_icon)
        eqContainer = findViewById(R.id.eqFrame)
        vol_text = findViewById(R.id.vol_text)
        brt_text = findViewById(R.id.brt_text)
        vol_progress = findViewById(R.id.vol_progress)
        brt_progress = findViewById(R.id.brt_progress)
        brt_progress_container = findViewById(R.id.brt_progress_container)
        vol_progress_container = findViewById(R.id.vol_progress_container)
        brt_text_container = findViewById(R.id.brt_text_container)
        vol_text_container = findViewById(R.id.vol_text_container)
        vol_icon = findViewById(R.id.vol_icon)
        brt_icon = findViewById(R.id.brt_icon)
        double_tap_playpause = findViewById(R.id.double_tap_play_pause)

        zoomContainer = findViewById(R.id.zoom_container)
        zoomLevelText = findViewById(R.id.zoom_level_text)
        resetZoomBtn = findViewById(R.id.reset_zoom_btn)
        resetZoomBtn!!.setOnClickListener { resetZoom() }
        zoomContainer!!.visibility = View.GONE
        zoomLevelText!!.text = "1.0x"
        resetZoomBtn!!.visibility = View.GONE

        scaleGestureDetector = ScaleGestureDetector(this, object : SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                scaleFactor *= factor
                scaleFactor = max(1f, min(scaleFactor, 4f))
                val focusX = detector.focusX
                val focusY = detector.focusY
                translationX += (focusX - playerView!!.width / 2f) * (1 - prevScaleFactor / scaleFactor)
                translationY += (focusY - playerView!!.height / 2f) * (1 - prevScaleFactor / scaleFactor)

                if (scaleFactor <= 1f + 1e-3) {
                    scaleFactor = 1f
                    translationX = 0f
                    translationY = 0f
                }
                isZoomed = scaleFactor > 1.02f
                applyTransform()
                updateZoomUI()
                return true
            }
        })
        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleControls()
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distX: Float, distY: Float): Boolean {
                if (!isZoomed || activePointerCount < 2) return false
                translationX -= distX
                translationY -= distY
                applyTransform()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val x = e.x
                val dm = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(dm)
                val width = dm.widthPixels.toFloat()
                showRippleAt(e.x, e.y)
                when {
                    x < width / 3 -> seekBackward()
                    x > width * 2 / 3 -> seekForward()
                    else -> togglePlayPause()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (activePointerCount < 2) {
                    activateLongPressSpeed()
                }
            }
        })
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        title = findViewById(R.id.video_title)
        title!!.text = videoTitle
        videoContainer = findViewById(R.id.video_container)

        nextButton!!.setOnClickListener(this)
        previousButton!!.setOnClickListener(this)
        videoBack!!.setOnClickListener(this)
        lock!!.setOnClickListener(this)
        unlock!!.setOnClickListener(this)
        videoList!!.setOnClickListener(this)
        videoMore!!.setOnClickListener(this)
        scaling!!.setOnClickListener(firstListener)

        val extras = intent.extras
        if (extras != null) {
            mVideoFiles = extras.getParcelableArrayList("videoArrayList")
        }
        dialogProperties = DialogProperties()
        filePickerDialog = FilePickerDialog(this@VideoPlayerActivity)
        filePickerDialog!!.setTitle("Select a subtitle file")
        filePickerDialog!!.setPositiveBtnName("OK")
        filePickerDialog!!.setNegativeBtnName("Cancel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPicture = PictureInPictureParams.Builder()
        }
    }

    private fun toggleControls() {
        handler.removeCallbacks(hideControlsRunnable)
        if (playerView!!.isControllerFullyVisible) {
            animateControlsOut()
            hideSystemUI()
        } else {
            animateControlsIn()
            showSystemUI()
            handler.postDelayed(hideControlsRunnable, 3000)
        }
    }

    private fun applyTransform() {
        val videoSurface = playerView!!.videoSurfaceView ?: return
        val containerWidth = playerView!!.width.toFloat()
        val containerHeight = playerView!!.height.toFloat()
        if (containerWidth == 0f || containerHeight == 0f) return
        videoSurface.pivotX = containerWidth / 2f
        videoSurface.pivotY = containerHeight / 2f
        val maxTransX = max(0f, (scaleFactor - 1f) * containerWidth / 2f)
        val maxTransY = max(0f, (scaleFactor - 1f) * containerHeight / 2f)
        translationX = clamp(translationX, -maxTransX, maxTransX)
        translationY = clamp(translationY, -maxTransY, maxTransY)
        videoSurface.scaleX = scaleFactor
        videoSurface.scaleY = scaleFactor
        videoSurface.translationX = translationX
        videoSurface.translationY = translationY
        val videoContainer = findViewById<FrameLayout?>(R.id.video_container)
        if (videoContainer != null) {
            videoContainer.setPadding(0, 0, 0, 0)
            videoContainer.scaleX = 1f
            videoContainer.scaleY = 1f
            videoContainer.translationX = 0f
            videoContainer.translationY = 0f
        }
        playerView!!.scaleX = 1f
        playerView!!.scaleY = 1f
        playerView!!.translationX = 0f
        playerView!!.translationY = 0f
        isZoomed = scaleFactor > 1.02f
        updateZoomUI()
    }

    private fun buildDataSourceFactory() = DefaultDataSource.Factory(this)

    private fun playVideoFromPath() {
        player = ExoPlayer.Builder(this).build()
        val dataSourceFactory = buildDataSourceFactory()
        concatenatingMediaSource = ConcatenatingMediaSource()

        mVideoFiles?.let { files ->
            for (i in files.indices) {
                val path = files[i]!!.path
                val uri = Uri.parse(path)
                val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
                concatenatingMediaSource!!.addMediaSource(mediaSource)
            }
        }
        playerView!!.player = player
        playerView!!.keepScreenOn = true
        player!!.playbackParameters = parameters ?: PlaybackParameters(1f)
        player!!.setMediaSource(concatenatingMediaSource!!)
        player!!.prepare()
        player!!.seekTo(position, C.TIME_UNSET)
        player!!.playWhenReady = true
        animateControlsIn()
        showSystemUI()
        handler.postDelayed(hideControlsRunnable, 3000)
        playError()
    }

    private fun playVideoFromUri(uri: Uri) {
        val dataSourceFactory = buildDataSourceFactory()
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))

        player = ExoPlayer.Builder(this).build()
        playerView!!.player = player
        player!!.setMediaSource(mediaSource)
        player!!.prepare()
        player!!.seekTo(position, C.TIME_UNSET)
        player!!.playWhenReady = true
        animateControlsIn()
        showSystemUI()
        handler.postDelayed(hideControlsRunnable, 3000)
        playError()
    }

    // Now uses MediaItem.SubtitleConfiguration instead of a manual
    // SingleSampleMediaSource + MergingMediaSource pair — same result, less code.
    private fun playVideoSubtitle(subtitle: Uri?) {
        val oldPosition = player!!.currentPosition
        player!!.stop()

        val path = mVideoFiles!![position]!!.path
        val videoUri = Uri.parse(path)
        player = ExoPlayer.Builder(this).build()
        val dataSourceFactory = buildDataSourceFactory()

        val mediaItemBuilder = MediaItem.Builder().setUri(videoUri)
        if (subtitle != null) {
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subtitle)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("und")
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        }
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItemBuilder.build())

        playerView!!.player = player
        playerView!!.keepScreenOn = true
        player!!.playbackParameters = parameters ?: PlaybackParameters(1f)
        player!!.setMediaSource(mediaSource)
        player!!.prepare()
        player!!.seekTo(position, oldPosition)
        playError()
    }

    private fun playError() {
        player!!.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(this@VideoPlayerActivity, "Video Playing Error", Toast.LENGTH_SHORT).show()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    handlePlayPauseState(player!!.playWhenReady)
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                handlePlayPauseState(playWhenReady)
            }
        })
        player!!.playWhenReady = true
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val fragment = supportFragmentManager.findFragmentById(R.id.eqFrame)
            if (eqContainer!!.visibility == View.GONE) {
                finishThroughDispatcher()
            } else {
                if (fragment != null && fragment.isVisible && eqContainer!!.visibility == View.VISIBLE) {
                    eqContainer!!.visibility = View.GONE
                } else {
                    player?.release()
                    finishThroughDispatcher()
                }
            }
        }

        private fun finishThroughDispatcher() {
            // Temporarily disable this callback so the dispatcher falls through
            // to the default back behavior (finishing the Activity) instead of
            // calling straight back into this same callback.
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun onPause() {
        super.onPause()
        player!!.playWhenReady = isInPictureInPictureMode
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        player!!.playWhenReady = true
    }

    override fun onRestart() {
        super.onRestart()
        player!!.playWhenReady = true
    }

    override fun onClick(v: View) {
        if (v.id == R.id.video_back) {
            player?.release()
            finish()
        } else if (v.id == R.id.lock) {
            root!!.visibility = View.VISIBLE
            lock!!.visibility = View.VISIBLE
            Toast.makeText(this, "Unlocked", Toast.LENGTH_SHORT).show()
        } else if (v.id == R.id.unlock) {
            root!!.visibility = View.INVISIBLE
            lock!!.visibility = View.VISIBLE
            Toast.makeText(this, "Locked", Toast.LENGTH_SHORT).show()
        } else if (v.id == R.id.video_list) {
            val playlistDialog = PlaylistDialog(mVideoFiles, videoFilesAdapter!!)
            playlistDialog.show(supportFragmentManager, playlistDialog.tag)
        } else if (v.id == R.id.exo_next) {
            try {
                player!!.stop()
                position++
                playVideoFromPath()
                title!!.text = mVideoFiles!![position]!!.displayName
            } catch (e: Exception) {
                Toast.makeText(this, "No next video", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (v.id == R.id.exo_prev) {
            try {
                player!!.stop()
                position--
                playVideoFromPath()
                title!!.text = mVideoFiles!![position]!!.displayName
            } catch (e: Exception) {
                Toast.makeText(this, "No previous video", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (v.id == R.id.video_more) {
            val popupMenu = PopupMenu(this@VideoPlayerActivity, videoMore)
            popupMenu.inflate(R.menu.player_menu)
            popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
                when (menuItem.itemId) {
                    R.id.next -> nextButton!!.performClick()
                    R.id.send -> {
                        val videoUri = intent.data
                        val uri: Uri? = videoUri ?: mVideoFiles?.let { Uri.parse(it[position]!!.path) }
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "video/*"
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                        startActivity(Intent.createChooser(shareIntent, "Share video via?"))
                    }
                    R.id.properties -> showPropertiesDialog()
                    R.id.delete -> showDeleteDialog()
                    R.id.subtitle -> showUnifiedSubtitleDialog()
                }
                true
            }
            popupMenu.show()
        }
    }

    private fun showPropertiesDialog() {
        val alertDialog = AlertDialog.Builder(this@VideoPlayerActivity)
        alertDialog.setTitle("Properties")
        val file = mVideoFiles!![position]!!
        val one = "File: " + file.displayName
        val path = file.path
        val indexOfPath = path?.lastIndexOf("/")
        val two = "Path: " + path?.substring(0, indexOfPath!!)
        val three = "Size: " + Formatter.formatFileSize(this@VideoPlayerActivity,
            file.size?.toLong() ?: return
        )
        val nameWithFormat = file.displayName
        val index = nameWithFormat?.lastIndexOf(".")
        val format = nameWithFormat?.substring(index?.plus(1) ?: 1)
        val five = "Format: $format"
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(file.path)
        val height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val six = "Resolution: " + width + "x" + height
        alertDialog.setMessage("$one\n\n$two\n\n$three\n\n$five\n\n$six")
        alertDialog.setPositiveButton("OK") { dialogInterface, _ -> dialogInterface.dismiss() }
        alertDialog.show()
    }

    private fun showDeleteDialog() {
        val alertDialogDelete = AlertDialog.Builder(this@VideoPlayerActivity)
        alertDialogDelete.setTitle("Delete")
        alertDialogDelete.setMessage("Do you want to delete this video?")
        alertDialogDelete.setPositiveButton("Delete") { _, _ ->
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                mVideoFiles!![position]!!.id?.toLong() ?: return@setPositiveButton
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uriList = ArrayList<Uri?>()
                uriList.add(contentUri)
                val deleteIntent = MediaStore.createDeleteRequest(contentResolver, uriList)
                try {
                    startIntentSenderForResult(deleteIntent.intentSender, 108, null, 0, 0, 0)
                } catch (e: SendIntentException) {
                    e.printStackTrace()
                }
            } else {
                val rowsDeleted = contentResolver.delete(contentUri, null, null)
                if (rowsDeleted > 0) {
                    mVideoFiles!!.removeAt(position)
                    videoFilesAdapter!!.notifyItemRemoved(position)
                    nextButton!!.performClick()
                    Toast.makeText(this@VideoPlayerActivity, "Video deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@VideoPlayerActivity, "Video is not deleted", Toast.LENGTH_SHORT).show()
                }
            }
        }
        alertDialogDelete.setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
        alertDialogDelete.show()
    }

    // ---------------------------------------------------------------
    // Subtitle selection
    // ---------------------------------------------------------------

    private fun showUnifiedSubtitleDialog() {
        val trackSelector = player?.trackSelector as? DefaultTrackSelector
        if (player == null || trackSelector == null) {
            Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val subtitleOptions = mutableListOf(
            SubtitleOption(SubtitleOption.TYPE_NONE, null, "No Subtitle"),
            SubtitleOption(SubtitleOption.TYPE_EXTERNAL, null, "Select from Device")
        )
        val embeddedTracks = collectTracks(C.TRACK_TYPE_TEXT, includeTechnicalDetail = false)
        embeddedTracks.forEach { subtitleOptions.add(SubtitleOption(SubtitleOption.TYPE_EMBEDDED, it, it.name)) }

        showSubtitleSelectionDialog(subtitleOptions, trackSelector)
    }

    private fun showSubtitleSelectionDialog(subtitleOptions: List<SubtitleOption>, trackSelector: DefaultTrackSelector) {
        val options = subtitleOptions.map { it.name }.toTypedArray()
        val currentlySelectedIndex = findCurrentlySelectedSubtitleIndex(subtitleOptions, trackSelector)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Subtitle")
        builder.setSingleChoiceItems(options, currentlySelectedIndex) { dialog, which ->
            val selectedOption = subtitleOptions[which]
            when (selectedOption.type) {
                SubtitleOption.TYPE_NONE -> {
                    disableTrackType(trackSelector, C.TRACK_TYPE_TEXT)
                    uriSubtitle = null
                    Toast.makeText(this@VideoPlayerActivity, "Subtitles disabled", Toast.LENGTH_SHORT).show()
                }
                SubtitleOption.TYPE_EXTERNAL -> showExternalSubtitlePicker()
                SubtitleOption.TYPE_EMBEDDED -> {
                    selectTrack(trackSelector, selectedOption.track!!, C.TRACK_TYPE_TEXT)
                    uriSubtitle = null
                    Toast.makeText(this@VideoPlayerActivity, "Subtitle: " + selectedOption.name, Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
            handler.postDelayed(hideControlsRunnable, 3000)
        }
        builder.setNegativeButton("Cancel", null)
        builder.create().show()
    }

    private fun findCurrentlySelectedSubtitleIndex(subtitleOptions: List<SubtitleOption>, trackSelector: DefaultTrackSelector): Int {
        if (uriSubtitle != null) {
            val externalIndex = subtitleOptions.indexOfFirst { it.type == SubtitleOption.TYPE_EXTERNAL }
            if (externalIndex >= 0) return externalIndex
        }
        val embeddedIndex = subtitleOptions.indexOfFirst {
            it.type == SubtitleOption.TYPE_EMBEDDED && isTrackSelected(trackSelector, it.track!!)
        }
        return if (embeddedIndex >= 0) embeddedIndex else 0
    }

    private fun showExternalSubtitlePicker() {
        dialogProperties!!.selection_mode = DialogConfigs.SINGLE_MODE
        dialogProperties!!.extensions = arrayOf(".srt", ".vtt", ".ass", ".ssa", ".sub")
        dialogProperties!!.root = File("/storage/emulated/0")
        filePickerDialog!!.setProperties(dialogProperties)
        filePickerDialog!!.show()
        filePickerDialog!!.setDialogSelectionListener(DialogSelectionListener { files: Array<String>? ->
            if (!files.isNullOrEmpty()) {
                val path = files[0]
                val file = File(path)
                uriSubtitle = Uri.parse(file.absolutePath)
                (player?.trackSelector as? DefaultTrackSelector)?.let { disableTrackType(it, C.TRACK_TYPE_TEXT) }
                playVideoSubtitle(uriSubtitle)
                Toast.makeText(this@VideoPlayerActivity, "Loaded: " + file.name, Toast.LENGTH_SHORT).show()
            }
        })
    }

    var firstListener: View.OnClickListener = View.OnClickListener {
        playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        scaling!!.setImageResource(R.drawable.fullscreen)
        Toast.makeText(this@VideoPlayerActivity, "Full Screen", Toast.LENGTH_SHORT).show()
        scaling!!.setOnClickListener(secondListener)
    }
    var secondListener: View.OnClickListener = View.OnClickListener {
        playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        scaling!!.setImageResource(R.drawable.zoom)
        Toast.makeText(this@VideoPlayerActivity, "Zoom", Toast.LENGTH_SHORT).show()
        scaling!!.setOnClickListener(thirdListener)
    }
    var thirdListener: View.OnClickListener = View.OnClickListener {
        playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        scaling!!.setImageResource(R.drawable.fit)
        Toast.makeText(this@VideoPlayerActivity, "Fit", Toast.LENGTH_SHORT).show()
        scaling!!.setOnClickListener(firstListener)
    }

    override fun onStop() {
        super.onStop()
        if (isCrossChecked) {
            player!!.release()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111) {
            writeSettingsPromptShown = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(applicationContext)) {
                    success = true
                } else {
                    Toast.makeText(this, "Write Permission not granted to this app", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (requestCode == 108) {
            if (resultCode == RESULT_OK) {
                mVideoFiles!!.removeAt(position)
                videoFilesAdapter!!.notifyItemRemoved(position)
                nextButton!!.performClick()
                Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Delete cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustControlsForOrientation()
        if (singleTap) showSystemUI() else hideSystemUI()
    }

    private fun setFullScreenImmediately() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            val decorView = window.decorView
            val uiOptions = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            decorView.systemUiVisibility = uiOptions
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun setupOrientationAwareControls() {
        adjustControlsForOrientation()
        window.decorView.setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener {
            adjustControlsForOrientation()
        })
    }

    private fun adjustControlsForOrientation() {
        if (root == null) return
        val orientation = resources.configuration.orientation

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            applyMarginsToView(root!!, 90, 0, 90, 0)
            findViewById<LinearLayout?>(R.id.custom_controls_container)?.let { applyMarginsToView(it, 80, 0, 80, 0) }
        } else {
            applyMarginsToView(root!!, 0, 0, 0, 0)
            findViewById<LinearLayout?>(R.id.custom_controls_container)?.let { applyMarginsToView(it, 0, 0, 0, 0) }
            applyPortraitSystemUIInsets()
        }
    }

    private fun applyPortraitSystemUIInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root!!, OnApplyWindowInsetsListener { _: View?, insets: WindowInsetsCompat? ->
            val systemBars = insets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            applyMarginsToView(root!!, 0, 0, 0, systemBars.bottom)
            findViewById<LinearLayout?>(R.id.custom_controls_container)?.let {
                applyMarginsToView(it, 0, 0, 0, systemBars.bottom)
            }
            WindowInsetsCompat.CONSUMED
        })
        ViewCompat.requestApplyInsets(root!!)
    }

    private fun applyMarginsToView(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        val params = view.layoutParams as MarginLayoutParams?
        if (params != null) {
            params.leftMargin = left
            params.topMargin = top
            params.rightMargin = right
            params.bottomMargin = bottom
            view.layoutParams = params
        }
    }

    companion object {
        const val MINIMUM_DISTANCE: Int = 100
    }
}