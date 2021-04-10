package lor.and.company.driver.glideModules;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;


public final class DriveDataLoader implements ModelLoader<DriveWallpaperContainer, ProgressInputStream> {
    Context context;

    DriveDataLoader(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public LoadData<ProgressInputStream> buildLoadData(@NonNull DriveWallpaperContainer wallpaper, int width, int height, @NonNull Options options) {
        return new LoadData<ProgressInputStream>(new ObjectKey(wallpaper.getWallpaper().getId()), new DriveDataFetcher(wallpaper, context));
    }

    @Override
    public boolean handles(@NonNull DriveWallpaperContainer wallpaper) {
        return true;
    }
}

