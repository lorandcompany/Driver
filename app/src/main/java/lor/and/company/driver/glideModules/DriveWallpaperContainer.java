package lor.and.company.driver.glideModules;

import lor.and.company.driver.models.Wallpaper;

public class DriveWallpaperContainer {
    Wallpaper wallpaper;
    ProgressInputStream progressInputStream = null;
    DriveWallpaperListener listener;

    public DriveWallpaperContainer(Wallpaper wallpaper, DriveWallpaperListener listener) {
        this.wallpaper = wallpaper;
        this.listener = listener;
    }

    public Wallpaper getWallpaper() {
        return wallpaper;
    }

    public void setProgressInputStream(ProgressInputStream progressInputStream) {
        this.progressInputStream = progressInputStream;
        listener.onSetInputStream(progressInputStream);
    }

    public interface DriveWallpaperListener {
        void onSetInputStream(ProgressInputStream progressInputStream);
    }
}
