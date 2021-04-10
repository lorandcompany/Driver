package lor.and.company.driver.glideModules;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

public class DriveDataLoaderFactory implements ModelLoaderFactory<DriveWallpaperContainer, ProgressInputStream> {
    Context context;

    public DriveDataLoaderFactory(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ModelLoader<DriveWallpaperContainer, ProgressInputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new DriveDataLoader(context);
    }

    @Override
    public void teardown() {

    }
}
