package lor.and.company.driver.glideModules;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;

import lor.and.company.driver.helpers.DriveHelper;

public class DriveDataFetcher implements DataFetcher<ProgressInputStream> {
    DriveWallpaperContainer wallpaper;
    Context context;

    private static final String TAG = "DriveDataFetcher";

    DriveDataFetcher(DriveWallpaperContainer wallpaper, Context context) {
        Log.d(TAG, "DriveDataFetcher: Created now Listener for " + wallpaper.getWallpaper().getId());
        this.wallpaper = wallpaper;
        this.context = context;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super ProgressInputStream> callback) {
        try {
            ProgressInputStream in = new ProgressInputStream(DriveHelper.getDrive(context).files().get(wallpaper.getWallpaper().getId()).executeMediaAsInputStream(), wallpaper.getWallpaper().getSize());
            wallpaper.setProgressInputStream(in);
            Log.d(TAG, "loadData: InputStream has been set as Listener");
            callback.onDataReady(in);
        } catch (IOException e) {
            callback.onLoadFailed(e);
        }
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<ProgressInputStream> getDataClass() {
        return ProgressInputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }
}
