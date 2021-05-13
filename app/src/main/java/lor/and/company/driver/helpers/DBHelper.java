package lor.and.company.driver.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Keep;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import lor.and.company.driver.RefresherListener;
import lor.and.company.driver.helpers.DBHelper.CollectionsDB;
import lor.and.company.driver.helpers.DBHelper.WallpaperDB;
import lor.and.company.driver.models.Collection;
import lor.and.company.driver.models.Wallpaper;

public class DBHelper {
    public static SQLiteDatabase openDB(Context context) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(context.getFilesDir() + "/files.db", null);
        return db;
    }

    private static final String TAG = "DBHelper";

    public static class DeletedDB {
        public final SQLiteDatabase db;
        Context context;

        public DeletedDB(Context context) {
            this.context = context;
            db = openDB(context);
            db.execSQL("CREATE TABLE IF NOT EXISTS DeletedCollectionDB (id VARCHAR, name VARCHAR, owner VARCHAR, folderLink VARCHAR, modifiedTime INTEGER, count VARCHAR, thumbnailLink VARCHAR, error INTEGER, errorMessage VARCHAR)");
            db.execSQL("CREATE TABLE IF NOT EXISTS DeletedWallpaperDB (collection VARCHAR, id VARCHAR, name VARCHAR, thumbnailLink VARCHAR, downloadLink VARCHAR, createdOn INTEGER, size INTEGER, x INTEGER, y INTEGER)");
            db.execSQL("CREATE TABLE IF NOT EXISTS DeletedFavouriteDB (id VARCHAR, collection VARCHAR, addedOn VARCHAR)");
        }

        public void clear(){
            db.execSQL("DELETE FROM DeletedCollectionDB");
            db.execSQL("DELETE FROM DeletedWallpaperDB");
            db.execSQL("DELETE FROM DeletedFavouriteDB");
        }

        public void archiveCollection(Collection collection){
            Log.d(TAG, "archiveCollection: Archived Collection");
            db.execSQL("INSERT INTO DeletedCollectionDB (id, name, owner, folderLink, count, modifiedTime, thumbnailLink, error, errorMessage) VALUES (?,?,?,?,?,?,?,?,?)", collection.createObject());
        }

        public void archiveWallpapers(ArrayList<Wallpaper> wallpapers){
            for (Wallpaper wallpaper: wallpapers) {
                db.execSQL("INSERT INTO DeletedWallpaperDB (id, name, thumbnailLink, downloadLink, collection, createdOn, size, x, y) VALUES (?,?,?,?,?,?,?,?,?)", wallpaper.createObject());
                if (wallpaper.isFavourite()) {
                    db.execSQL("INSERT INTO DeletedFavouriteDB (id, collection, addedOn) VALUES (?,?,?)", new String[]{wallpaper.getId(), wallpaper.getCollection(), Long.toString(wallpaper.getAddedOn())});
                }
            }
        }

        public void undo() {
            SQLiteDatabase wallpaperDB = new WallpaperDB(context).db;
            SQLiteDatabase collectionsDB = new CollectionsDB(context).db;
            wallpaperDB.execSQL("INSERT INTO CollectionsDB SELECT * FROM DeletedCollectionDB");
            collectionsDB.execSQL("INSERT INTO WallpaperDB SELECT * FROM DeletedWallpaperDB");
            clear();
        }
    }

    public static class CollectionsDB {
        public final SQLiteDatabase db;
        Context context;

        public CollectionsDB(Context context) {
            this.context = context;
            db = openDB(context);
            db.execSQL("CREATE TABLE IF NOT EXISTS CollectionsDB (id VARCHAR, name VARCHAR, owner VARCHAR, folderLink VARCHAR, modifiedTime INTEGER, count VARCHAR, thumbnailLink VARCHAR, error INTEGER, errorMessage VARCHAR)");
        }

        public boolean ifExists(String id) {
            try (Cursor cursor = db.rawQuery("SELECT id FROM CollectionsDB WHERE id = ?", new String[]{id});){
                return cursor.moveToFirst();
            }
        }

        public void setError(String id, String errorMessage) {
            db.execSQL("UPDATE CollectionsDB SET error = 1, errorMessage = ? WHERE id = ?",
                    new Object[]{errorMessage, id});
        }

        public ArrayList<Collection> getCollections() {
            try (Cursor cursor = db.rawQuery("SELECT id, name, owner, folderLink, modifiedTime, count, thumbnailLink, error, errorMessage FROM CollectionsDB ORDER BY modifiedTime DESC", null);){
                ArrayList<Collection> collections = new ArrayList<>();
                while (cursor.moveToNext()) {
                    Log.d(TAG, "getCollections: " +cursor.getString(6));
                    collections.add(new Collection(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4), cursor.getInt(5), cursor.getString(6), cursor.getInt(7) == 1, cursor.getString(8)));
                }
                return collections;
            }
        }

        public void addCollection(Drive drive, String id) throws Exception{
            FileList result;
            ArrayList<Wallpaper> wallpapers = new ArrayList<>();
            File folder = drive.files().get(id).setFields("modifiedTime, name, owners, webViewLink").execute();
            result = drive.files().list().setQ("\"" + id + "\" in parents and mimeType contains \"image/\"").setPageSize(400).setFields("files(id, name, webContentLink, createdTime, thumbnailLink, size, imageMediaMetadata)").execute();
            List<File> files = result.getFiles();
            File thumbnail = null;
            for (File file : files) {
                if (thumbnail == null) {
                    thumbnail = file;
                }
                //                wallpapers.add(new Wallpaper(file.getId(), file.getName(), "https://drive.google.com/thumbnail?authuser=0&sz=w500&id="+file.getId(), file.getWebContentLink(), id, file.getCreatedTime().getValue()));
                wallpapers.add(new Wallpaper(file.getId(), file.getName(), file.getThumbnailLink().replace("s220", "s500"), file.getWebContentLink(), id, file.getCreatedTime().getValue(), file.getSize(), file.getImageMediaMetadata().getWidth(), file.getImageMediaMetadata().getHeight()));
            }
            while (result.getNextPageToken() != null) {
                String token = result.getNextPageToken();
                result = drive.files().list().setQ("\"" + id + "\" in parents and mimeType contains \"image/\" and trashed = false").setPageSize(400).setPageToken(token).setFields("files(id, name, webContentLink, createdTime, thumbnailLink, size, imageMediaMetadata)").execute();
                for (File file : files) {
                    wallpapers.add(new Wallpaper(file.getId(), file.getName(), file.getThumbnailLink().replace("s220", "s500"), file.getWebContentLink(), id, file.getCreatedTime().getValue(), file.getSize(), file.getImageMediaMetadata().getWidth(), file.getImageMediaMetadata().getHeight()));
                }
            }
            Log.d(TAG, "doInBackground: Now updating collection " + id);
            WallpaperDB wallpaperDB = new WallpaperDB(context);
            Log.d(TAG, "addCollection: " + wallpapers.size());
            wallpaperDB.updateCollectionWallpapers(wallpapers);
            db.execSQL("DELETE FROM CollectionsDB WHERE id = ?", new Object[]{id});
            try (Cursor cursor = wallpaperDB.db.rawQuery("SELECT id FROM WallpaperDB WHERE collection = ? ORDER BY createdOn DESC LIMIT 1", new String[]{id})) {
                cursor.moveToFirst();
                db.execSQL("INSERT INTO CollectionsDB (id, name, owner, folderLink, count, modifiedTime, thumbnailLink, error, errorMessage) VALUES (?,?,?,?,?,?,?,?,?)",
                        new Object[]{id, folder.getName(), folder.getOwners().get(0).getDisplayName(), folder.getWebViewLink(), wallpapers.size(), folder.getModifiedTime().getValue(), thumbnail.getThumbnailLink().replace("s220", "s1080"), 0, ""});
            }
        }

        public void deleteCollection(Collection collection) {
            DeletedDB deletedDB = new DeletedDB(context);
            deletedDB.clear();
            deletedDB.archiveCollection(collection);
            db.execSQL("DELETE FROM CollectionsDB WHERE id = ?", new String[]{collection.getId()});
            new WallpaperDB(context).deleteCollectionsWallpapers(collection, deletedDB);
        }


        public void updateCollections(Drive drive, RefresherListener listener, FinishCallback finishCallback) {
            try (Cursor cursor = db.rawQuery("SELECT id FROM CollectionsDB;", null)){
                final int[] errorcount = {0};
                final int[] finished = {0};
                listener.onRefreshUpdate(0, cursor.getCount());
                while (cursor.moveToNext()) {
                    new AsyncTask<String, Void, Exception>() {
                        String id;
                        String errorMessage = "";

                        @Override
                        protected Exception doInBackground(String... strings) {
                            id = strings[0];
                            try {
                                addCollection(drive, id);
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (e.getClass() == GoogleJsonResponseException.class) {
                                    errorMessage = DriveHelper.getError(e.getMessage());
                                } else {
                                    errorMessage = e.toString();
                                }
                                return e;
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Exception error) {
                            super.onPostExecute(error);
                            if (error != null) {
                                errorcount[0] += 1;
                                setError(id, errorMessage);
                            }
                            finished[0]++;
                            listener.onRefreshUpdate(finished[0], cursor.getCount());
                            if (finished[0] == cursor.getCount()) {
                                finishCallback.finished(errorcount[0]);
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, cursor.getString(0));
                }
            }
        }

        public void updateCollection(Collection collection, RefresherListener listener) throws IOException {
            Drive drive = DriveHelper.getDrive(context);
            FileList result;
            ArrayList<Wallpaper> wallpapers = new ArrayList<>();
            result = drive.files().list().setQ("\"" + collection.getId() + "\" in parents and mimeType contains \"image/\"").setPageSize(400).setFields("files(id, name, webContentLink, thumbnailLink, createdTime, size, imageMediaMetadata)").execute();
            List<File> files = result.getFiles();
            File thumbnail = null;
            for (File file : files) {
                if (thumbnail == null) {
                    thumbnail = file;
                }
                wallpapers.add(new Wallpaper(file.getId(), file.getName(), file.getThumbnailLink() , file.getWebContentLink(), collection.getId(), file.getCreatedTime().getValue(), file.getSize(), file.getImageMediaMetadata().getWidth(), file.getImageMediaMetadata().getHeight()));
                listener.onRefreshUpdate(wallpapers.size(), 0);
            }
            while (result.getNextPageToken() != null) {
                String token = result.getNextPageToken();
                result = drive.files().list().setQ("\"" + collection.getId() + "\" in parents and mimeType contains \"image/\"").setPageSize(400).setPageToken(token).setFields("files(id, name, thumbnailLink, webContentLink, createdTime, size, imageMediaMetadata)").execute();
                for (File file : files) {
                    wallpapers.add(new Wallpaper(file.getId(), file.getName(), file.getThumbnailLink(), file.getWebContentLink(), collection.getId(), file.getCreatedTime().getValue(), file.getSize(), file.getImageMediaMetadata().getWidth(), file.getImageMediaMetadata().getHeight()));
                    listener.onRefreshUpdate(wallpapers.size(), 0);
                }
            }
            Log.d(TAG, "doInBackground: Now updating collection " + collection.getId());
            WallpaperDB wallpaperDB = new WallpaperDB(context);
            wallpaperDB.updateCollectionWallpapers(wallpapers);
        }

        public Collection getCollectionById(String id) {
            try (Cursor cursor = db.rawQuery("SELECT id, name, owner, folderLink, modifiedTime, count, thumbnailLink, error, errorMessage FROM CollectionsDB WHERE id = ?", new String[]{id})) {
                if (cursor.moveToFirst()) {
                    return new Collection(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4), cursor.getInt(5), cursor.getString(6), cursor.getInt(7) == 1, cursor.getString(8));
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
            db.execSQL("CREATE TABLE IF NOT EXISTS WallpaperDB (collection VARCHAR, id VARCHAR, name VARCHAR, thumbnailLink VARCHAR, downloadLink VARCHAR, createdOn INTEGER, size INTEGER, x INTEGER, y INTEGER)");
        }

        public void deleteCollectionsWallpapers(Collection collection, DeletedDB deletedDB) {
            deletedDB.archiveWallpapers(this.getWallpapers(collection));
            db.execSQL("DELETE FROM WallpaperDB WHERE collection = ?", new Object[]{collection.getId()});
        }

        public ArrayList<Wallpaper> getWallpapers(Collection collection) {
            ArrayList<Wallpaper> wallpapers = new ArrayList<>();
            try (Cursor cursor = db.rawQuery("SELECT id, name, thumbnailLink, downloadLink, collection, createdOn, size, x, y FROM WallpaperDB WHERE collection = ? ORDER BY createdOn DESC", new String[]{collection.getId()});){
                while (cursor.moveToNext()) {
                    wallpapers.add(new Wallpaper(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getLong(5), cursor.getLong(6), cursor.getInt(7), cursor.getInt(8)));
                }
                FavouriteDB favDB = new FavouriteDB(context);
                HashMap<String, String> favs = favDB.getFavouritesFromCollection(collection);
                for (Wallpaper wallpaper: wallpapers){
                    if (favs.containsKey(wallpaper.getId())){
                        wallpaper.setFavourite(true);
                    }
                }
            }

            return wallpapers;
        }

        public void updateCollectionWallpapers(ArrayList<Wallpaper> wallpapers) {
            try {
                db.execSQL("DELETE FROM WallpaperDB WHERE collection = ?", new Object[]{wallpapers.get(0).getCollection()});
            } catch (Exception ignored) {}
            for (Wallpaper wallpaper: wallpapers) {
                db.execSQL("INSERT INTO WallpaperDB (id, name, thumbnailLink, downloadLink, collection, createdOn, size, x, y) VALUES (?,?,?,?,?,?,?,?,?)", wallpaper.createObject());
            }
        }
    }

    public static class FavouriteDB {
        public final SQLiteDatabase db;
        Context context;

        public FavouriteDB(Context context) {
            this.context = context;
            db = openDB(context);
            db.execSQL("CREATE TABLE IF NOT EXISTS FavouriteDB (id VARCHAR, collection VARCHAR, addedOn VARCHAR)");
        }

        public HashMap<String, String> getFavouritesFromCollection(Collection collection) {
            try (Cursor cursor = db.rawQuery("SELECT id, addedOn FROM FavouriteDB WHERE collection = ?", new String[]{collection.getId()})) {
                HashMap<String, String> favourites = new HashMap<>();
                while (cursor.moveToNext()) {
                    favourites.put(cursor.getString(0), cursor.getString(1));
                }
                return favourites;
            } catch (Exception e) {
                Log.d(TAG, "getFavouritesFromCollection: FAILED! OMG");
                return null;
            }
        }

        public void deleteFavourites(Collection collection){
            db.execSQL("DELETE FROM FavouriteDB WHERE collection = ?", new String[]{collection.getId()});
        }

        public void removeFromFavourites(Wallpaper wallpaper) {
            db.execSQL("DELETE FROM FavouriteDB WHERE id = ?", new String[]{wallpaper.getId()});
        }

        public void addFromFavourites(Wallpaper wallpaper) {
            long timestamp = new Date().getTime();
            db.execSQL("INSERT INTO FavouriteDB (id, collection, addedOn) VALUES (?,?,?)", new String[]{wallpaper.getId(), wallpaper.getCollection(), Long.toString(timestamp)});
        }
    }

    public interface FinishCallback {
        void finished(int errorcount);
    }
}

