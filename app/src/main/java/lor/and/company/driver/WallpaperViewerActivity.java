package lor.and.company.driver;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import lor.and.company.driver.glideModules.DriveWallpaperContainer;
import lor.and.company.driver.glideModules.ProgressInputStream;
import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.models.Wallpaper;
import lor.and.company.driver.services.WallpaperDownloaderService;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class WallpaperViewerActivity extends AppCompatActivity implements DriveWallpaperContainer.DriveWallpaperListener, WallpaperDownloaderService.DownloadServiceListener {
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */

    private static final int REQUEST_CODE_DOWNLOAD_PERMISSION = 102;


    private static final int UI_ANIMATION_DELAY = 300;

    private final Handler mHideHandler = new Handler();
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            preview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            getWindow().setAttributes(attributes);

            preview.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);
        }
    };

    private FrameLayout contentContainer;

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            contentContainer.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    Context context;

    SubsamplingScaleImageView preview;

    GestureDetector gestureDetector;

    Wallpaper wallpaper;

    ProgressBar previewProgress;

//    Animation animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_viewer);

        context = this;

        preview = findViewById(R.id.previewImage);

        previewProgress = findViewById(R.id.previewProgress);

        wallpaper = getIntent().getParcelableExtra("wallpaper");

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (preview.isReady()) {
                    toggle();
                }
                return true;
            }
        });

        TextView title = findViewById(R.id.collectionName);
        title.setText(new DBHelper.CollectionsDB(context).getCollectionById(wallpaper.getCollection()).getName());

        ImageView back = findViewById(R.id.wallpaperBack);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loadWallpaper();

        mVisible = true;
        contentContainer = findViewById(R.id.contentContainer);
    }

    FullProgressBar downloadProgress;
    FrameLayout downloadContainer;
    String action;

    void loadWallpaper() {
        LinearLayout loadingView = findViewById(R.id.loadingView);
        LinearLayout errorView = findViewById(R.id.wallErrorView);

        Glide.with(context)
                .asBitmap()
                .load(new DriveWallpaperContainer(wallpaper, (DriveWallpaperContainer.DriveWallpaperListener) this))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Animation animation = AnimationUtils.loadAnimation(context,R.anim.fade_in);
                        animation.reset();
                        preview.clearAnimation();

                        loadingView.setVisibility(View.GONE);

                        preview.setImage(ImageSource.bitmap(resource));

                        preview.startAnimation(animation);
                        preview.setMaxScale(2.0f);
                        preview.setMinScale(0.5f);

                        preview.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);

                        preview.resetScaleAndCenter();

                        TextView fileName = findViewById(R.id.fileName);
                        TextView fileSize = findViewById(R.id.fileSize);
                        TextView orientation = findViewById(R.id.orientation);

                        fileName.setText(wallpaper.getName());
                        fileSize.setText(humanReadableByteCountBin(wallpaper.getSize()));

                        if (resource.getWidth() > resource.getHeight()) {
                            orientation.setText("Landscape — " + resource.getWidth() + "x" + resource.getHeight());
                        } else if (resource.getWidth() < resource.getHeight()) {
                            orientation.setText("Portrait — " + resource.getWidth() + "x" + resource.getHeight());
                        } else {
                            orientation.setText("Neither (Square) — " + resource.getWidth() + "x" + resource.getHeight());
                        }

                        LinearLayout actionView = findViewById(R.id.actionView);

                        actionView.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                return true;
                            }
                        });

                        preview.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                return gestureDetector.onTouchEvent(motionEvent);
                            }
                        });

                        LinearLayout download = findViewById(R.id.download);

                        download.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (context.checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions((Activity) context, new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_DOWNLOAD_PERMISSION);
                                } else {
                                    onRequestPermissionsResult(REQUEST_CODE_DOWNLOAD_PERMISSION, new String[]{WRITE_EXTERNAL_STORAGE},new int[]{PackageManager.PERMISSION_GRANTED});
                                }
                            }
                        });

                        LinearLayout apply = findViewById(R.id.apply);

                        apply.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                action = "setWallpaper";
                                Intent intent = new Intent(context, WallpaperDownloaderService.class);
                                intent.putExtra("wallpaper", wallpaper);
                                intent.putExtra("action", "setWallpaper");
                                context.startService(intent);

                                ServiceConnection connection = new ServiceConnection() {
                                    @Override
                                    public void onServiceConnected(ComponentName className,
                                                                   IBinder service) {
                                        WallpaperDownloaderService.ServiceBinder binder = (WallpaperDownloaderService.ServiceBinder) service;
                                        mService = binder.getService();
                                        mService.setListener((WallpaperDownloaderService.DownloadServiceListener) context);
                                        Log.d(TAG, "onServiceConnected: BOUND! NICE");
                                        mBound = true;
                                    }

                                    @Override
                                    public void onServiceDisconnected(ComponentName arg0) {
                                        mBound = false;
                                    }
                                };

                                bindService(intent, connection, Context.BIND_AUTO_CREATE);

                                downloadContainer = findViewById(R.id.downloadContainer);
                                downloadProgress = findViewById(R.id.downloadProgress);

                                downloadContainer.setVisibility(View.VISIBLE);
                            }
                        });

                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        errorView.setVisibility(View.VISIBLE);
                        Button retry = findViewById(R.id.retryWallpaperLoad);
                        retry.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                errorView.setVisibility(View.GONE);
                                loadingView.setVisibility(View.VISIBLE);
                                loadWallpaper();
                            }
                        });
                    }
                });
    }

    WallpaperDownloaderService mService;
    boolean mBound = false;

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

