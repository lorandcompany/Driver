package lor.and.company.driver;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
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

import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.prefs.Preferences;

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


    private static final int UI_ANIMATION_DELAY = 0;

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

    SharedPreferences preferences;

//    Animation animation;


    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        WindowInsets insets = this.getWindow().getDecorView().getRootWindowInsets();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int statusBarHeight = insets.getSystemWindowInsetTop();
            int navbarHeight = insets.getSystemWindowInsetBottom();
        } else {
            Rect rectangle = new Rect();
            Window window = getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
            int statusBarHeight = rectangle.top;
            int navbarHeight;
            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navbarHeight = resources.getDimensionPixelSize(resourceId);
            } else {
                navbarHeight = 0;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_viewer);

        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);


        this.getWindow().setStatusBarColor(Color.parseColor("#80000000"));
        this.getWindow().setNavigationBarColor(Color.parseColor("#80000000"));

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

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        loadWallpaper();

        mVisible = true;
        contentContainer = findViewById(R.id.contentContainer);

        show();
    }

    FullProgressBar downloadProgress;
    FrameLayout downloadContainer;
    String action;

    void loadWallpaper() {
        LinearLayout loadingView = findViewById(R.id.loadingView);
        LinearLayout errorView = findViewById(R.id.wallErrorView);

        TextView fileName = findViewById(R.id.fileName);
        TextView fileSize = findViewById(R.id.fileSize);
        TextView orientation = findViewById(R.id.orientation);

        fileName.setText(wallpaper.getName());
        fileSize.setText(humanReadableByteCountBin(wallpaper.getSize()));

        if (wallpaper.getWidth() > wallpaper.getHeight()) {
            orientation.setText("Landscape — " + wallpaper.getWidth() + "x" + wallpaper.getHeight());
        } else if (wallpaper.getWidth() < wallpaper.getHeight()) {
            orientation.setText("Portrait — " + wallpaper.getWidth() + "x" + wallpaper.getHeight());
        } else {
            orientation.setText("Neither (Square) — " + wallpaper.getWidth() + "x" + wallpaper.getHeight());
        }

        Glide.with(context)
                .asBitmap()
                .load(new DriveWallpaperContainer(wallpaper, (DriveWallpaperContainer.DriveWallpaperListener) this))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (preferences.getBoolean("animations", true)) {
                            Animation animation = AnimationUtils.loadAnimation(context,R.anim.fade_in);
                            animation.reset();
                            preview.clearAnimation();
                            preview.setImage(ImageSource.bitmap(resource));
                            preview.startAnimation(animation);
                        } else {
                            preview.setImage(ImageSource.bitmap(resource));
                        }

                        loadingView.setVisibility(View.GONE);

                        preview.setMaxScale(2.0f);
                        preview.setMinScale(0.5f);

                        preview.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);

                        preview.resetScaleAndCenter();



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
                                downloadProgress.setProgress(0);

                                if (preferences.getBoolean("animations", true)) {
                                    Animation animation = AnimationUtils.loadAnimation(context,R.anim.fade_in);
                                    animation.reset();
                                    downloadContainer.clearAnimation();
                                    downloadContainer.setAlpha(1);
                                    downloadContainer.startAnimation(animation);
                                } else {
                                    downloadContainer.setAlpha(1);
                                }
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
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (preferences.getBoolean("animations", true)) {
                    ObjectAnimator animation = ObjectAnimator.ofInt(downloadProgress, "progress", downloadProgress.progress, progress);
                    animation.setDuration(250);
                    animation.setAutoCancel(true);
                    animation.setInterpolator(new DecelerateInterpolator());
                    animation.start();
                } else {
                    downloadProgress.setProgress(progress);
                }
            }
        });
    }

    @Override
    public void onFinish(File file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (preferences.getBoolean("animations", true)) {
                    Animation animation = AnimationUtils.loadAnimation(context,R.anim.fade_in);
                    animation.reset();
                    downloadContainer.clearAnimation();
                    downloadContainer.setAlpha(0);
                    downloadContainer.startAnimation(animation);
                } else {
                    downloadContainer.setAlpha(0);
                }
                downloadContainer.setOnTouchListener(null);
            }
        });

        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        intent.addCategory("android.intent.category.DEFAULT");
        String str = "image/*";
//            intent.setDataAndType(Uri.fromFile(new File(context.getDataDir() + File.separator + "setAsWallpaperCache")), str);
        Uri apkURI = FileProvider.getUriForFile(
                context,
                context.getApplicationContext()
                        .getPackageName() + ".provider", file);
        intent.setDataAndType(apkURI, str);
        intent.putExtra("mimeType", str);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
//        start(Intent.createChooser(intent, "Set Image As:"));
        startActivityForResult(Intent.createChooser(intent, "Set Image As:"), 999);
    }



    @Override
    public void onFinish(String action) {
        Log.d(TAG, "onProgress: Finished");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (preferences.getBoolean("animations", true)) {
                    Animation animation = AnimationUtils.loadAnimation(context,R.anim.fade_in);
                    animation.reset();
                    downloadContainer.clearAnimation();
                    downloadContainer.setAlpha(0);
                    downloadContainer.startAnimation(animation);
                } else {
                    downloadContainer.setAlpha(0);
                }
                downloadContainer.setOnTouchListener(null);
                Toast.makeText(context, "Download Finished", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onError() {
        Log.d(TAG, "onProgress: Finished");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (preferences.getBoolean("animations", true)) {
                    Animation animation = AnimationUtils.loadAnimation(context,R.anim.fade_in);
                    animation.reset();
                    downloadContainer.clearAnimation();
                    downloadContainer.setAlpha(0);
                    downloadContainer.startAnimation(animation);
                } else {
                    downloadContainer.setAlpha(0);
                }
                Toast.makeText(context, "Download Failed. Something went wrong.", Toast.LENGTH_SHORT).show();
                downloadContainer.setOnTouchListener(null);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_DOWNLOAD_PERMISSION) {
            if (permissions[0].equals(WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadContainer = findViewById(R.id.downloadContainer);
                downloadProgress = findViewById(R.id.downloadProgress);
                downloadProgress.setProgress(0);

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

                if (preferences.getBoolean("animations", true)) {
                    Animation animation = AnimationUtils.loadAnimation(context,R.anim.fade_in);
                    animation.reset();
                    downloadContainer.clearAnimation();
                    downloadContainer.setAlpha(1);
                    downloadContainer.startAnimation(animation);
                } else {
                    downloadContainer.setAlpha(1);
                }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 999) {
            Toast.makeText(context, "Finished.", Toast.LENGTH_SHORT).show();
        }
    }
}