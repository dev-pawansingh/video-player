package com.pawansingh.vp.activities;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bullhead.equalizer.EqualizerFragment;
import com.bullhead.equalizer.Settings;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.pawansingh.vp.R;
import com.pawansingh.vp.adapters.PlaybackIconsAdapter;
import com.pawansingh.vp.adapters.VideoFilesAdapter;
import com.pawansingh.vp.models.BrightnessDialog;
import com.pawansingh.vp.models.IconModel;
import com.pawansingh.vp.models.MediaFiles;
import com.pawansingh.vp.models.OnSwipeTouchListener;
import com.pawansingh.vp.models.PlaylistDialog;
import com.pawansingh.vp.models.VolumeDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity implements View.OnClickListener {

    ArrayList<MediaFiles> mVideoFiles = new ArrayList<>();
    PlayerView playerView;
    SimpleExoPlayer player;
    int position;
    String videoTitle;
    TextView title;
    Window window;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 1f;
    private float translationX = 0f, translationY = 0f;
    private boolean isZoomed = false;
    private int activePointerCount = 0;
    private View videoSurfaceView;

    private Handler handler = new Handler();
    private Runnable hideControlsRunnable = new Runnable() {
        @Override
        public void run() {
            if (playerView.isControllerVisible()) {
                playerView.hideController();
                hideSystemUI();
            }
        }
    };

    ImageView videoBack, lock, unlock, scaling, videoList, videoMore, rotate, audio_track;
    VideoFilesAdapter videoFilesAdapter;
    RelativeLayout root;
    LinearLayout zoomContainer;
    TextView zoomLevelText;
    ImageButton resetZoomBtn;
    ImageView nextButton, previousButton;
    ConcatenatingMediaSource concatenatingMediaSource;
    FrameLayout videoContainer;
    // Horizontal recyclerview variables
    private final ArrayList<IconModel> iconModelsArrayList = new ArrayList<>();
    PlaybackIconsAdapter playbackIconsAdapter;
    RecyclerView recyclerViewIcons;
    boolean expand = false;
    View nightMode;
    boolean dark = false;
    boolean mute = false;
    PlaybackParameters parameters;
    float speed;
    DialogProperties dialogProperties;
    FilePickerDialog filePickerDialog;
    Uri uriSubtitle;
    PictureInPictureParams.Builder pictureInPicture;
    boolean isCrossChecked;
    FrameLayout eqContainer;
    // Swipe and zoom Variables
    private int device_height, device_width, brightness, mediaVolume;
    boolean start = false;
    boolean left, right;
    private float baseX, baseY;
    boolean swipe_move = false;
    long diffX, diffY;
    public static final int MINIMUM_DISTANCE = 100;
    boolean success = false;
    TextView vol_text, brt_text;
    ProgressBar vol_progress, brt_progress;
    LinearLayout vol_progress_container, vol_text_container, brt_progress_container, brt_text_container;
    ImageView vol_icon, brt_icon;
    AudioManager audioManager;
    private ContentResolver contentResolver;
    boolean singleTap = false;
    private float prevScaleFactor = 1f;
    RelativeLayout double_tap_playpause;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setStatusBarColor(Color.TRANSPARENT);
        setFullScreenImmediately();
        View rootLayout = findViewById(R.id.root_main);

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (view, windowInsets) -> {
            Insets sys = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            View toolbar = view.findViewById(R.id.toolbar);
            if (toolbar != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                lp.topMargin = sys.top; // push toolbar below status bar
                toolbar.setLayoutParams(lp);
            }
            // Apply bottom inset to the playback controls so they sit above nav bar
            View controls = view.findViewById(R.id.custom_controls_container);
            if (controls != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) controls.getLayoutParams();
                lp.bottomMargin = sys.bottom; // keep controls above nav bar
                controls.setLayoutParams(lp);
            }
            View videoContainer = view.findViewById(R.id.video_container);
            if (videoContainer != null) {
                videoContainer.setPadding(0, 0, 0, 0);
            }
            return WindowInsetsCompat.CONSUMED;
        });

        playerView = findViewById(R.id.exoplayer_view);
        playerView.setControllerHideOnTouch(false);
        playerView.setControllerAutoShow(false);
        playerView.setControllerShowTimeoutMs(0);
        playerView.setUseController(true);

        rotate = findViewById(R.id.rotate);
        audio_track = findViewById(R.id.audio_track);
        position = getIntent().getIntExtra("position", 0);
        videoTitle = getIntent().getStringExtra("video_title");
        initViews();
        setupOrientationAwareControls();
        hideSystemUI();
        videoSurfaceView = playerView.getVideoSurfaceView();
        Uri videoUri = getIntent().getData();
        if (videoUri != null) {
            playVideoFromUri(videoUri);
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mVideoFiles = extras.getParcelableArrayList("videoArrayList");
                if (mVideoFiles != null) {
                    playVideoFromPath();
                }
            }
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        device_width = displayMetrics.widthPixels;
        device_height = displayMetrics.heightPixels;

        audio_track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {showAudioTrackSelector();}

            private void showAudioTrackSelector() {
                if (player == null) {
                    Toast.makeText(VideoPlayerActivity.this, "Player not ready", Toast.LENGTH_SHORT).show();
                    return;
                }

                DefaultTrackSelector trackSelector = (DefaultTrackSelector) player.getTrackSelector();
                MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

                if (mappedTrackInfo == null) {
                    Toast.makeText(VideoPlayerActivity.this, "No track information available", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<AudioTrack> audioTracks = new ArrayList<>();
                int audioRendererIndex = -1;

                for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
                    if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                        audioRendererIndex = rendererIndex;
                        TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);

                        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                            TrackGroup trackGroup = trackGroups.get(groupIndex);
                            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                                Format format = trackGroup.getFormat(trackIndex);
                                String trackName = buildAudioTrackName(format);
                                audioTracks.add(new AudioTrack(rendererIndex, groupIndex, trackIndex, trackName, format));
                            }
                        }
                        break;
                    }
                }
                if (audioTracks.isEmpty()) {
                    Toast.makeText(VideoPlayerActivity.this, "No audio tracks found", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (audioTracks.size() == 1) {
                    Toast.makeText(VideoPlayerActivity.this, "Only one audio track available", Toast.LENGTH_SHORT).show();
                    return;
                }
                showAudioTrackDialog(audioTracks, trackSelector, audioRendererIndex);
            }
            private String buildAudioTrackName(Format format) {
                StringBuilder builder = new StringBuilder();

                if (format.language != null) {
                    String languageName = getLanguageName(format.language);
                    builder.append(languageName);
                } else {
                    builder.append("Unknown Language");
                }
                if (format.label != null && !format.label.isEmpty()) {
                    builder.append(" (").append(format.label).append(")");
                }
                if (format.codecs != null) {
                    builder.append(" - ").append(format.codecs);
                }
                if (format.channelCount != Format.NO_VALUE) {
                    builder.append(" - ").append(format.channelCount).append("ch");
                }
                return builder.toString();
            }

            private String getLanguageName(String languageCode) {
                if (languageCode == null) return "Unknown";
                switch (languageCode.toLowerCase()) {
                    case "en":
                        return "English";
                    case "hi":
                        return "Hindi";
                    case "es":
                        return "Spanish";
                    case "fr":
                        return "French";
                    case "de":
                        return "German";
                    case "ja":
                        return "Japanese";
                    case "ko":
                        return "Korean";
                    case "zh":
                        return "Chinese";
                    case "ar":
                        return "Arabic";
                    case "ru":
                        return "Russian";
                    case "pt":
                        return "Portuguese";
                    case "it":
                        return "Italian";
                    case "ta":
                        return "Tamil";
                    case "te":
                        return "Telugu";
                    case "kn":
                        return "Kannada";
                    case "ml":
                        return "Malayalam";
                    case "bn":
                        return "Bengali";
                    default:
                        return languageCode.toUpperCase();
                }
            }

            private void showAudioTrackDialog(List<AudioTrack> audioTracks, DefaultTrackSelector trackSelector, int audioRendererIndex) {
                String[] trackNames = new String[audioTracks.size()];
                for (int i = 0; i < audioTracks.size(); i++) {
                    trackNames[i] = audioTracks.get(i).name;
                }
                int currentSelected = findCurrentAudioTrack(audioTracks, trackSelector, audioRendererIndex);

                AlertDialog.Builder builder = new AlertDialog.Builder(VideoPlayerActivity.this);
                builder.setTitle("Select Audio Language");
                builder.setSingleChoiceItems(trackNames, currentSelected, (dialog, which) -> {
                    selectAudioTrack(audioTracks.get(which), trackSelector);
                    dialog.dismiss();
                    Toast.makeText(VideoPlayerActivity.this, "Switched to: " + audioTracks.get(which).name, Toast.LENGTH_SHORT).show();
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }

            private int findCurrentAudioTrack(List<AudioTrack> audioTracks, DefaultTrackSelector trackSelector, int audioRendererIndex) {
                DefaultTrackSelector.Parameters parameters = trackSelector.getParameters();
                TrackGroupArray trackGroups = trackSelector.getCurrentMappedTrackInfo().getTrackGroups(audioRendererIndex);
                DefaultTrackSelector.SelectionOverride override = parameters.getSelectionOverride(audioRendererIndex, trackGroups);

                if (override != null) {
                    for (int i = 0; i < audioTracks.size(); i++) {
                        AudioTrack track = audioTracks.get(i);
                        if (track.groupIndex == override.groupIndex && override.containsTrack(track.trackIndex)) {
                            return i;
                        }
                    }
                }
                // Remove the try-catch block that was causing issues
                return 0;
            }

            private void selectAudioTrack(AudioTrack audioTrack, DefaultTrackSelector trackSelector) {
                DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.getParameters().buildUpon();
                parametersBuilder.clearSelectionOverrides(audioTrack.rendererIndex);
                TrackGroupArray trackGroups = trackSelector.getCurrentMappedTrackInfo().getTrackGroups(audioTrack.rendererIndex);
                parametersBuilder.setSelectionOverride(
                        audioTrack.rendererIndex,
                        trackGroups,
                        new DefaultTrackSelector.SelectionOverride(audioTrack.groupIndex, audioTrack.trackIndex)
                );
                trackSelector.setParameters(parametersBuilder);
            }

            class AudioTrack {
                int rendererIndex;
                int groupIndex;
                int trackIndex;
                String name;
                Format format;

                AudioTrack(int rendererIndex, int groupIndex, int trackIndex, String name, Format format) {
                    this.rendererIndex = rendererIndex;
                    this.groupIndex = groupIndex;
                    this.trackIndex = trackIndex;
                    this.name = name;
                    this.format = format;
                }
            }
        });

        rotate.setOnClickListener(view -> {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        });

        playerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                activePointerCount = event.getPointerCount();
                scaleGestureDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);

                if (activePointerCount >= 2) {
                    return true;
                }
                if (activePointerCount < 2) {
                    // reproduce your existing brightness/volume single-finger logic:
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            start = true;
                            if (event.getX() < (device_width / 2)) {
                                left = true;
                                right = false;
                            } else {
                                left = false;
                                right = true;
                            }
                            baseX = event.getX();
                            baseY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            swipe_move = true;
                            diffX = (long) Math.ceil(event.getX() - baseX);
                            diffY = (long) Math.ceil(event.getY() - baseY);
                            double brightnessSpeed = 0.01;
                            if (Math.abs(diffY) > MINIMUM_DISTANCE) {
                                if (Math.abs(diffY) > Math.abs(diffX)) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        boolean value = android.provider.Settings.System.canWrite(getApplicationContext());
                                        if (value) {
                                            if (left) {
                                                // brightness code (unchanged)
                                                contentResolver = getContentResolver();
                                                window = getWindow();
                                                try {
                                                    android.provider.Settings.System.putInt(contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                                                            android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                                                    brightness = android.provider.Settings.System.getInt(contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS);
                                                } catch (
                                                        android.provider.Settings.SettingNotFoundException e) {
                                                    e.printStackTrace();
                                                }
                                                int new_brightness = (int) (brightness - (diffY * brightnessSpeed));
                                                new_brightness = Math.max(1, Math.min(250, new_brightness));
                                                double brt_percentage = Math.ceil((((double) new_brightness / (double) 250) * 100d));
                                                brt_progress_container.setVisibility(View.VISIBLE);
                                                brt_text_container.setVisibility(View.VISIBLE);
                                                brt_progress.setProgress((int) brt_percentage);
                                                if (brt_percentage < 30)
                                                    brt_icon.setImageResource(R.drawable.ic_brightness_low);
                                                else if (brt_percentage < 80)
                                                    brt_icon.setImageResource(R.drawable.ic_brightness_moderate);
                                                else
                                                    brt_icon.setImageResource(R.drawable.ic_brightness);
                                                brt_text.setText(" " + (int) brt_percentage + "%");
                                                android.provider.Settings.System.putInt(contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS, new_brightness);
                                                WindowManager.LayoutParams layoutParams = window.getAttributes();
                                                layoutParams.screenBrightness = brightness / (float) 255;
                                                window.setAttributes(layoutParams);
                                            } else {
                                                // volume code (unchanged)
                                                vol_text_container.setVisibility(View.VISIBLE);
                                                mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                                int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                                                int newMediaVolume = getNewMediaVolume(maxVol, brightnessSpeed);
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newMediaVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                                                double volPercentage = Math.ceil((((double) newMediaVolume / (double) maxVol) * 100d));
                                                vol_text.setText(" " + (int) volPercentage + "%");
                                                if (volPercentage < 1) {
                                                    vol_icon.setImageResource(R.drawable.ic_volume_off);
                                                    vol_text.setText("OFF");
                                                } else {
                                                    vol_icon.setImageResource(R.drawable.ic_volume);
                                                }
                                                vol_progress_container.setVisibility(View.VISIBLE);
                                                vol_progress.setProgress((int) volPercentage);
                                            }
                                            success = true;
                                        } else {
                                            Toast.makeText(VideoPlayerActivity.this, "Allow write settings for swipe controls", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                            intent.setData(Uri.parse("package:" + getPackageName()));
                                            startActivityForResult(intent, 111);
                                        }
                                    }
                                }
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            swipe_move = false;
                            start = false;
                            vol_progress_container.setVisibility(View.GONE);
                            brt_progress_container.setVisibility(View.GONE);
                            vol_text_container.setVisibility(View.GONE);
                            brt_text_container.setVisibility(View.GONE);
                            break;
                    }
                }
                return true;
            }

            private int getNewMediaVolume(int maxVol, double brightnessSpeed) {
                double cal = (double) diffY * ((double) maxVol / ((double) (device_height * 2) - brightnessSpeed));
                int newMediaVolume = mediaVolume - (int) cal;
                if (newMediaVolume > maxVol) {
                    newMediaVolume = maxVol;
                } else if (newMediaVolume < 1) {
                    newMediaVolume = 0;
                }
                return newMediaVolume;
            }

            @Override
            public void onDoubleTouch(MotionEvent event) {
                if (player == null) return;
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                float screenWidth = displayMetrics.widthPixels;
//                float screenWidth = device_width;
                float oneThird = screenWidth / 3;
                float x = event.getX();

                if (x < oneThird) {
                    // Left side - seek backward 10 seconds
                    seekBackward();
                } else if (x > oneThird * 2) {
                    // Right side - seek forward 10 seconds
                    seekForward();
                } else {
                    // Middle - toggle play/pause
                    togglePlayPause();
                }
                super.onDoubleTouch(event);
            }

            @Override
            public void onSingleTouch() {
                super.onSingleTouch();
                // Simply toggle the controls - don't use delayed calls here
                handler.removeCallbacks(hideControlsRunnable);
                if (playerView.isControllerVisible()) {
                    // Hide controls immediately
                    playerView.hideController();
                    hideSystemUI();
                } else {
                    // Show controls
                    playerView.showController();
                    showSystemUI();
                    // Auto-hide after 3 seconds
                    handler.postDelayed(hideControlsRunnable, 3000);
                }
                if (double_tap_playpause.getVisibility() == View.VISIBLE) {
                    double_tap_playpause.setVisibility(View.GONE);
                }
            }
        });
        horizontalIconList();
    }

    private void resetZoom() {
        View videoSurface = playerView.getVideoSurfaceView();
        if (videoSurface == null) return;

        // animate reset to normal
        videoSurface.animate()
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .translationY(0f)
                .setDuration(200)
                .start();

        scaleFactor = 1f;
        translationX = 0f;
        translationY = 0f;
        isZoomed = false;
        updateZoomUI();
    }

    private void updateZoomUI() {
        if (zoomContainer == null || zoomLevelText == null || resetZoomBtn == null) return;
        if (isZoomed) {
            zoomLevelText.setText(String.format(Locale.US, "%.1fx", scaleFactor));
            zoomContainer.setVisibility(View.VISIBLE);
            resetZoomBtn.setVisibility(View.VISIBLE);
        } else {
            // hide when at 1x
            zoomContainer.setVisibility(View.GONE);
            resetZoomBtn.setVisibility(View.GONE);
        }
    }

    private float clamp(float val, float min, float max) {
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    private void showSeekFeedback(String text, int iconRes) {
        // Remove existing feedback view if any
        removeSeekFeedback();
        // Create feedback layout
        FrameLayout feedbackLayout = new FrameLayout(VideoPlayerActivity.this);
        feedbackLayout.setId(View.generateViewId());
        feedbackLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        ));
        feedbackLayout.setBackgroundResource(R.drawable.seek_feedback_bg);
        // Create icon
        ImageView icon = new ImageView(VideoPlayerActivity.this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        // Create text
        TextView textView = new TextView(VideoPlayerActivity.this);
        textView.setText(text);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(14);
        textView.setGravity(Gravity.CENTER);
        // Layout for icon + text
        LinearLayout contentLayout = new LinearLayout(VideoPlayerActivity.this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.addView(icon, new LinearLayout.LayoutParams(80, 80));
        contentLayout.addView(textView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        feedbackLayout.addView(contentLayout);
        // Add to root layout
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(feedbackLayout);
        // Animate and remove
        feedbackLayout.setAlpha(0f);
        feedbackLayout.animate()
                .alpha(1f)
                .setDuration(200)
                .withEndAction(() -> {
                    new Handler().postDelayed(() -> {
                        feedbackLayout.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction(() -> removeSeekFeedback())
                                .start();
                    }, 800);
                })
                .start();
    }

    public void seekBackward() {
        long currentPosition = player.getCurrentPosition();
        long newPosition = currentPosition - 10000; // 10 seconds back

        if (newPosition < 0) newPosition = 0;

        player.seekTo(newPosition);
        showSeekFeedback("-10s", R.drawable.ic_rewind);
    }

    public void seekForward() {
        long currentPosition = player.getCurrentPosition();
        long duration = player.getDuration();
        long newPosition = currentPosition + 10000; // 10 seconds forward

        if (newPosition > duration) newPosition = duration;

        player.seekTo(newPosition);
        showSeekFeedback("+10s", R.drawable.ic_forward);
    }

    private void handlePlayPauseState(boolean isPlaying) {
        if (!isPlaying) {
            // Video paused - show controls immediately
            playerView.showController();
            showSystemUI();

            // Keep controls visible while paused (remove auto-hide)
            handler.removeCallbacks(hideControlsRunnable);
        } else {
            // Video playing - hide controls after delay
            handler.postDelayed(hideControlsRunnable, 3000);
        }
    }

    public void togglePlayPause() {
        boolean isPlaying = player.getPlayWhenReady();
        player.setPlayWhenReady(!isPlaying);
        handlePlayPauseState(!isPlaying);
        showPlayPauseFeedback(!isPlaying);
    }

    private void showPlayPauseFeedback(boolean isPlaying) {
        removeSeekFeedback();
        FrameLayout feedbackLayout = new FrameLayout(VideoPlayerActivity.this);
        feedbackLayout.setId(View.generateViewId());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        feedbackLayout.setLayoutParams(params);
        feedbackLayout.setBackgroundResource(R.drawable.seek_feedback_bg);

        ImageView icon = new ImageView(VideoPlayerActivity.this);
        icon.setImageResource(isPlaying ? R.drawable.ic_play : R.drawable.ic_pause);
        icon.setColorFilter(Color.WHITE);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(120, 120, Gravity.CENTER);
        icon.setLayoutParams(iconParams);

        TextView textView = new TextView(VideoPlayerActivity.this);
        textView.setText(isPlaying ? "Playing" : "Paused");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(16);
        textView.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        textView.setLayoutParams(textParams);

        LinearLayout contentLayout = new LinearLayout(VideoPlayerActivity.this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        contentLayout.setLayoutParams(contentParams);
        contentLayout.addView(icon);
        contentLayout.addView(textView);
        feedbackLayout.addView(contentLayout);

        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(feedbackLayout);
        feedbackLayout.setAlpha(0f);
        feedbackLayout.animate()
                .alpha(1f)
                .setDuration(200)
                .withEndAction(() -> {
                    new Handler().postDelayed(() -> {
                        feedbackLayout.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction(() -> {
                                    if (feedbackLayout.getParent() != null) {
                                        ((ViewGroup) feedbackLayout.getParent()).removeView(feedbackLayout);
                                    }
                                })
                                .start();
                    }, 800);
                })
                .start();
    }

    private void removeSeekFeedback() {
        ViewGroup rootView = findViewById(android.R.id.content);
        for (int i = 0; i < rootView.getChildCount(); i++) {
            View child = rootView.getChildAt(i);
            if (child instanceof FrameLayout && child.getId() != R.id.controller_root) {
                rootView.removeView(child);
            }
        }
    }

    private void hideSystemUI() {
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape, hide system bars completely
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.hide(WindowInsetsCompat.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // In portrait, keep system bars visible but transparent
            // This ensures toolbar stays within safe area
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars());
                controller.show(WindowInsetsCompat.Type.navigationBars());
            }
        }
    }

    private void showSystemUI() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
        controller.show(WindowInsetsCompat.Type.navigationBars());
    }

    private void horizontalIconList() {
        // Initial icons in order (left to right): Speed, Equalizer, Pop-Up, Subtitle, Right
        iconModelsArrayList.add(new IconModel(R.drawable.ic_speed, "Speed"));
        iconModelsArrayList.add(new IconModel(R.drawable.ic_equalizer, "Equalizer"));
        iconModelsArrayList.add(new IconModel(R.drawable.ic_pip_mode, "Pop-Up"));
        iconModelsArrayList.add(new IconModel(R.drawable.ic_subtitle, "Subtitle"));
        iconModelsArrayList.add(new IconModel(R.drawable.ic_right, "")); // Expand icon on the rightmost

        playbackIconsAdapter = new PlaybackIconsAdapter(iconModelsArrayList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        recyclerViewIcons.setLayoutManager(layoutManager);
        recyclerViewIcons.setAdapter(playbackIconsAdapter);
        playbackIconsAdapter.notifyDataSetChanged();
        playbackIconsAdapter.setOnItemClickListener(new PlaybackIconsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Cancel auto-hide when any icon is clicked (keep controls visible)
                handler.removeCallbacks(hideControlsRunnable);

                if ((!expand && position == 4) || (expand && position == 9)) { // Expand/Collapse button
                    if (expand) {
                        // Collapse - show only main 5 icons
                        iconModelsArrayList.clear();
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_speed, "Speed"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_equalizer, "Equalizer"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_pip_mode, "Pop-Up"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_subtitle, "Subtitle"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_right, ""));
                        playbackIconsAdapter.notifyDataSetChanged();
                        expand = false;

                        // Restart auto-hide when collapsed
                        handler.postDelayed(hideControlsRunnable, 3000);
                    } else {
                        // Expand - add additional options and move collapse button to the end
                        iconModelsArrayList.clear();
                        // Keep original 4 icons
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_speed, "Speed"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_equalizer, "Equalizer"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_pip_mode, "Pop-Up"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_subtitle, "Subtitle"));
                        // Add new options
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_night_mode, "Night"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_rotate, "Rotate"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_volume_off, "Mute"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_volume, "Volume"));
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_brightness, "Brightness"));
                        // Add collapse button at the END
                        iconModelsArrayList.add(new IconModel(R.drawable.ic_left, ""));
                        playbackIconsAdapter.notifyDataSetChanged();
                        expand = true;

                        // Keep controls visible when expanded (no auto-hide)
                    }
                } else if (position == 0) { // Speed
                    showSpeedDialog();
                } else if (position == 1) { // Equalizer
                    showEqualizer();
                } else if (position == 2) { // Pop-Up (PIP)
                    enterPipMode();
                } else if (position == 3) { // Subtitle
                    showUnifiedSubtitleDialog();
                } else if (expand && position == 4) { // Night Mode (expanded)
                    toggleNightMode();
                } else if (expand && position == 5) { // Rotate (expanded)
                    toggleRotation();
                } else if (expand && position == 6) { // Mute (expanded)
                    toggleMute();
                } else if (expand && position == 7) { // Volume (expanded)
                    showVolumeDialog();
                } else if (expand && position == 8) { // Brightness (expanded)
                    showBrightnessDialog();
                }
            }
        });
    }

    private void showSpeedDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(VideoPlayerActivity.this);
        alertDialog.setTitle("Select Playback Speed").setPositiveButton("Ok", null);
        String[] items = {"0.5x", "1x", "1.25x", "1.5x", "2x"};
        int checkedItem = -1;
        alertDialog.setSingleChoiceItems(items, checkedItem, (dialogInterface, i) -> {
            switch (i) {
                case 0:
                    speed = 0.5f;
                    break;
                case 1:
                    speed = 1f;
                    break;
                case 2:
                    speed = 1.25f;
                    break;
                case 3:
                    speed = 1.5f;
                    break;
                case 4:
                    speed = 2f;
                    break;
            }
            parameters = new PlaybackParameters(speed);
            player.setPlaybackParameters(parameters);
            handler.postDelayed(hideControlsRunnable, 3000);
        });
        alertDialog.show();
    }

    private void showEqualizer() {
        if (eqContainer.getVisibility() == View.GONE) {
            eqContainer.setVisibility(View.VISIBLE);
        }
        final int sessionId = player.getAudioSessionId();
        Settings.isEditing = false;
        EqualizerFragment equalizerFragment = EqualizerFragment.newBuilder()
                .setAccentColor(Color.parseColor(("#1A78F2")))
                .setAudioSessionId(sessionId)
                .build();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.eqFrame, equalizerFragment)
                .commit();
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Calculate aspect ratio based on video dimensions
                Rational aspectRatio = new Rational(16, 9);
                // Get video dimensions if available
                if (player != null && player.getVideoFormat() != null) {
                    int width = player.getVideoFormat().width;
                    int height = player.getVideoFormat().height;
                    if (width > 0 && height > 0) {
                        aspectRatio = new Rational(width, height);
                    }
                }
                pictureInPicture.setAspectRatio(aspectRatio);

                // Add actions for PIP mode (optional)
                ArrayList<RemoteAction> actions = new ArrayList<>();
                Intent actionIntent = new Intent(this, VideoPlayerActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE);

                Icon icon = Icon.createWithResource(this, R.drawable.ic_pip_mode);
                RemoteAction remoteAction = new RemoteAction(icon, "Play/Pause", "Toggle play/pause", pendingIntent);
                actions.add(remoteAction);

                pictureInPicture.setActions(actions);
                // Enter PIP mode
                enterPictureInPictureMode(pictureInPicture.build());
            } catch (Exception e) {
                Toast.makeText(this, "PIP not supported on this device", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "PIP requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        isCrossChecked = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            // Hide controls when in PIP mode
            playerView.hideController();
            hideSystemUI();
        } else {
            // Show controls when returning from PIP
            playerView.showController();
            showSystemUI();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Automatically enter PIP when user presses home button (optional)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode()) {
            enterPipMode();
        }
    }

    private void toggleNightMode() {
        if (dark) {
            nightMode.setVisibility(View.GONE);
            iconModelsArrayList.set(4, new IconModel(R.drawable.ic_night_mode, "Night"));
            dark = false;
        } else {
            nightMode.setVisibility(View.VISIBLE);
            iconModelsArrayList.set(4, new IconModel(R.drawable.ic_day, "Day"));
            dark = true;
        }
        playbackIconsAdapter.notifyDataSetChanged();
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void toggleRotation() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void toggleMute() {
        if (mute) {
            player.setVolume(100);
            iconModelsArrayList.set(6, new IconModel(R.drawable.ic_volume_off, "Mute"));
            mute = false;
        } else {
            player.setVolume(0);
            iconModelsArrayList.set(6, new IconModel(R.drawable.ic_volume, "Un-mute"));
            mute = true;
        }
        playbackIconsAdapter.notifyDataSetChanged();
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void showVolumeDialog() {
        VolumeDialog volumeDialog = new VolumeDialog();
        volumeDialog.show(getSupportFragmentManager(), "dialog");
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void showBrightnessDialog() {
        BrightnessDialog brightnessDialog = new BrightnessDialog();
        brightnessDialog.show(getSupportFragmentManager(), "dialog");
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void initViews() {
        nextButton = findViewById(R.id.exo_next);
        previousButton = findViewById(R.id.exo_prev);
        videoBack = findViewById(R.id.video_back);
        lock = findViewById(R.id.lock);
        unlock = findViewById(R.id.unlock);
        scaling = findViewById(R.id.scaling);
        root = findViewById(R.id.controller_root);
        nightMode = findViewById(R.id.night_mode);
        videoList = findViewById(R.id.video_list);
        videoMore = findViewById(R.id.video_more);
        recyclerViewIcons = findViewById(R.id.recyclerview_icon);
        eqContainer = findViewById(R.id.eqFrame);
        vol_text = findViewById(R.id.vol_text);
        brt_text = findViewById(R.id.brt_text);
        vol_progress = findViewById(R.id.vol_progress);
        brt_progress = findViewById(R.id.brt_progress);
        brt_progress_container = findViewById(R.id.brt_progress_container);
        vol_progress_container = findViewById(R.id.vol_progress_container);
        brt_text_container = findViewById(R.id.brt_text_container);
        vol_text_container = findViewById(R.id.vol_text_container);
        vol_icon = findViewById(R.id.vol_icon);
        brt_icon = findViewById(R.id.brt_icon);
        double_tap_playpause = findViewById(R.id.double_tap_play_pause);

        zoomContainer = findViewById(R.id.zoom_container);
        zoomLevelText = findViewById(R.id.zoom_level_text);
        resetZoomBtn = findViewById(R.id.reset_zoom_btn);
        resetZoomBtn.setOnClickListener(v -> resetZoom());
        zoomContainer.setVisibility(View.GONE);
        zoomLevelText.setText("1.0x");
        resetZoomBtn.setVisibility(View.GONE);


        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                scaleFactor *= factor;
                scaleFactor = Math.max(1f, Math.min(scaleFactor, 4f));
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                translationX += (focusX - playerView.getWidth() / 2f) * (1 - prevScaleFactor / scaleFactor);
                translationY += (focusY - playerView.getHeight() / 2f) * (1 - prevScaleFactor / scaleFactor);

                if (scaleFactor <= 1f + 1e-3) {
                    scaleFactor = 1f;
                    translationX = 0f;
                    translationY = 0f;
                }

                isZoomed = scaleFactor > 1.02f;
                applyTransform();
                updateZoomUI();
                return true;
            }
        });
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                toggleControls();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distX, float distY) {
                if (!isZoomed) return false;
                if (activePointerCount < 2) return false;
                translationX -= distX;
                translationY -= distY;
                applyTransform();
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                float x = e.getX();
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                float width = dm.widthPixels;

                if (x < width / 3) {
                    seekBackward();
                } else if (x > width * 2 / 3) {
                    seekForward();
                } else {
                    togglePlayPause();
                }
                return true;
            }
        });
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        title = findViewById(R.id.video_title);
        title.setText(videoTitle);
        videoContainer = findViewById(R.id.video_container);

        nextButton.setOnClickListener(this);
        previousButton.setOnClickListener(this);
        videoBack.setOnClickListener(this);
        lock.setOnClickListener(this);
        unlock.setOnClickListener(this);
        videoList.setOnClickListener(this);
        videoMore.setOnClickListener(this);
        scaling.setOnClickListener(firstListener);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mVideoFiles = extras.getParcelableArrayList("videoArrayList");
        }
        dialogProperties = new DialogProperties();
        filePickerDialog = new FilePickerDialog(VideoPlayerActivity.this);
        filePickerDialog.setTitle("Select a subtitle file");
        filePickerDialog.setPositiveBtnName("OK");
        filePickerDialog.setNegativeBtnName("Cancel");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPicture = new PictureInPictureParams.Builder();
        }
    }

    private void toggleControls() {
        handler.removeCallbacks(hideControlsRunnable);
        if (playerView.isControllerVisible()) {
            playerView.hideController();
            hideSystemUI();
        } else {
            playerView.showController();
            showSystemUI();
            handler.postDelayed(hideControlsRunnable, 3000);
        }
    }

    private void applyTransform() {
        View videoSurface = playerView.getVideoSurfaceView();
        if (videoSurface == null) return;
        // Get container dimensions
        float containerWidth = playerView.getWidth();
        float containerHeight = playerView.getHeight();
        if (containerWidth == 0 || containerHeight == 0) return;
        // Use center pivot for consistent scaling
        videoSurface.setPivotX(containerWidth / 2f);
        videoSurface.setPivotY(containerHeight / 2f);
        // Compute max translation bounds to keep content visible
        float maxTransX = Math.max(0f, (scaleFactor - 1f) * containerWidth / 2f);
        float maxTransY = Math.max(0f, (scaleFactor - 1f) * containerHeight / 2f);
        // Clamp translations to bounds
        translationX = clamp(translationX, -maxTransX, maxTransX);
        translationY = clamp(translationY, -maxTransY, maxTransY);
        // CRITICAL: Apply transforms directly and forcefully
        // This ensures zoom state is preserved regardless of system UI changes
        videoSurface.setScaleX(scaleFactor);
        videoSurface.setScaleY(scaleFactor);
        videoSurface.setTranslationX(translationX);
        videoSurface.setTranslationY(translationY);
        // CRITICAL: Ensure video container ignores any system UI padding
        // This prevents the video from shifting when system UI appears
        FrameLayout videoContainer = findViewById(R.id.video_container);
        if (videoContainer != null) {
            videoContainer.setPadding(0, 0, 0, 0);
            // Also ensure the container itself doesn't get transformed
            videoContainer.setScaleX(1f);
            videoContainer.setScaleY(1f);
            videoContainer.setTranslationX(0f);
            videoContainer.setTranslationY(0f);
        }
        // CRITICAL: Force the PlayerView to ignore any layout changes
        // This prevents ExoPlayer from resetting our transforms
        playerView.setScaleX(1f);
        playerView.setScaleY(1f);
        playerView.setTranslationX(0f);
        playerView.setTranslationY(0f);
        // Update zoom state and UI
        isZoomed = scaleFactor > 1.02f;
        updateZoomUI();
    }

    private void playVideoFromPath() {
        player = new SimpleExoPlayer.Builder(this).build();
        DefaultDataSourceFactory defaultDataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "Video Player"));
        concatenatingMediaSource = new ConcatenatingMediaSource();

        if (mVideoFiles != null) {
            for (int i = 0; i < mVideoFiles.size(); i++) {
                String path = mVideoFiles.get(i).getPath();
                Uri uri = Uri.parse(path);
                MediaSource mediaSource = new ProgressiveMediaSource.Factory(defaultDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri));
                concatenatingMediaSource.addMediaSource(mediaSource);
            }
        }
        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        player.setPlaybackParameters(parameters);
        player.prepare(concatenatingMediaSource);
        player.seekTo(position, C.TIME_UNSET);
        player.setPlayWhenReady(true);
        playerView.showController();
        showSystemUI();
        // Auto-hide after 3 seconds
        handler.postDelayed(hideControlsRunnable, 3000);
        playError();
    }

    private void playVideoFromUri(Uri uri) {
        DefaultDataSourceFactory defaultDataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "Video Player"));

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(defaultDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));

        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.prepare(mediaSource);
        player.seekTo(position, C.TIME_UNSET);
        player.setPlayWhenReady(true);
        playerView.showController();
        showSystemUI();
        // Auto-hide after 3 seconds
        handler.postDelayed(hideControlsRunnable, 3000);
        playError();
    }

    private void playVideoSubtitle(Uri subtitle) {
        long oldPosition = player.getCurrentPosition();
        player.stop();

        String path = mVideoFiles.get(position).getPath();
        Uri uri = Uri.parse(path);
        player = new SimpleExoPlayer.Builder(this).build();
        DefaultDataSourceFactory defaultDataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "Video Player"));
        concatenatingMediaSource = new ConcatenatingMediaSource();

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(defaultDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(String.valueOf(uri))));
        Format textFormat = Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP, Format.NO_VALUE, "app");
        MediaSource subtitleSource = new SingleSampleMediaSource.Factory(defaultDataSourceFactory).setTreatLoadErrorsAsEndOfStream(true)
                .createMediaSource(Uri.parse(String.valueOf(subtitle)), textFormat, C.TIME_UNSET);
        MergingMediaSource mergingMediaSource = new MergingMediaSource(mediaSource, subtitleSource);
        concatenatingMediaSource.addMediaSource(mergingMediaSource);

        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        player.setPlaybackParameters(parameters);
        player.prepare(concatenatingMediaSource);
        player.seekTo(position, oldPosition);
        playError();
    }

    private void playError() {
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Toast.makeText(VideoPlayerActivity.this, "Video Playing Error", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    boolean isPlaying = player.getPlayWhenReady();
                    handlePlayPauseState(isPlaying);
                }
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                handlePlayPauseState(playWhenReady);
            }
        });
        player.setPlayWhenReady(true);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.eqFrame);
        if (eqContainer.getVisibility() == View.GONE) {
            super.onBackPressed();
        } else {
            if (fragment.isVisible() && eqContainer.getVisibility() == View.VISIBLE) {
                eqContainer.setVisibility(View.GONE);
            } else {
                if (player != null) {
                    player.release();
                }
                super.onBackPressed();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onPause() {
        super.onPause();
        player.setPlayWhenReady(false);
        if (isInPictureInPictureMode()) {
            player.setPlayWhenReady(true);
        } else {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        player.setPlayWhenReady(true);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.video_back) {
            if (player != null) {
                player.release();
            }
            finish();
        } else if (v.getId() == R.id.lock) {
            root.setVisibility(View.VISIBLE);
            lock.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Unlocked", Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.unlock) {
            root.setVisibility(View.INVISIBLE);
            lock.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Locked", Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.video_list) {
            PlaylistDialog playlistDialog = new PlaylistDialog(mVideoFiles, videoFilesAdapter);
            playlistDialog.show(getSupportFragmentManager(), playlistDialog.getTag());
        } else if (v.getId() == R.id.exo_next) {
            try {
                player.stop();
                position++;
                playVideoFromPath();
                title.setText(mVideoFiles.get(position).getDisplayName());
            } catch (Exception e) {
                Toast.makeText(this, "No next video", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (v.getId() == R.id.exo_prev) {
            try {
                player.stop();
                position--;
                playVideoFromPath();
                title.setText(mVideoFiles.get(position).getDisplayName());
            } catch (Exception e) {
                Toast.makeText(this, "No previous video", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (v.getId() == R.id.video_more) {
            PopupMenu popupMenu = new PopupMenu(VideoPlayerActivity.this, videoMore);
            popupMenu.inflate(R.menu.player_menu);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    int itemId = menuItem.getItemId();
                    if (itemId == R.id.next) {
                        nextButton.performClick();
                    } else if (itemId == R.id.send) {
                        Uri videoUri = getIntent().getData();
                        Uri uri = null;
                        if (videoUri != null) {
                            uri = videoUri;
                        } else {
                            if (mVideoFiles != null) {
                                uri = Uri.parse(mVideoFiles.get(position).getPath());
                            }
                        }
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("video/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        startActivity(Intent.createChooser(shareIntent, "Share video via?"));
                    } else if (itemId == R.id.properties) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(VideoPlayerActivity.this);
                        alertDialog.setTitle("Properties");
                        String one = "File: " + mVideoFiles.get(position).getDisplayName();
                        String path = mVideoFiles.get(position).getPath();
                        int indexOfPath = path.lastIndexOf("/");
                        String two = "Path: " + path.substring(0, indexOfPath);
                        String three = "Size: " + android.text.format.Formatter
                                .formatFileSize(VideoPlayerActivity.this, Long.parseLong(mVideoFiles.get(position).getSize()));
                        String nameWithFormat = mVideoFiles.get(position).getDisplayName();
                        int index = nameWithFormat.lastIndexOf(".");
                        String format = nameWithFormat.substring(index + 1);
                        String five = "Format: " + format;
                        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                        mediaMetadataRetriever.setDataSource(mVideoFiles.get(position).getPath());
                        String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                        String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                        String six = "Resolution: " + width + "x" + height;
                        alertDialog.setMessage(one + "\n\n" + two + "\n\n" + three + "\n\n" + five + "\n\n" + six);
                        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        alertDialog.show();
                    } else if (itemId == R.id.delete) {
                        AlertDialog.Builder alertDialogDelete = new AlertDialog.Builder(VideoPlayerActivity.this);
                        alertDialogDelete.setTitle("Delete");
                        alertDialogDelete.setMessage("Do you want to delete this video?");
                        alertDialogDelete.setPositiveButton("Delete", (dialogInterface, i) -> {
                            Uri contentUri = ContentUris.withAppendedId(
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    Long.parseLong(mVideoFiles.get(position).getId())
                            );
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                ArrayList<Uri> uriList = new ArrayList<>();
                                uriList.add(contentUri);
                                PendingIntent deleteIntent = MediaStore.createDeleteRequest(getContentResolver(), uriList);
                                try {
                                    startIntentSenderForResult(deleteIntent.getIntentSender(), 108, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                int rowsDeleted = getContentResolver().delete(contentUri, null, null);
                                if (rowsDeleted > 0) {
                                    mVideoFiles.remove(position);
                                    videoFilesAdapter.notifyItemRemoved(position);
                                    nextButton.performClick();
                                    Toast.makeText(VideoPlayerActivity.this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(VideoPlayerActivity.this, "Video is not deleted", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        alertDialogDelete.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
                        alertDialogDelete.show();
                    } else if (itemId == R.id.subtitle) {
                        showUnifiedSubtitleDialog();
                    }
                    return true;
                }
            });
            popupMenu.show();
        }
    }

    private List<SubtitleTrack> getEmbeddedSubtitleTracks(MappingTrackSelector.MappedTrackInfo mappedTrackInfo) {
        List<SubtitleTrack> subtitleTracks = new ArrayList<>();
        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
            if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT) {
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
                for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                    TrackGroup trackGroup = trackGroups.get(groupIndex);
                    for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                        Format format = trackGroup.getFormat(trackIndex);
                        String trackName = buildSubtitleTrackName(format);
                        subtitleTracks.add(new SubtitleTrack(rendererIndex, groupIndex, trackIndex, trackName, format));
                    }
                }
                break;
            }
        }
        return subtitleTracks;
    }

    private void disableAllSubtitles(DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.getParameters().buildUpon();

        // Disable all text renderers (subtitle tracks)
        for (int rendererIndex = 0; rendererIndex < trackSelector.getCurrentMappedTrackInfo().getRendererCount(); rendererIndex++) {
            if (trackSelector.getCurrentMappedTrackInfo().getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT) {
                parametersBuilder.setRendererDisabled(rendererIndex, true);
            }
        }
        // Clear external subtitle
        uriSubtitle = null;
        trackSelector.setParameters(parametersBuilder);
    }

    private String buildSubtitleTrackName(Format format) {
        StringBuilder builder = new StringBuilder();
        // Language
        if (format.language != null) {
            String languageName = getLanguageName(format.language);
            builder.append(languageName);
        } else {
            builder.append("Unknown");
        }
        // Label if available
        if (format.label != null && !format.label.isEmpty()) {
            builder.append(" (").append(format.label).append(")");
        }
        return builder.toString();
    }

    private String getLanguageName(String languageCode) {
        if (languageCode == null) return "Unknown";

        switch (languageCode.toLowerCase()) {
            case "en":
                return "English";
            case "hi":
                return "Hindi";
            case "es":
                return "Spanish";
            case "fr":
                return "French";
            case "de":
                return "German";
            case "ja":
                return "Japanese";
            case "ko":
                return "Korean";
            case "zh":
                return "Chinese";
            case "ar":
                return "Arabic";
            case "ru":
                return "Russian";
            case "pt":
                return "Portuguese";
            case "it":
                return "Italian";
            case "ta":
                return "Tamil";
            case "te":
                return "Telugu";
            case "kn":
                return "Kannada";
            case "ml":
                return "Malayalam";
            case "bn":
                return "Bengali";
            default:
                return languageCode.toUpperCase();
        }
    }

    class SubtitleTrack {
        int rendererIndex;
        int groupIndex;
        int trackIndex;
        String name;
        Format format;

        SubtitleTrack(int rendererIndex, int groupIndex, int trackIndex, String name, Format format) {
            this.rendererIndex = rendererIndex;
            this.groupIndex = groupIndex;
            this.trackIndex = trackIndex;
            this.name = name;
            this.format = format;
        }
    }

    View.OnClickListener firstListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            scaling.setImageResource(R.drawable.fullscreen);
            Toast.makeText(VideoPlayerActivity.this, "Full Screen", Toast.LENGTH_SHORT).show();
            scaling.setOnClickListener(secondListener);
        }
    };
    View.OnClickListener secondListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            scaling.setImageResource(R.drawable.zoom);
            Toast.makeText(VideoPlayerActivity.this, "Zoom", Toast.LENGTH_SHORT).show();
            scaling.setOnClickListener(thirdListener);
        }
    };
    View.OnClickListener thirdListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            scaling.setImageResource(R.drawable.fit);
            Toast.makeText(VideoPlayerActivity.this, "Fit", Toast.LENGTH_SHORT).show();
            scaling.setOnClickListener(firstListener);
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (isCrossChecked) {
            player.release();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 111) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean value = android.provider.Settings.System.canWrite(getApplicationContext());
                if (value) {
                    success = true;
                } else {
                    Toast.makeText(this, "Write Permission not granted to this app", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (requestCode == 108) {
            if (resultCode == RESULT_OK) {
                mVideoFiles.remove(position);
                videoFilesAdapter.notifyItemRemoved(position);
                nextButton.performClick();
                Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Delete cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustControlsForOrientation();
        if (singleTap) {
            showSystemUI();
        } else {
            hideSystemUI();
        }
    }

    private void setFullScreenImmediately() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Fallback for older versions
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    private void setupOrientationAwareControls() {
        adjustControlsForOrientation();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            adjustControlsForOrientation();
        });
    }

    private void adjustControlsForOrientation() {
        if (root == null) return;

        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape: Apply small fixed margins
            applyMarginsToView(root, 90, 0, 90, 0);

            LinearLayout customControls = findViewById(R.id.custom_controls_container);
            if (customControls != null) {
                applyMarginsToView(customControls, 80, 0, 80, 0);
            }
        } else {
            // Portrait: Use system UI insets instead of fixed margins
            applyMarginsToView(root, 0, 0, 0, 0);

            LinearLayout customControls = findViewById(R.id.custom_controls_container);
            if (customControls != null) {
                applyMarginsToView(customControls, 0, 0, 0, 0);
            }
            // Apply proper bottom margin for navigation bar in portrait
            applyPortraitSystemUIInsets();
        }
    }

    private void applyPortraitSystemUIInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Only apply bottom margin in portrait to avoid navigation bar overlap
            applyMarginsToView(root, 0, 0, 0, systemBars.bottom);

            LinearLayout customControls = findViewById(R.id.custom_controls_container);
            if (customControls != null) {
                applyMarginsToView(customControls, 0, 0, 0, systemBars.bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });
        // Force a refresh of the insets
        ViewCompat.requestApplyInsets(root);
    }

    private void applyMarginsToView(View view, int left, int top, int right, int bottom) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (params != null) {
            params.leftMargin = left;
            params.topMargin = top;
            params.rightMargin = right;
            params.bottomMargin = bottom;
            view.setLayoutParams(params);
        }
    }

    private void showUnifiedSubtitleDialog() {
        if (player == null) {
            Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        DefaultTrackSelector trackSelector = (DefaultTrackSelector) player.getTrackSelector();
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();

        List<SubtitleOption> subtitleOptions = new ArrayList<>();

        // Add "No Subtitle" option (first)
        subtitleOptions.add(new SubtitleOption(SubtitleOption.TYPE_NONE, -1, -1, -1, "No Subtitle", null));

        // Add "Select from Device" option (second)
        subtitleOptions.add(new SubtitleOption(SubtitleOption.TYPE_EXTERNAL, -1, -1, -1, "Select from Device", null));

        // Add embedded subtitles (after the main options)
        if (mappedTrackInfo != null) {
            List<SubtitleTrack> embeddedTracks = getEmbeddedSubtitleTracks(mappedTrackInfo);
            for (SubtitleTrack track : embeddedTracks) {
                subtitleOptions.add(new SubtitleOption(
                        SubtitleOption.TYPE_EMBEDDED,
                        track.rendererIndex,
                        track.groupIndex,
                        track.trackIndex,
                        track.name,
                        track.format
                ));
            }
        }
        showSubtitleSelectionDialog(subtitleOptions, trackSelector);
    }

    class SubtitleOption {
        static final int TYPE_NONE = 0;
        static final int TYPE_EMBEDDED = 1;
        static final int TYPE_EXTERNAL = 2;

        int type;
        int rendererIndex;
        int groupIndex;
        int trackIndex;
        String name;
        Format format;

        SubtitleOption(int type, int rendererIndex, int groupIndex, int trackIndex, String name, Format format) {
            this.type = type;
            this.rendererIndex = rendererIndex;
            this.groupIndex = groupIndex;
            this.trackIndex = trackIndex;
            this.name = name;
            this.format = format;
        }
    }

    private void showSubtitleSelectionDialog(List<SubtitleOption> subtitleOptions, DefaultTrackSelector trackSelector) {
        String[] options = new String[subtitleOptions.size()];
        for (int i = 0; i < subtitleOptions.size(); i++) {
            options[i] = subtitleOptions.get(i).name;
        }

        // Find which subtitle is currently selected
        int currentlySelectedIndex = findCurrentlySelectedSubtitleIndex(subtitleOptions, trackSelector);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Subtitle");

        builder.setSingleChoiceItems(options, currentlySelectedIndex, (dialog, which) -> {
            SubtitleOption selectedOption = subtitleOptions.get(which);

            switch (selectedOption.type) {
                case SubtitleOption.TYPE_NONE:
                    disableAllSubtitles(trackSelector);
                    uriSubtitle = null; // Clear external subtitle
                    Toast.makeText(VideoPlayerActivity.this, "Subtitles disabled", Toast.LENGTH_SHORT).show();
                    break;
                case SubtitleOption.TYPE_EXTERNAL:
                    // Show file picker for external subtitle
                    showExternalSubtitlePicker();
                    break;
                case SubtitleOption.TYPE_EMBEDDED:
                    enableEmbeddedSubtitle(selectedOption, trackSelector);
                    uriSubtitle = null; // Clear external subtitle
                    Toast.makeText(VideoPlayerActivity.this, "Subtitle: " + selectedOption.name, Toast.LENGTH_SHORT).show();
                    break;
            }
            dialog.dismiss();

            // Auto-hide controls after selection
            handler.postDelayed(hideControlsRunnable, 3000);
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private int findCurrentlySelectedSubtitleIndex(List<SubtitleOption> subtitleOptions, DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.Parameters parameters = trackSelector.getParameters();
        // Check if external subtitle is loaded
        if (uriSubtitle != null) {
            // Find the "Select from Device" option (index 1)
            for (int i = 0; i < subtitleOptions.size(); i++) {
                if (subtitleOptions.get(i).type == SubtitleOption.TYPE_EXTERNAL) {
                    return i;
                }
            }
        }
        // Check for embedded subtitles
        for (int i = 0; i < subtitleOptions.size(); i++) {
            SubtitleOption option = subtitleOptions.get(i);

            if (option.type == SubtitleOption.TYPE_EMBEDDED) {
                TrackGroupArray trackGroups = trackSelector.getCurrentMappedTrackInfo().getTrackGroups(option.rendererIndex);
                DefaultTrackSelector.SelectionOverride override = parameters.getSelectionOverride(option.rendererIndex, trackGroups);

                if (override != null && override.groupIndex == option.groupIndex && override.containsTrack(option.trackIndex)) {
                    return i; // This embedded subtitle is currently selected
                }
            }
        }
        // Default to "No Subtitle" (index 0)
        return 0;
    }

    private void enableEmbeddedSubtitle(SubtitleOption subtitleOption, DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.getParameters().buildUpon();
        // First, enable the text renderer
        parametersBuilder.setRendererDisabled(subtitleOption.rendererIndex, false);
        // Clear any previous subtitle selections
        parametersBuilder.clearSelectionOverrides(subtitleOption.rendererIndex);
        // Set the new subtitle track
        TrackGroupArray trackGroups = trackSelector.getCurrentMappedTrackInfo().getTrackGroups(subtitleOption.rendererIndex);
        parametersBuilder.setSelectionOverride(
                subtitleOption.rendererIndex,
                trackGroups,
                new DefaultTrackSelector.SelectionOverride(subtitleOption.groupIndex, subtitleOption.trackIndex)
        );
        // Clear external subtitle
        uriSubtitle = null;
        trackSelector.setParameters(parametersBuilder);
    }

    private void showExternalSubtitlePicker() {
        dialogProperties.selection_mode = DialogConfigs.SINGLE_MODE;
        dialogProperties.extensions = new String[]{".srt", ".vtt", ".ass", ".ssa", ".sub"};
        dialogProperties.root = new File("/storage/emulated/0");
        filePickerDialog.setProperties(dialogProperties);
        filePickerDialog.show();
        filePickerDialog.setDialogSelectionListener(files -> {
            if (files.length > 0) {
                String path = files[0];
                File file = new File(path);
                uriSubtitle = Uri.parse(file.getAbsolutePath());
                // Disable embedded subtitles when selecting external
                disableAllSubtitles((DefaultTrackSelector) player.getTrackSelector());
                playVideoSubtitle(uriSubtitle);
                String fileName = file.getName();
                Toast.makeText(VideoPlayerActivity.this, "Loaded: " + fileName, Toast.LENGTH_SHORT).show();
            }
        });
    }
}