package com.pawansingh.vp.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bullhead.equalizer.EqualizerFragment;
import com.bullhead.equalizer.Settings;
import com.pawansingh.vp.R;
import com.pawansingh.vp.adapters.PlaybackIconsAdapter;
import com.pawansingh.vp.adapters.VideoFilesAdapter;
import com.pawansingh.vp.models.BrightnessDialog;
import com.pawansingh.vp.models.IconModel;
import com.pawansingh.vp.models.MediaFiles;
import com.pawansingh.vp.models.PlaylistDialog;
import com.pawansingh.vp.models.VolumeDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity implements View.OnClickListener {

    private ArrayList<MediaFiles> mVideoFiles = new ArrayList<>();
    private VideoFilesAdapter videoFilesAdapter;

    private PlayerView playerView;                 // R.id.exoplayer_view
    private TextView title;                        // R.id.video_title
    private ImageView videoBack, lock, unlock, scaling, videoList, videoMore, rotate, audio_track, exoPlay, exoPause;
    private FrameLayout videoContainer;            // R.id.video_container
    private RelativeLayout root;                   // R.id.controller_root
    private LinearLayout zoomContainer;            // R.id.zoom_container
    private TextView zoomLevelText;                // R.id.zoom_level_text
    private ImageButton resetZoomBtn;              // R.id.reset_zoom_btn
    private RecyclerView recyclerViewIcons;        // R.id.recyclerview_icon
    private FrameLayout eqContainer;
    private boolean askedForWriteSettings = false;

    private ImageView nextButton, previousButton;
    private TextView vol_text, brt_text;
    private ProgressBar vol_progress, brt_progress;
    private LinearLayout vol_progress_container, vol_text_container, brt_progress_container, brt_text_container;
    private ImageView vol_icon, brt_icon;
    private RelativeLayout double_tap_playpause;

    private ExoPlayer player;
    private final TrackSelectionParameters trackSelectionParameters = TrackSelectionParameters.DEFAULT;

    private android.view.ScaleGestureDetector scaleGestureDetector;
    private android.view.GestureDetector gestureDetector;
    private float scaleFactor = 1f;
    private float translationX = 0f, translationY = 0f;
    private boolean isZoomed = false;
    private int activePointerCount = 0;
    private View videoSurfaceView;

    private final ArrayList<IconModel> iconModelsArrayList = new ArrayList<>();
    private PlaybackIconsAdapter playbackIconsAdapter;
    private boolean expand = false;
    private View nightMode;
    private boolean mute = false;

    private int position;
    private String videoTitle;
    private Window windowRef;
    private boolean dark = false;
    private PlaybackParameters playbackParameters;
    private float speed = 1f;
    private AudioManager audioManager;
    private Uri externalSubtitleUri = null;
    private PictureInPictureParams.Builder pictureInPictureParamsBuilder;
    private final boolean isInPip = false;

    private final Handler handler = new Handler();
    private final Runnable hideControlsRunnable = () -> {
        if (playerView != null && playerView.isControllerFullyVisible()) {
            playerView.hideController();
            hideSystemUI();
        }
    };

    private boolean startSwipe = false;
    private boolean left = false;
    private float baseX = 0f, baseY = 0f;
    private long diffX = 0, diffY = 0;
    private int device_width = 0, device_height = 0;
    private int brightness = 0;
    private int mediaVolume = 0;

    public static final int MINIMUM_DISTANCE = 100;
    private static final int REQ_PICK_SUBTITLE = 5001;
    private static final int REQ_DELETE_VIDEO = 108;
    private int pendingDeletePos = -1;

    private static final String TAG = "VideoPlayerActivity";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        windowRef = getWindow();
        WindowCompat.setDecorFitsSystemWindows(windowRef, false);
        windowRef.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            windowRef.setStatusBarColor(Color.TRANSPARENT);
        }
        setFullScreenImmediately();

        position = getIntent().getIntExtra("position", 0);
        videoTitle = getIntent().getStringExtra("video_title");
        registerReceiver(pipReceiver, new IntentFilter("ACTION_TOGGLE_PLAY_PAUSE"));

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initViews();
        View gestureOverlay = findViewById(R.id.swipe_gestures);
        if (gestureOverlay != null) {
            gestureOverlay.bringToFront();
        }
        setupWindowInsets();
        setupGestureDetectors();
        preparePlaybackIconList();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            ArrayList<MediaFiles> list = extras.getParcelableArrayList("videoArrayList");
            if (list != null) mVideoFiles = list;
        }

        title.setText(videoTitle != null ? videoTitle : "");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        device_width = displayMetrics.widthPixels;
        device_height = displayMetrics.heightPixels;
    }

    private void initViews() {
        playerView = findViewById(R.id.exoplayer_view);
        title = findViewById(R.id.video_title);

        videoBack = findViewById(R.id.video_back);
        lock = findViewById(R.id.lock);
        unlock = findViewById(R.id.unlock);
        scaling = findViewById(R.id.scaling);
        videoList = findViewById(R.id.video_list);
        videoMore = findViewById(R.id.video_more);
        rotate = findViewById(R.id.rotate);
        audio_track = findViewById(R.id.audio_track);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mVideoFiles = extras.getParcelableArrayList("videoArrayList");
        }
        videoFilesAdapter = new VideoFilesAdapter(mVideoFiles, this, 0);
        videoFilesAdapter.setOnVideoActionListener(new VideoFilesAdapter.OnVideoActionListener() {
            @Override
            public void onDeleteRequested(MediaFiles video, int pos) {
                deleteVideoFromPlayer(video, pos);
            }

            @Override
            public void onRenameRequested(MediaFiles video, int pos, String currentName) {
                renameVideo(video, pos, currentName);
            }
        });

        nightMode = findViewById(R.id.night_mode);

        videoContainer = findViewById(R.id.video_container);
        root = findViewById(R.id.controller_root);

        zoomContainer = findViewById(R.id.zoom_container);
        zoomLevelText = findViewById(R.id.zoom_level_text);
        resetZoomBtn = findViewById(R.id.reset_zoom_btn);
        resetZoomBtn.setOnClickListener(v -> resetZoom());
        zoomContainer.setVisibility(View.GONE);
        resetZoomBtn.setVisibility(View.GONE);

        recyclerViewIcons = findViewById(R.id.recyclerview_icon);
        eqContainer = findViewById(R.id.eqFrame);

        nextButton = findViewById(R.id.exo_next);
        previousButton = findViewById(R.id.exo_prev);

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

        videoBack.setOnClickListener(this);
        lock.setOnClickListener(this);
        unlock.setOnClickListener(this);
        videoList.setOnClickListener(v -> {
            PlaylistDialog playlistDialog = new PlaylistDialog(mVideoFiles, videoFilesAdapter);
            playlistDialog.show(getSupportFragmentManager(), "playlist");
        });
        videoMore.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        previousButton.setOnClickListener(this);
        scaling.setOnClickListener(v -> cycleResizeMode());
        rotate.setOnClickListener(v -> toggleRotation());
        audio_track.setOnClickListener(v -> showAudioTrackSelector());

        if (playerView != null) {
            playerView.setControllerHideOnTouch(false);
            playerView.setControllerAutoShow(false);
            playerView.setControllerShowTimeoutMs(0);
            playerView.setUseController(true);
            playerView.setUseArtwork(false);
        }
        exoPlay = findViewById(R.id.exo_play);
        exoPause = findViewById(R.id.exo_pause);

        exoPlay.setOnClickListener(v -> {
            if (player == null) return;
            player.setPlayWhenReady(true);
            handlePlayPauseState(true);
        });

        exoPause.setOnClickListener(v -> {
            if (player == null) return;
            player.setPlayWhenReady(false);
            handlePlayPauseState(false);
        });

    }

    private void updatePlayPauseButtons(boolean isPlaying) {
        if (exoPlay == null || exoPause == null) return;
        if (isPlaying) {
            exoPlay.setVisibility(View.GONE);
            exoPause.setVisibility(View.VISIBLE);
        } else {
            exoPlay.setVisibility(View.VISIBLE);
            exoPause.setVisibility(View.GONE);
        }
    }

    private void setupWindowInsets() {
        View rootLayout = findViewById(R.id.root_main);
        if (rootLayout == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (view, windowInsets) -> {
            Insets sys = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            View toolbar = view.findViewById(R.id.toolbar);
            if (toolbar != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                lp.topMargin = sys.top;
                toolbar.setLayoutParams(lp);
            }
            View controls = view.findViewById(R.id.custom_controls_container);
            if (controls != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) controls.getLayoutParams();
                lp.bottomMargin = sys.bottom;
                controls.setLayoutParams(lp);
            }
            View videoContainer = view.findViewById(R.id.video_container);
            if (videoContainer != null) videoContainer.setPadding(0, 0, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupGestureDetectors() {
        scaleGestureDetector = new android.view.ScaleGestureDetector(this, new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private final float MIN_SCALE = 1f;
            private final float MAX_SCALE = 4f;
            private final float EPS = 1e-3f;

            @Override
            public boolean onScale(android.view.ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                scaleFactor *= factor;
                scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                translationX += (focusX - playerView.getWidth() / 2f) * (1 - 1f / scaleFactor);
                translationY += (focusY - playerView.getHeight() / 2f) * (1 - 1f / scaleFactor);

                if (scaleFactor <= MIN_SCALE + EPS) {
                    scaleFactor = MIN_SCALE;
                    translationX = 0f;
                    translationY = 0f;
                }

                isZoomed = scaleFactor > 1.02f;
                applyTransform();
                updateZoomUI();
                return true;
            }
        });

        gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControlsImmediate();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float x = e.getX();
                float oneThird = device_width / 3f;
                if (x < oneThird) seekBackward();
                else if (x > oneThird * 2) seekForward();
                else togglePlayPause();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!isZoomed) return false;
                if (activePointerCount < 2) return false;
                translationX -= distanceX;
                translationY -= distanceY;
                applyTransform();
                return true;
            }
        });

        if (playerView != null) {
            playerView.setOnTouchListener((v, event) -> {
                activePointerCount = event.getPointerCount();
                if (scaleGestureDetector != null) scaleGestureDetector.onTouchEvent(event);
                if (gestureDetector != null) gestureDetector.onTouchEvent(event);

                if (activePointerCount < 2) {
                    handleSingleFingerTouch(event);
                }
                return true;
            });
        }
    }

    private void createPlayerIfNeeded() {
        if (player != null) return;
        player = new ExoPlayer.Builder(this).build();
        if (playerView != null) playerView.setPlayer(player);
        if (speed != 1f) player.setPlaybackParameters(new PlaybackParameters(speed));
        attachPlayerListeners();
    }

    private void playVideoFromPath() {
        if (mVideoFiles == null || mVideoFiles.isEmpty()) return;
        createPlayerIfNeeded();

        List<MediaItem> items = new ArrayList<>();
        for (MediaFiles mf : mVideoFiles) {
            if (mf == null || mf.getPath() == null) continue;
            Uri uri = Uri.parse(mf.getPath());
            MediaItem item = new MediaItem.Builder()
                    .setUri(uri)
                    .setMimeType(determineMimeTypeFromPath(mf.getPath()))
                    .setTag(mf)
                    .build();
            items.add(item);
        }
        if (items.isEmpty()) return;

        try {
            boolean sameList = player.getMediaItemCount() == items.size();
            if (sameList) {
                for (int i = 0; i < items.size(); i++) {
                    if (!player.getMediaItemAt(i).localConfiguration.uri.equals(items.get(i).localConfiguration.uri)) {
                        sameList = false;
                        break;
                    }
                }
            }
            if (!sameList) {
                player.setMediaItems(items, false);
            }
        } catch (Exception e) {
            player.setMediaItems(items, false);
        }

        player.prepare();
        reattachSurfaceToPlayer();

        if (position < 0) position = 0;
        if (position >= player.getMediaItemCount()) position = player.getMediaItemCount() - 1;
        player.seekTo(position, C.TIME_UNSET);
        player.setPlayWhenReady(true);
    }

    private void playVideoFromUri(Uri uri) {
        if (uri == null) return;
        createPlayerIfNeeded();
        MediaItem item = new MediaItem.Builder()
                .setUri(uri)
                .setMimeType(determineMimeTypeFromPath(uri.toString()))
                .build();
        player.setMediaItem(item, C.TIME_UNSET);
        player.prepare();
        reattachSurfaceToPlayer();
        player.seekTo(position, C.TIME_UNSET);
        player.setPlayWhenReady(true);
    }

    private void reattachSurfaceToPlayer() {
        if (player == null || playerView == null) return;
        try {
            View surfaceView = playerView.getVideoSurfaceView();
            if (surfaceView == null) return;
            try {
                if (surfaceView instanceof SurfaceView) {
                    player.setVideoSurfaceView((SurfaceView) surfaceView);
                } else if (surfaceView instanceof TextureView) {
                    player.setVideoTextureView((TextureView) surfaceView);
                }
            } catch (Throwable t) {
                // Some devices/versions may not support direct setters; PlayerView usually manages attachment.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String determineMimeTypeFromPath(String path) {
        if (path == null) return null;
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".mp4") || lower.endsWith(".m4v")) return MimeTypes.VIDEO_MP4;
        if (lower.endsWith(".mkv")) return MimeTypes.VIDEO_WEBM;
        if (lower.endsWith(".webm")) return MimeTypes.VIDEO_WEBM;
        if (lower.endsWith(".ts")) return MimeTypes.VIDEO_MP2T;
        if (lower.endsWith(".mp3")) return MimeTypes.AUDIO_MPEG;
        return MimeTypes.VIDEO_UNKNOWN;
    }

    private void attachPlayerListeners() {
        if (player == null) return;

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    boolean isPlaying = player.getPlayWhenReady();
                    handlePlayPauseState(isPlaying);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Toast.makeText(VideoPlayerActivity.this, "Playback error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "PlaybackException", error);
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                handlePlayPauseState(isPlaying);
            }
        });
    }

    private void showAudioTrackSelector() {
        if (player == null) {
            Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        Tracks tracks = player.getCurrentTracks();
        List<TrackInfoItem> audioItems = new ArrayList<>();
        int selectedIndex = -1;
        for (int gi = 0; gi < tracks.getGroups().size(); gi++) {
            Tracks.Group group = tracks.getGroups().get(gi);
            if (group.getType() == C.TRACK_TYPE_AUDIO) {
                for (int ti = 0; ti < group.length; ti++) {
                    Format format = group.getTrackFormat(ti);
                    TrackInfoItem item = new TrackInfoItem(gi, ti, format);
                    audioItems.add(item);
                    if (group.isTrackSelected(ti)) {
                        selectedIndex = audioItems.size() - 1;
                    }
                }
            }
        }
        if (audioItems.isEmpty()) {
            Toast.makeText(this, "No audio tracks", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[audioItems.size()];
        for (int i = 0; i < audioItems.size(); i++) {
            names[i] = audioItems.get(i).getDisplayName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Audio Language");
        builder.setSingleChoiceItems(names, selectedIndex, (dialog, which) -> {
            applyAudioTrack(audioItems.get(which));
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void applyAudioTrack(TrackInfoItem item) {
        if (player == null) return;
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters().buildUpon();
        builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false);
        builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO);
        Tracks.Group group = player.getCurrentTracks().getGroups().get(item.groupIndex);
        TrackSelectionOverride override = new TrackSelectionOverride(group.getMediaTrackGroup(),
                Collections.singletonList(item.trackIndex)
        );
        builder.addOverride(override);
        player.setTrackSelectionParameters(builder.build());
        Toast.makeText(this, "Switched to " + item.getDisplayName(), Toast.LENGTH_SHORT).show();
    }

    static class TrackInfoItem {
        int groupIndex;
        int trackIndex;
        Format format;

        TrackInfoItem(int groupIndex, int trackIndex, Format format) {
            this.groupIndex = groupIndex;
            this.trackIndex = trackIndex;
            this.format = format;
        }

        String getDisplayName() {
            return getLanguageName(format.language);
        }

    }
    private static String getLanguageName(String code) {
        if (code == null || code.trim().isEmpty()) return "Unknown";

        try {
            Locale loc = new Locale(code);
            String name = loc.getDisplayLanguage(Locale.ENGLISH);
            if (name == null || name.trim().isEmpty()) return code.toUpperCase();
            return name;
        } catch (Exception e) {
            return code.toUpperCase();
        }
    }

    private void showSubtitleDialog() {
        if (player == null) {
            Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> options = new ArrayList<>();
        List<SubtitleItem> embedded = getEmbeddedSubtitles();
        options.add("No Subtitle");
        options.add("Select External Subtitle");
        for (SubtitleItem s : embedded) options.add(s.label);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Select Subtitle");
        b.setItems(options.toArray(new String[0]), (d, w) -> {
            if (w == 0) disableSubtitles();
            else if (w == 1) pickExternalSubtitle();
            else applyEmbeddedSubtitle(embedded.get(w - 2));
        });
        b.show();
    }

    private List<SubtitleItem> getEmbeddedSubtitles() {
        List<SubtitleItem> list = new ArrayList<>();
        Tracks tracks = player.getCurrentTracks();
        for (int gi = 0; gi < tracks.getGroups().size(); gi++) {
            Tracks.Group g = tracks.getGroups().get(gi);
            if (g.getType() == C.TRACK_TYPE_TEXT) {
                for (int ti = 0; ti < g.length; ti++) {
                    Format f = g.getTrackFormat(ti);
                    String lang = f.language != null ? f.language : "Unknown";
                    list.add(new SubtitleItem(gi, ti, lang));
                }
            }
        }
        return list;
    }

    private void applyEmbeddedSubtitle(SubtitleItem item) {
        if (player == null) return;
        TrackSelectionParameters.Builder b = player.getTrackSelectionParameters().buildUpon();
        b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false);
        b.clearOverridesOfType(C.TRACK_TYPE_TEXT);
        b.addOverride(new TrackSelectionOverride(player.getCurrentTracks().getGroups().get(item.groupIndex).getMediaTrackGroup(), (List<Integer>) Collections.singleton(item.trackIndex)));
        player.setTrackSelectionParameters(b.build());
        externalSubtitleUri = null;
        Toast.makeText(this, "Subtitle: " + item.label, Toast.LENGTH_SHORT).show();
    }

    private void disableSubtitles() {
        if (player == null) return;
        TrackSelectionParameters.Builder b = player.getTrackSelectionParameters().buildUpon();
        b.clearOverridesOfType(C.TRACK_TYPE_TEXT);
        b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true);
        player.setTrackSelectionParameters(b.build());
        externalSubtitleUri = null;
        Toast.makeText(this, "Subtitles off", Toast.LENGTH_SHORT).show();
    }

    static class SubtitleItem {
        int groupIndex, trackIndex;
        String label;

        SubtitleItem(int g, int t, String l) {
            groupIndex = g;
            trackIndex = t;
            label = l;
        }
    }

    private void pickExternalSubtitle() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, REQ_PICK_SUBTITLE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_SUBTITLE && resultCode == RESULT_OK && data != null) {
            externalSubtitleUri = data.getData();
            attachExternalSubtitle(externalSubtitleUri);
        } else if (requestCode == REQ_DELETE_VIDEO) {
            if (resultCode == RESULT_OK) {
                if (pendingDeletePos != -1) handleItemDeleted(pendingDeletePos);
            } else {
                Toast.makeText(this, "Delete cancelled", Toast.LENGTH_SHORT).show();
            }
            pendingDeletePos = -1;
        }
    }

    private void attachExternalSubtitle(Uri uri) {
        if (player == null || uri == null) return;

        TrackSelectionParameters.Builder b = player.getTrackSelectionParameters().buildUpon();
        b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false);
        player.setTrackSelectionParameters(b.build());

        String mime = MimeTypes.APPLICATION_SUBRIP;
        MediaItem.SubtitleConfiguration sub = new MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mime).setLanguage("und")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build();

        int idx = player.getCurrentMediaItemIndex();
        MediaItem old = player.getMediaItemAt(idx);
        long pos = player.getCurrentPosition();
        MediaItem newItem = old.buildUpon().setSubtitleConfigurations(Collections.singletonList(sub)).build();
        player.replaceMediaItem(idx, newItem);
        player.prepare();
        player.seekTo(pos);
        Toast.makeText(this, "External subtitle attached", Toast.LENGTH_SHORT).show();
    }

    private void applyTransform() {
        View videoSurface = playerView.getVideoSurfaceView();
        if (videoSurface == null) return;
        float containerWidth = playerView.getWidth();
        float containerHeight = playerView.getHeight();
        if (containerWidth == 0 || containerHeight == 0) return;

        videoSurface.setPivotX(containerWidth / 2f);
        videoSurface.setPivotY(containerHeight / 2f);
        float maxTransX = Math.max(0f, (scaleFactor - 1f) * containerWidth / 2f);
        float maxTransY = Math.max(0f, (scaleFactor - 1f) * containerHeight / 2f);
        translationX = clamp(translationX, -maxTransX, maxTransX);
        translationY = clamp(translationY, -maxTransY, maxTransY);

        videoSurface.setScaleX(scaleFactor);
        videoSurface.setScaleY(scaleFactor);
        videoSurface.setTranslationX(translationX);
        videoSurface.setTranslationY(translationY);

        FrameLayout container = findViewById(R.id.video_container);
        if (container != null) {
            container.setPadding(0, 0, 0, 0);
            container.setScaleX(1f);
            container.setScaleY(1f);
            container.setTranslationX(0f);
            container.setTranslationY(0f);
        }
        playerView.setScaleX(1f);
        playerView.setScaleY(1f);
        playerView.setTranslationX(0f);
        playerView.setTranslationY(0f);
        isZoomed = scaleFactor > 1.02f;
        updateZoomUI();
    }

    private float clamp(float v, float a, float b) {
        if (v < a) return a;
        if (v > b) return b;
        return v;
    }

    private void resetZoom() {
        View videoSurface = playerView.getVideoSurfaceView();
        if (videoSurface == null) return;
        videoSurface.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f).setDuration(200).start();
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
            zoomContainer.setVisibility(View.GONE);
            resetZoomBtn.setVisibility(View.GONE);
        }
    }

    private void seekBackward() {
        if (player == null) return;
        long pos = player.getCurrentPosition();
        player.seekTo(Math.max(0, pos - 10000));
        showSeekFeedback("-10s", R.drawable.ic_rewind);
    }

    private void seekForward() {
        if (player == null) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        player.seekTo(Math.min(dur, pos + 10000));
        showSeekFeedback("+10s", R.drawable.ic_forward);
    }

    private void togglePlayPause() {
        if (player == null) return;
        boolean playing = player.getPlayWhenReady();
        player.setPlayWhenReady(!playing);
        handlePlayPauseState(!playing);
        showPlayPauseFeedback(!playing);
    }

    private void handlePlayPauseState(boolean isPlaying) {
        updatePlayPauseButtons(isPlaying);
        if (!isPlaying) {
            playerView.showController();
            handler.removeCallbacks(hideControlsRunnable);
        } else {
            handler.postDelayed(hideControlsRunnable, 3000);
        }
    }

    private void showSeekFeedback(String text, int iconRes) {
        removeSeekFeedback();
        FrameLayout feedbackLayout = new FrameLayout(this);
        feedbackLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        feedbackLayout.setBackgroundResource(R.drawable.seek_feedback_bg);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.addView(icon, new LinearLayout.LayoutParams(120, 120));
        content.addView(tv);
        feedbackLayout.addView(content);

        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(feedbackLayout);
        feedbackLayout.setAlpha(0f);
        feedbackLayout.animate().alpha(1f).setDuration(200).withEndAction(() ->
                handler.postDelayed(() -> feedbackLayout.animate().alpha(0f).setDuration(300).withEndAction(() -> removeSpecificView(feedbackLayout)).start(), 800)
        ).start();
    }

    private void showPlayPauseFeedback(boolean isPlaying) {
        removeSeekFeedback();
        FrameLayout feedbackLayout = new FrameLayout(this);
        feedbackLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        feedbackLayout.setBackgroundResource(R.drawable.seek_feedback_bg);

        ImageView icon = new ImageView(this);
        icon.setImageResource(isPlaying ? R.drawable.ic_play : R.drawable.ic_pause);
        icon.setColorFilter(Color.WHITE);

        TextView tv = new TextView(this);
        tv.setText(isPlaying ? "Playing" : "Paused");
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.addView(icon, new LinearLayout.LayoutParams(140, 140));
        content.addView(tv);
        feedbackLayout.addView(content);

        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(feedbackLayout);
        feedbackLayout.setAlpha(0f);
        feedbackLayout.animate().alpha(1f).setDuration(200).withEndAction(() ->
                handler.postDelayed(() -> feedbackLayout.animate().alpha(0f).setDuration(300).withEndAction(() -> removeSpecificView(feedbackLayout)).start(), 800)
        ).start();
    }

    private void removeSpecificView(View v) {
        if (v.getParent() != null) ((ViewGroup) v.getParent()).removeView(v);
    }

    private void removeSeekFeedback() {
        ViewGroup rootView = findViewById(android.R.id.content);
        for (int i = rootView.getChildCount() - 1; i >= 0; i--) {
            View child = rootView.getChildAt(i);
            if (child instanceof FrameLayout && child.getId() != R.id.controller_root) {
                rootView.removeView(child);
            }
        }
    }

    private void preparePlaybackIconList() {
        iconModelsArrayList.clear();
        iconModelsArrayList.add(new IconModel(R.drawable.ic_speed, "Speed"));
        iconModelsArrayList.add(new IconModel(R.drawable.ic_equalizer, "Equalizer"));
        iconModelsArrayList.add(new IconModel(R.drawable.ic_pip_mode, "Pop-Up"));
        iconModelsArrayList.add(new IconModel(R.drawable.ic_subtitle, "Subtitle"));
        iconModelsArrayList.add(new IconModel(R.drawable.ic_right, "")); // expand

        playbackIconsAdapter = new PlaybackIconsAdapter(iconModelsArrayList, this);
        recyclerViewIcons.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        recyclerViewIcons.setAdapter(playbackIconsAdapter);
        playbackIconsAdapter.notifyDataSetChanged();

        playbackIconsAdapter.setOnItemClickListener(position -> {
            handler.removeCallbacks(hideControlsRunnable);

            if ((!expand && position == 4) || (expand && position == 9)) {
                if (expand) {
                    iconModelsArrayList.clear();
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_speed, "Speed"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_equalizer, "Equalizer"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_pip_mode, "Pop-Up"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_subtitle, "Subtitle"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_right, ""));
                    expand = false;
                    playbackIconsAdapter.notifyDataSetChanged();
                    handler.postDelayed(hideControlsRunnable, 3000);
                } else {
                    iconModelsArrayList.clear();
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_speed, "Speed"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_equalizer, "Equalizer"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_pip_mode, "Pop-Up"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_subtitle, "Subtitle"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_night_mode, "Night"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_rotate, "Rotate"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_volume_off, "Mute"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_volume, "Volume"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_brightness, "Brightness"));
                    iconModelsArrayList.add(new IconModel(R.drawable.ic_left, "")); // collapse
                    expand = true;
                    playbackIconsAdapter.notifyDataSetChanged();
                }
                return;
            }

            if (position == 0) {
                showSpeedDialog();
            } else if (position == 1) showEqualizer();
            else if (position == 2) enterPipMode();
            else if (position == 3) showSubtitleDialog();
            else if (expand && position == 4) toggleNightMode();
            else if (expand && position == 5) toggleRotation();
            else if (expand && position == 6) toggleMute();
            else if (expand && position == 7) showVolumeDialog();
            else if (expand && position == 8) showBrightnessDialog();
        });
    }

    private void toggleNightMode() {
        if (nightMode == null) return;
        if (dark) {
            nightMode.setVisibility(View.GONE);
            dark = false;
        } else {
            nightMode.setVisibility(View.VISIBLE);
            dark = true;
        }
        playbackIconsAdapter.notifyDataSetChanged();
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void toggleMute() {
        if (player == null) return;
        if (mute) {
            player.setVolume(1f);
            mute = false;
        } else {
            player.setVolume(0f);
            mute = true;
        }
        playbackIconsAdapter.notifyDataSetChanged();
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void showVolumeDialog() {
        VolumeDialog volumeDialog = new VolumeDialog();
        volumeDialog.show(getSupportFragmentManager(), "volume_dialog");
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void showBrightnessDialog() {
        BrightnessDialog brightnessDialog = new BrightnessDialog();
        brightnessDialog.show(getSupportFragmentManager(), "brightness_dialog");
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void showSpeedDialog() {
        final String[] items = {"0.5x", "1x", "1.25x", "1.5x", "2x"};
        final float[] speeds = {0.5f, 1f, 1.25f, 1.5f, 2f};
        int checkedIndex = 1; // default 1x
        for (int i = 0; i < speeds.length; i++) {
            if (Math.abs(speed - speeds[i]) < 0.01f) {
                checkedIndex = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Playback Speed");
        builder.setSingleChoiceItems(items, checkedIndex, (dialog, which) -> {
            speed = speeds[which];
            if (player != null) {
                player.setPlaybackParameters(new PlaybackParameters(speed));
            }

            dialog.dismiss();
            handler.postDelayed(hideControlsRunnable, 3000);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
            Intent intent = new Intent("ACTION_TOGGLE_PLAY_PAUSE");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            );

            RemoteAction action = new RemoteAction(
                    Icon.createWithResource(this, R.drawable.ic_play), "Play/Pause",
                    "Toggle Play Pause", pendingIntent
            );
            PictureInPictureParams params =
                    new PictureInPictureParams.Builder()
                            .setActions(Collections.singletonList(action)).build();
            enterPictureInPictureMode(params);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPictureInPictureModeChanged(boolean isInPip, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPip, newConfig);

        if (isInPip) {
            playerView.hideController();
        } else {
            playerView.showController();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode()) {
            enterPipMode();
        }
    }

    private void cycleResizeMode() {
        if (playerView == null) return;
        int mode = playerView.getResizeMode();
        if (mode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            scaling.setImageResource(R.drawable.zoom);
            Toast.makeText(this, "Zoom", Toast.LENGTH_SHORT).show();
        } else if (mode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            scaling.setImageResource(R.drawable.fullscreen);
            Toast.makeText(this, "Full screen", Toast.LENGTH_SHORT).show();
        } else {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            scaling.setImageResource(R.drawable.fit);
            Toast.makeText(this, "Fit", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleRotation() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private void handleSingleFingerTouch(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startSwipe = true;
                baseX = event.getX();
                baseY = event.getY();
                left = baseX < device_width / 2f;
                break;
            case MotionEvent.ACTION_MOVE:
                diffX = (long) Math.ceil(event.getX() - baseX);
                diffY = (long) Math.ceil(event.getY() - baseY);
                double brightnessSpeed = 0.01;
                if (Math.abs(diffY) > MINIMUM_DISTANCE) {
                    if (Math.abs(diffY) > Math.abs(diffX)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            boolean canWrite = android.provider.Settings.System.canWrite(getApplicationContext());
                            if (canWrite) {
                                if (left) {
                                    try {
                                        android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                                        brightness = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
                                    } catch (Exception ignore) {
                                    }
                                    int new_brightness = (int) (brightness - (diffY * brightnessSpeed));
                                    new_brightness = Math.max(1, Math.min(250, new_brightness));
                                    double brt_percentage = Math.ceil((((double) new_brightness / (double) 250) * 100d));
                                    brt_progress_container.setVisibility(View.VISIBLE);
                                    brt_text_container.setVisibility(View.VISIBLE);
                                    brt_progress_container.bringToFront();
                                    brt_text_container.bringToFront();
                                    brt_progress.setProgress((int) brt_percentage);
                                    if (brt_percentage < 30)
                                        brt_icon.setImageResource(R.drawable.ic_brightness_low);
                                    else if (brt_percentage < 80)
                                        brt_icon.setImageResource(R.drawable.ic_brightness_moderate);
                                    else brt_icon.setImageResource(R.drawable.ic_brightness);
                                    brt_text.setText(" " + (int) brt_percentage + "%");
                                    android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, new_brightness);
                                    WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                                    layoutParams.screenBrightness = brightness / (float) 255;
                                    getWindow().setAttributes(layoutParams);
                                } else {
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
                                    } else vol_icon.setImageResource(R.drawable.ic_volume);
                                    vol_progress_container.setVisibility(View.VISIBLE);
                                    vol_text_container.setVisibility(View.VISIBLE);
                                    vol_progress_container.bringToFront();
                                    vol_text_container.bringToFront();
                                    vol_progress.setProgress((int) volPercentage);
                                }
                            } else {
                                if(!askedForWriteSettings){
                                    askedForWriteSettings = true;
                                    Toast.makeText(this, "Allow write settings for swipe controls", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    startActivityForResult(intent, 111);
                                }
                                return;
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                startSwipe = false;
                vol_progress_container.setVisibility(View.GONE);
                brt_progress_container.setVisibility(View.GONE);
                vol_text_container.setVisibility(View.GONE);
                brt_text_container.setVisibility(View.GONE);
                break;
        }
    }

    private int getNewMediaVolume(int maxVol, double brightnessSpeed) {
        double cal = (double) diffY * ((double) maxVol / ((double) (device_height * 2) - brightnessSpeed));
        int newMediaVolume = mediaVolume - (int) cal;
        if (newMediaVolume > maxVol) newMediaVolume = maxVol;
        else if (newMediaVolume < 1) newMediaVolume = 0;
        return newMediaVolume;
    }

    private void toggleControlsImmediate() {
        handler.removeCallbacks(hideControlsRunnable);
        if (playerView.isControllerFullyVisible()) {
            playerView.hideController();
        } else {
            playerView.showController();
            handler.postDelayed(hideControlsRunnable, 3000);
        }
    }

    private void setFullScreenImmediately() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void showSystemUI() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) controller.show(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    protected void onStart() {
        super.onStart();
        createPlayerIfNeeded();
        Uri dataUri = getIntent().getData();
        if (dataUri != null) playVideoFromUri(dataUri);
        else if (mVideoFiles != null && !mVideoFiles.isEmpty()) playVideoFromPath();
    }

    @Override
    protected void onResume() {
        super.onResume();
        askedForWriteSettings = false;
        hideSystemUI();
        if (player != null) player.setPlayWhenReady(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                !isInPictureInPictureMode()) {
            if (player != null) {
                player.setPlayWhenReady(false);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                isInPictureInPictureMode()) {
            return;
        }
        if (isFinishing()) {
            if (player != null) {
                player.release();
                player = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(pipReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.video_back) {
            if (player != null) player.release();
            finish();
        } else if (id == R.id.lock) {
            root.setVisibility(View.VISIBLE);
            lock.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Unlocked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.unlock) {
            root.setVisibility(View.INVISIBLE);
            lock.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Locked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.video_list) {
            PlaylistDialog playlistDialog = new PlaylistDialog(mVideoFiles, videoFilesAdapter);
            playlistDialog.show(getSupportFragmentManager(), playlistDialog.getTag());
        } else if (id == R.id.video_more) {
            showPlayerPopupMenu();
        } else if (id == R.id.exo_next) {
            if (position < mVideoFiles.size() - 1) {
                position++;
                playCurrentIndex();
            } else {
                Toast.makeText(this, "No next video", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.exo_prev) {
            if (position > 0) {
                position--;
                playCurrentIndex();
            } else {
                Toast.makeText(this, "No previous video", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPlayerPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(this, videoMore);
        popupMenu.inflate(R.menu.player_menu);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.next) {
                if (player != null) player.seekToNext();
            } else if (itemId == R.id.send) {
                Uri videoUri = getIntent().getData();
                Uri uri = null;
                if (videoUri != null) uri = videoUri;
                else if (mVideoFiles != null && position < mVideoFiles.size())
                    uri = Uri.parse(mVideoFiles.get(position).getPath());
                if (uri != null) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("video/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(shareIntent, "Share video via?"));
                }
            } else if (itemId == R.id.properties) {
                if (mVideoFiles != null && position < mVideoFiles.size()) {
                    MediaFiles mf = mVideoFiles.get(position);
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mf.getPath());
                    String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String msg = "File: " + mf.getDisplayName() + "\nPath: " + mf.getPath() + "\nSize: " + mf.getSize() + "\nResolution: " + width + "x" + height;
                    new AlertDialog.Builder(this).setTitle("Properties").setMessage(msg).setPositiveButton("OK", (d, w) -> d.dismiss()).show();
                }
            } else if (itemId == R.id.delete) {
                AlertDialog.Builder alertDialogDelete = new AlertDialog.Builder(VideoPlayerActivity.this);
                alertDialogDelete.setTitle("Delete");
                alertDialogDelete.setMessage("Do you want to delete this video?");
                alertDialogDelete.setPositiveButton("Delete", (dialogInterface, i) -> {
                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            Long.parseLong(mVideoFiles.get(position).getId())
                    );

                    // Android R+ deletion flow
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ArrayList<Uri> uriList = new ArrayList<>();
                        uriList.add(contentUri);
                        pendingDeletePos = position;
                        PendingIntent deleteIntent = MediaStore.createDeleteRequest(getContentResolver(), uriList);
                        try {
                            startIntentSenderForResult(deleteIntent.getIntentSender(), REQ_DELETE_VIDEO, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                            pendingDeletePos = -1;
                            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        int rowsDeleted = getContentResolver().delete(contentUri, null, null);
                        if (rowsDeleted > 0) {
                            handleItemDeleted(position);
                            Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Video is not deleted", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                alertDialogDelete.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
                alertDialogDelete.show();
            } else if (itemId == R.id.subtitle) {
                showSubtitleDialog();
            }
            return true;
        });
        popupMenu.show();
    }

    private final BroadcastReceiver pipReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_TOGGLE_PLAY_PAUSE".equals(intent.getAction())) {
                if (player != null) {
                    boolean isPlaying = player.isPlaying();
                    player.setPlayWhenReady(!isPlaying);
                }
            }
        }
    };

    private void playCurrentIndex() {
        if (mVideoFiles == null || mVideoFiles.size() == 0) return;
        if (position < 0) position = 0;
        if (position >= mVideoFiles.size()) position = mVideoFiles.size() - 1;

        String path = mVideoFiles.get(position).getPath();
        MediaItem item = MediaItem.fromUri(Uri.parse(path));

        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
        }
        int count = player.getMediaItemCount();
        if (count > 0) {
            player.clearMediaItems();
        }
        player.setMediaItem(item);
        player.prepare();
        player.play();
        title.setText(mVideoFiles.get(position).getDisplayName());
    }

    private void handleItemDeleted(int deletedPos) {
        if (mVideoFiles == null || mVideoFiles.isEmpty()) return;
        if (deletedPos < 0 || deletedPos >= mVideoFiles.size()) return;

        if (player != null) {
            player.stop();
        }

        mVideoFiles.remove(deletedPos);
        videoFilesAdapter.notifyItemRemoved(deletedPos);
        videoFilesAdapter.notifyDataSetChanged();
        if (mVideoFiles.isEmpty()) {
            if (player != null) {
                player.release();
                player = null;
            }
            Toast.makeText(this, "Playlist empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (deletedPos < mVideoFiles.size()) {
            position = deletedPos;                 // play item that moved into deleted slot
        } else {
            position = mVideoFiles.size() - 1;     // deleted last → play new last = previous
        }
        playCurrentIndex();
    }

    private void deleteVideoFromPlayer(MediaFiles video, int pos) {

        Uri contentUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                Long.parseLong(video.getId())
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ArrayList<Uri> list = new ArrayList<>();
            list.add(contentUri);

            PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), list);

            try {
                pendingDeletePos = pos;
                startIntentSenderForResult(
                        pendingIntent.getIntentSender(), REQ_DELETE_VIDEO, null, 0, 0, 0
                );
            } catch (Exception e) {
                pendingDeletePos = -1;
                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        int rows = getContentResolver().delete(contentUri, null, null);

        if (rows > 0) {
            handleItemDeleted(pos);
            Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Video is not deleted", Toast.LENGTH_SHORT).show();
        }
    }

    private void renameVideo(MediaFiles video, int pos, String currentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Video");

        final EditText input = new EditText(this);
        input.setText(currentName.substring(0, currentName.lastIndexOf(".")));
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {

            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Invalid name!", Toast.LENGTH_SHORT).show();
                return;
            }
            File file = new File(video.getPath());
            File newFile = new File(file.getParent(), newName + "." + currentName.substring(currentName.lastIndexOf(".") + 1));

            if (file.renameTo(newFile)) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DATA, newFile.getAbsolutePath());
                getContentResolver().update(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        values,
                        MediaStore.Video.Media.DATA + "=?",
                        new String[]{video.getPath()}
                );

                video.setPath(newFile.getAbsolutePath());
                video.setDisplayName(newFile.getName());

                mVideoFiles.set(pos, video);
                videoFilesAdapter.notifyItemChanged(pos);

                if (pos == position) {
                    title.setText(video.getDisplayName());
                }
            } else {
                Toast.makeText(this, "Rename failed!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private ViewGroup getRootView() {
        return findViewById(android.R.id.content);
    }
}