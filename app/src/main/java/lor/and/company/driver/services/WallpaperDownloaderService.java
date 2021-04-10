package lor.and.company.driver.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.api.client.util.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import lor.and.company.driver.R;
import lor.and.company.driver.WallpaperViewerActivity;
import lor.and.company.driver.glideModules.ProgressInputStream;
import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.helpers.DriveHelper;
import lor.and.company.driver.models.Wallpaper;

import static android.widget.Toast.LENGTH_SHORT;

public class WallpaperDownloaderService extends Service{

    DownloadServiceListener listener;
    NotificationManager manager;
    NotificationCompat.Builder progressBuilder;



    public void setListener(DownloadServiceListener listener) {
        this.listener = listener;
        manager.notify(1, progressBuilder.build());
    }

    public class ServiceBinder extends Binder {
        public WallpaperDownloaderService getService() {
            return WallpaperDownloaderService.this;
        }
    }

    public interface DownloadServiceListener {
        void onProgress(int progress);
        void onFinish();
        void onError();
    }

    private final IBinder binder = new ServiceBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private static final String TAG = "WallpaperDownloader";
    Context context;
    Intent intent;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        Log.d(TAG, "onCreate: Service created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            this.intent = intent;
            startForegroundService();
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent resultIntent = new Intent(this, WallpaperViewerActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(resultIntent);


            NotificationChannel progressChannel = new NotificationChannel("progress", "Downloader Progress", NotificationManager.IMPORTANCE_DEFAULT);
            progressChannel.setDescription("Allows the downloader to view the progress of the download.");
            progressChannel.enableVibration(false);
            progressChannel.setVibrationPattern(new long[]{ 0 });
            progressChannel.setLightColor(Color.BLUE);
            progressChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationChannel finishedChannel = new NotificationChannel("finished", "Downloader Finished", NotificationManager.IMPORTANCE_DEFAULT);
            finishedChannel.enableVibration(true);
            finishedChannel.setLightColor(Color.BLUE);
            finishedChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            finishedChannel.setDescription("Allows the downloader to notify you if the download is finished by vibrating your phone.");

            manager.createNotificationChannel(progressChannel);
            manager.createNotificationChannel(finishedChannel);
        } else {

        }
        progressBuilder = new NotificationCompat.Builder(this, "progress");

        progressBuilder.setOngoing(true)
                .setColor(Color.BLUE)
                .setSmallIcon(R.drawable.download)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setPriority(Notification.PRIORITY_MIN);

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        progressBuilder.setProgress(100, 0, true);

                        Wallpaper wallpaper = ((Wallpaper) intent.getParcelableExtra("wallpaper"));

                        String action = intent.getStringExtra("action");

                        if (action.equals("download")) {
                            progressBuilder.setContentTitle("Download Wallpaper").setContentText("Downloading " + wallpaper.getName());
                        } else {
                            progressBuilder.setContentTitle("Apply Wallpaper").setContentText("Applying " + wallpaper.getName());
                        }

                        manager.notify(1, progressBuilder.build());

                        try {
                            InputStream inputStream = DriveHelper.getDrive(context).files().get(wallpaper.getId()).executeMediaAsInputStream();
                            Log.d(TAG, "run: Tagging inputstream");
                            ProgressInputStream progressInputStream = new ProgressInputStream(inputStream, wallpaper.getSize());
                            progressInputStream.setOnProgressListener(new ProgressInputStream.OnProgressListener() {
                                @Override
                                public void onProgress(int percentage, Object tag) {
                                    listener.onProgress(percentage);
                                    progressBuilder.setProgress(100, percentage, false);
                                    manager.notify(1, progressBuilder.build());
                                }
                            });

                            if (action.equals("setWallpaper")) {
                                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                                wallpaperManager.setStream(progressInputStream);
                            } else {
                                Uri insert;

                                ContentResolver contentResolver = getContentResolver();

                                try (Cursor query = contentResolver.query(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        new String[]{MediaStore.Images.Media._ID},
                                        MediaStore.Images.Media.TITLE + " LIKE '" + new DBHelper.CollectionsDB(context).getCollectionById(wallpaper.getCollection()).getName() + "' AND " + MediaStore.Images.Media.DISPLAY_NAME + " LIKE '" + wallpaper.getName() + "'",
                                        null,
                                        null)) {
                                    Log.d(TAG, "run query count: " + query.getCount());
                                    if (query.moveToFirst()) {
                                        Uri contentURI = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, query.getLong(0));
                                        if (contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + " LIKE ?", new String[]{
                                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "Driver" + File.separator + new DBHelper.CollectionsDB(context).getCollectionById(wallpaper.getCollection()).getName()+File.separator+wallpaper.getName()
                                        }) == 1) {
                                            Log.d(TAG, "run: Deleted");
                                        } else {
                                            Log.d(TAG, "run: Failed to delete");
                                        }
                                    } else {
                                        Log.d(TAG, "run: Duplicate not found");
                                    }
                                }

                                ContentValues values = new ContentValues();
                                values.put(MediaStore.MediaColumns.DISPLAY_NAME, wallpaper.getName());
                                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/");

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + wallpaper.getCollection());
                                } else {
                                    values.put(MediaStore.MediaColumns.DATA, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "Driver" + File.separator + new DBHelper.CollectionsDB(context).getCollectionById(wallpaper.getCollection()).getName()+File.separator+wallpaper.getName());
                                    Log.d(TAG, "run: " + values.getAsString(MediaStore.Images.Media.DATA));
                                }

                                insert = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                                OutputStream outputStream = contentResolver.openOutputStream(insert);

                                Log.d(TAG, "run: Finished copying file");
                            }

                            manager.cancel(1);

                            NotificationCompat.Builder finishedBuilder = new NotificationCompat.Builder(context, "finished");

                            finishedBuilder.setOngoing(false)
                                    .setColor(Color.BLUE)
                                    .setSmallIcon(R.drawable.check)
                                    .setCategory(Notification.CATEGORY_PROGRESS)
                                    .setPriority(Notification.PRIORITY_MAX);

                            if (action.equals("download")) {
                                finishedBuilder.setContentTitle("Download Wallpaper").setContentText("Wallpaper downloaded!");
                            } else {
                                finishedBuilder.setContentTitle("Apply Wallpaper").setContentText("Wallpaper applied!");
                            }

                            manager.notify(2, finishedBuilder.build());

                            listener.onFinish();

                            stopForegroundService();

                        } catch (IOException e) {
                            e.printStackTrace();

                            manager.cancel(1);

                            NotificationCompat.Builder finishedBuilder = new NotificationCompat.Builder(context, "finished");

                            finishedBuilder.setOngoing(false)
                                    .setColor(Color.BLUE)
                                    .setSmallIcon(R.drawable.error)
                                    .setCategory(Notification.CATEGORY_PROGRESS)
                                    .setPriority(Notification.PRIORITY_MAX);

                            if (action.equals("download")) {
                                finishedBuilder.setContentTitle("Download Wallpaper").setContentText("Download failed.");
                            } else {
                                finishedBuilder.setContentTitle("Apply Wallpaper").setContentText("Failed to apply wallpaper.");
                            }

                            manager.notify(2, finishedBuilder.build());

                            listener.onFinish();
                        }
                    }
                }
                // Starts the thread by calling the run() method in its Runnable
        ).start();
    }

    private void stopForegroundService() {
        Log.d(TAG, "Stopping foreground service.");
        // Stop foreground service and remove the notification.
        stopForeground(false);
        // Stop the foreground service.
        stopSelf();
    }
}
