package com.pawansingh.vp.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.pawansingh.vp.R;
import com.pawansingh.vp.adapters.ViewPagerAdapter;
import com.pawansingh.vp.fragments.FoldersFragment;
import com.pawansingh.vp.fragments.VideosFragment;
import com.pawansingh.vp.models.MediaFiles;
import com.pawansingh.vp.newthing.MediaActionHandler;

public class MainActivity extends AppCompatActivity implements MediaActionHandler {

    private static final int FLEXIBLE_UPDATE_REQUEST_CODE = 123;
    private AppUpdateManager appUpdateManager;

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private MaterialToolbar toolbar;
    private ViewPagerAdapter adapter;

    private MediaFiles videoToRename;
    private final int renamePosition = -1;
    private final String newVideoName = "";

    public MediaFiles selectedVideo = null;
    public int selectedPosition = RecyclerView.NO_POSITION;

    VideosFragment vf = new VideosFragment();
    FoldersFragment ff = new FoldersFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, 1);
            } else {
//                initViews();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
//                initViews();
            }
        }
        appUpdateManager = AppUpdateManagerFactory.create(this);
//        checkForAppUpdate();
    }

    @Override
    public void prepareForRename(MediaFiles video, int position, String newName) {

    }
    @Override
    public void prepareForDelete(MediaFiles video, int position) {

    }

//    private void checkForAppUpdate() {
//        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
//
//        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
//                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
//
//                try {
//                    appUpdateManager.startUpdateFlowForResult(
//                            appUpdateInfo,
//                            AppUpdateType.FLEXIBLE,
//                            this,
//                            FLEXIBLE_UPDATE_REQUEST_CODE
//                    );
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        // Listen to update state
//        appUpdateManager.registerListener(installStateUpdatedListener);
//    }
//
//    // Listener to track download and notify user to complete update
//    InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
//        @Override
//        public void onStateUpdate(InstallState state) {
//            if (state.installStatus() == InstallStatus.DOWNLOADED) {
//                // Show a toast or snackbar for user to complete update
//                Snackbar.make(findViewById(android.R.id.content),
//                                "An update has been downloaded.",
//                                Snackbar.LENGTH_INDEFINITE)
//                        .setAction("Restart", view -> appUpdateManager.completeUpdate())
//                        .show();
//            }
//        }
//    };
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        // If update is downloaded but not installed
//        appUpdateManager
//                .getAppUpdateInfo()
//                .addOnSuccessListener(appUpdateInfo -> {
//                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
//                        appUpdateManager.completeUpdate(); // Prompt to restart
//                    }
//                });
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (appUpdateManager != null) {
//            appUpdateManager.unregisterListener(installStateUpdatedListener);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            initViews();
//        }
//    }
//
//    private void initViews() {
//        toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        tabLayout = findViewById(R.id.tabLayout);
//        viewPager = findViewById(R.id.viewPager);
//
//        adapter = new ViewPagerAdapter(this);
//        viewPager.setAdapter(adapter);
//
//        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
//            tab.setText(position == 0 ? "Videos 🎥" : "Folders 📂");
//        }).attach();
//        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
//            @Override
//            public void onTabSelected(TabLayout.Tab tab) {
//                View tabView = tab.getCustomView();
//                if (tabView instanceof TextView) {
//                    ((TextView) tabView).setTypeface(null, Typeface.BOLD);
//                }
//            }
//
//            @Override
//            public void onTabUnselected(TabLayout.Tab tab) {
//                View tabView = tab.getCustomView();
//                if (tabView instanceof TextView) {
//                    ((TextView) tabView).setTypeface(null, Typeface.NORMAL);
//                }
//            }
//
//            @Override
//            public void onTabReselected(TabLayout.Tab tab) {
//            }
//        });
//
//        reduceDragSensitivity(viewPager);
//    }
//
//    private void reduceDragSensitivity(ViewPager2 viewPager2) {
//        try {
//            RecyclerView recyclerView = (RecyclerView) viewPager2.getChildAt(0);
//            Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
//            touchSlopField.setAccessible(true);
//            int touchSlop = touchSlopField.getInt(recyclerView);
//            touchSlopField.setInt(recyclerView, touchSlop * 2); // 3x less sensitive
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.folder_menu, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//
//        int id = item.getItemId();
//        if (id == R.id.rateUs) {
//            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id="
//                    + getApplicationContext().getPackageName());
//            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//            startActivity(intent);
//        } else if (id == R.id.refresh_folders) {
//            finish();
//            startActivity(getIntent());
//        } else if (id == R.id.share_app) {
//            Intent share_intent = new Intent();
//            share_intent.setAction(Intent.ACTION_SEND);
//            share_intent.putExtra(Intent.EXTRA_TEXT, "Check this app via\n" + "https://play.google.com/store/apps/details?id="
//                    + getApplicationContext().getPackageName());
//            share_intent.setType("text/plain");
//            startActivity(Intent.createChooser(share_intent, "Share app via"));
//        }
//        return true;
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == FLEXIBLE_UPDATE_REQUEST_CODE) { // App update
//            if (resultCode != RESULT_OK) {
//                Toast.makeText(this, "Update canceled by user", Toast.LENGTH_SHORT).show();
//            }
//        }
//
//        if (requestCode == 108) { // Delete
//            if (resultCode == RESULT_OK && selectedVideo != null && selectedPosition != RecyclerView.NO_POSITION) {
//                // Remove from UI
//                Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(this, "Video not deleted", Toast.LENGTH_SHORT).show();
//            }
//            selectedVideo = null;
//            selectedPosition = RecyclerView.NO_POSITION;
//        }
//
//        if (requestCode == 109 && resultCode == RESULT_OK && videoToRename != null) {
//            ContentValues values = new ContentValues();
//            String finalName = newVideoName.endsWith(".mp4") ? newVideoName : newVideoName + ".mp4";
//            values.put(MediaStore.Video.Media.DISPLAY_NAME, finalName);
//
//            Uri uri = ContentUris.withAppendedId(
//                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//                    Long.parseLong(videoToRename.id)
//            );
//
//            int rows = getContentResolver().update(uri, values, null, null);
//
//            if (rows > 0) {
//                Toast.makeText(this, "Video renamed successfully", Toast.LENGTH_SHORT).show();
//                videoToRename.displayName = finalName;
//
//                if (vf != null) {
//                    vf.updateRenamedItem(videoToRename, renamePosition);
//                } else if (ff != null) {
//                    ff.updateRenamedItem(videoToRename, renamePosition);
//                }
//
//            } else {
//                Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show();
//            }
//
//            videoToRename = null;
//            newVideoName = "";
//            renamePosition = -1;
//        }
//    }
//
//    @Override
//    public void prepareForRename(MediaFiles video, int position, String newName) {
//        this.videoToRename = video;
//        this.renamePosition = position;
//        this.newVideoName = newName;
//    }
//
//    @Override
//    public void prepareForDelete(MediaFiles video, int position) {
//        selectedVideo = video;
//        selectedPosition = position;
//    }
}