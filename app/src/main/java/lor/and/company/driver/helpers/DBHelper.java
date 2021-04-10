package lor.and.company.driver.helpers;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lor.and.company.driver.RefresherListener;
import lor.and.company.driver.models.Collection;
import lor.and.company.driver.models.Wallpaper;

public class DBHelper {
    public static SQLiteDatabase openDB(Context context) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(context.getFilesDir() + "/files.db", null);
        return db;
    }

    private static final String TAG = "DBHelper";

    public static class CollectionsDB {
        public final SQLiteDatabase db;
        Context context;

        public CollectionsDB(Context context) {
            this.context = context;
            db = openDB(context);
            db.execSQL("CREATE TABLE IF NOT EXISTS CollectionsDB (id VARCHAR, name VARCHAR, owner VARCHAR, folderLink VARCHAR, modifiedTime INTEGER, count VARCHAR, thumbnailLink VARCHAR, error INTEGER)");
        }

        public boolean ifExists(String id) {
            Cursor cursor = db.rawQuery("SELECT id FROM CollectionsDB WHERE id = ?", new String[]{id});
            return cursor.moveToFirst();
        }

        public void setError(String id) {
            db.execSQL("UPDATE CollectionsDB SET error = 1 WHERE id = ?",
                    new Object[]{id});
        }

        public ArrayList<Collection> getCollections() {
            Cursor cursor = db.rawQuery("SELECT id, name, owner, folderLink, modifiedTime, count, thumbnailLink, error FROM CollectionsDB ORDER BY modifiedTime DESC", null);
            ArrayList<Collection> collections = new ArrayList<>();
            while (cursor.moveToNext()) {
                Log.d(TAG, "getCollections: " +cursor.getString(6));
                collections.add(new Collection(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4), cursor.getInt(5), cursor.getString(6), cursor.getInt(7) == 1));
            }
            return collections;
        }

        public void addCollection(Drive drive, String id) throws Exception{
            FileList result;
            ArrayList<Wallpaper> wallpapers = new ArrayList<>();
            File folder = drive.files().get(id).setFields("modifiedTime, name, owners, webViewLink").execute();
            result = drive.files().list().setQ("\"" + id + "\" in parents and mimeType contains \"image/\"").setPageSize(400).setFields("files(id, name, webContentLink, createdTime, thumbnailLink, size)").execute();
            List<File> files = result.getFiles();
            File thumbnail = null;
            for (File file : files) {
                if (thumbnail == null) {
                    thumbnail = file;
                }
                //                wallpapers.add(new Wallpaper(file.getId(), file.getName(), "https://drive.google.com/thumbnail?authuser=0&sz=w500&id="+file.getId(), file.getWebContentLink(), id, file.getCreatedTime().getValue()));
                wallpapers.add(new Wallpaper(file.getId(), file.getName(), file.getThumbnailLink().replace("s220", "s500"), file.getWebContentLink(), id, file.getCreatedTime().getValue(), file.getSize()));
            }
            while (result.getNextPageToken() != null) {
                String token = result.getNextPageToken();
                result = drive.files().list().setQ("\"" + id + "\" in parents and mimeType contains \"image/\"").setPageSize(400).setPageToken(token).setFields("files(id, name, webContentLink, createdTime, thumbnailLink, size)").execute();
                for (File file : files) {
                    wallpapers.add(new Wallpaper(file.getId(), file.getName(), file.getThumbnailLink().replace("s220", "s500"), file.getWebContentLink(), id, file.getCreatedTime().getValue(), file.getSize()));
                }
            }
            Log.d(TAG, "doInBackground: Now updating collection " + id);
            WallpaperDB wallpaperDB = new WallpaperDB(context);
            Log.d(TAG, "addCollection: " + wallpapers.size());
            wallpaperDB.updateCollectionWallpapers(wallpapers);
            db.execSQL("DELETE FROM CollectionsDB WHERE id = ?", new Object[]{id});
            try (Cursor cursor = wallpaperDB.db.rawQuery("SELECT id FROM WallpaperDB WHERE collection = ? ORDER BY createdOn DESC LIMIT 1", new String[]{id})) {
                cursor.moveToFirst();
                db.execSQL("INSERT INTO CollectionsDB (id, name, owner, folderLink, count, modifiedTime, thumbnailLink, error) VALUES (?,?,?,?,?,?,?,?)",
                        new Object[]{id, folder.getName(), folder.getOwners().get(0).getDisplayName(), folder.getWebViewLink(), wallpapers.size(), folder.getModifiedTime().getValue(), thumbnail.getThumbnailLink().replace("s220", "s1080"), 0});
            }
        }

        public void deleteCollection(Collection collection) {
            db.execSQL("DELETE FROM CollectionsDB WHERE id = ?", new String[]{collection.getId()});
            new WallpaperDB(context).deleteCollectionsWallpapers(collection);
        }

        public int updateCollections(Drive drive, RefresherListener listener) {
            try (Cursor cursor = db.rawQuery("SELECT id FROM CollectionsDB;", null)){
                int errorcount = 0;
                while (cursor.moveToNext()) {
                    listener.onRefreshUpdate(cursor.getPosition() + 1, cursor.getCount());
                    try {
                        addCollection(drive, cursor.getString(0));
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorcount += 1;
                        setError(cursor.getString(0));
                    }
                }
                return errorcount;
            }
        }

        public void updateCollection(Collection collection, RefresherListener listener) throws IOException {
            Drive drive = DriveHelper.getDrive(context);
            FileList result;
            ArrayList<Wallpaper> wallpapers = new ArrayList<>();
            result = drive.files().list().setQ("\"" + collection.getId() + "\" in parents and mimeType contains \"image/\"").setPageSize(400).setFields("files(id, name, webContentLink, thumbnailLink, createdTime, size)").execute();
            List<File> files = result.getFiles();
            File thumbnail = null;
            for (File file : files) {
                if (thumbnail == null) {
                    thumbnail = file;
                }
                wallpapers.add(new Wallpaper(file.getId(), file.getName(), file.getThumbnailLink() , file.getWebContentLink(), collection.getId(), file.getCreatedTime().getValue(), file.getSize()));
                listener.onRefreshUpdate(wallpapers.size(), 0);
            }
            while (result.getNextPageToken() != null) {
                String token = result.getNextPageToken();
                result = drive.files().list().setQ("\"" + collection.getId() + "\" in parents and mimeType contains \"image/\"").setPageSize(400).setPageToken(token).setFields("files(id, name, thumbnailLink, webContentLink, createdTime, size)").execute();
                for (File file : files) {
                    wallpapers.add(new Wallpaper(file.getId(), file.getName(), file.getThumbnailLink(), file.getWebContentLink(), collection.getId(), file.getCreatedTime().getValue(), file.getSize()));
                    listener.onRefreshUpdate(wallpapers.size(), 0);
                }
            }
            Log.d(TAG, "doInBackground: Now updating collection " + collection.getId());
            WallpaperDB wallpaperDB = new WallpaperDB(context);
            wallpaperDB.updateCollectionWallpapers(wallpapers);
        }

        public Collection getCollectionById(String id) {
            try (Cursor cursor = db.rawQuery("SELECT id, name, owner, folderLink, modifiedTime, count, thumbnailLink, error FROM CollectionsDB WHERE id = ?", new String[]{id})) {
                if (cursor.moveToFirst()) {
                    return new Collection(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4), cursor.getInt(5), cursor.getString(6), cursor.getInt(7) == 1);
                } else {
                    return null;
                }
            }
        }
    }

    public static class WallpaperDB {
        public final SQLiteDatabase db;
        Context context;

        public WallpaperDB(Context context) {
            this.context = context;
            db = openDB(context);
            db.execSQL("CREATE TABLE IF NOT EXISTS WallpaperDB (collection VARCHAR, id VARCHAR, name VARCHAR, thumbnailLink VARCHAR, downloadLink VARCHAR, createdOn INTEGER, size INTEGER)");
        }

        public void deleteCollectionsWallpapers(Collection collection) {
            db.execSQL("DELETE FROM WallpaperDB WHERE collection = ?", new Object[]{collection.getId()});
        }

        public ArrayList<Wallpaper> getWallpapers(Collection collection) {
            Cursor cursor = db.rawQuery("SELECT id, name, thumbnailLink, downloadLink, collection, createdOn, size FROM WallpaperDB WHERE collection = ? ORDER BY createdOn DESC", new String[]{collection.getId()});
            ArrayList<Wallpaper> wallpapers = new ArrayList<>();
            while (cursor.moveToNext()) {
                wallpapers.add(new Wallpaper(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getLong(5), cursor.getLong(6)));
            }
            return wallpapers;
        }

        public void updateCollectionWallpapers(ArrayList<Wallpaper> wallpapers) {
            try {
                db.execSQL("DELETE FROM WallpaperDB WHERE collection = ?", new Object[]{wallpapers.get(0).getCollection()});
            } catch (Exception ignored) {}
            for (Wallpaper wallpaper: wallpapers) {
                db.execSQL("INSERT INTO WallpaperDB (id, name, thumbnailLink, downloadLink, collection, createdOn, size) VALUES (?,?,?,?,?,?,?)", wallpaper.createObject());
            }
        }
    }
}