//    @SuppressLint("InlinedApi")
    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        preview.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        contentContainer.setVisibility(View.GONE);
        mVisible = false;

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        getWindow().setAttributes(attributes);

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        preview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        preview.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }
        getWindow().setAttributes(attributes);

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onSetInputStream(ProgressInputStream progressInputStream) {
        Log.d("FUCK", "onSetInputStream: FUCK! IT WORKED FUCK");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                previewProgress.setMax(100);
                previewProgress.setIndeterminate(false);
                progressInputStream.setOnProgressListener(new ProgressInputStream.OnProgressListener() {
                    @Override
                    public void onProgress(int percentage, Object tag) {
                        previewProgress.setProgress(percentage);
                        Log.d("FUCK", "onProgress: " + percentage);
                    }
                });
            }
        });
    }

    @SuppressLint("DefaultLocale")
    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    private static final String TAG = "WallpaperViewerActivity";

    @Override
    public void onProgress(int progress) {
        Log.d(TAG, "onProgress: " + progress);
//        downloadProgress.setProgress(progress);
    }

    @Override
    public void onFinish() {
        Log.d(TAG, "onProgress: Finished");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                downloadContainer.setVisibility(View.GONE);
                Toast.makeText(context, "Download Finished", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onError() {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_DOWNLOAD_PERMISSION) {
            if (permissions[0].equals(WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                action = "download";
                Intent intent = new Intent(context, WallpaperDownloaderService.class);
                intent.putExtra("wallpaper", wallpaper);
                intent.putExtra("action", action);
                context.startService(intent);

                ServiceConnection connection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName className,
                                                   IBinder service) {
                        // We've bound to LocalService, cast the IBinder and get LocalService instance
                        WallpaperDownloaderService.ServiceBinder binder = (WallpaperDownloaderService.ServiceBinder) service;
                        mService = binder.getService();
                        mService.setListener((WallpaperDownloaderService.DownloadServiceListener) context);
                        Log.d(TAG, "onServiceConnected: BOUND! NICE");
                        mBound = true;
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName arg0) {
                        mBound = false;
                    }
                };

                bindService(intent, connection, Context.BIND_AUTO_CREATE);

                downloadContainer = findViewById(R.id.downloadContainer);
                downloadProgress = findViewById(R.id.downloadProgress);

                downloadContainer.setVisibility(View.VISIBLE);
                downloadContainer.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        return true;
                    }
                });
            } else {
                Toast.makeText(context, "Permission is required to download. Please grant the permission.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}