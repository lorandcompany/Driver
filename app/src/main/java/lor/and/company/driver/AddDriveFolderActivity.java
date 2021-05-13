package lor.and.company.driver;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.List;


import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.helpers.DriveHelper;
import lor.and.company.driver.adapters.PreviewRecyclerAdapter;

import static lor.and.company.driver.SettingsActivity.RESTORE_BACKUP;

public class AddDriveFolderActivity extends AppCompatActivity {

    ConstraintLayout previewLayout;
    TextInputEditText driveLink;
    DBHelper.CollectionsDB db;
    Button importer;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        db = new DBHelper.CollectionsDB(context);

        setContentView(R.layout.activity_add_drive_folder);

        previewLayout = findViewById(R.id.previewLayout);

        driveLink = findViewById(R.id.driveLink);
        importer = findViewById(R.id.btn_import);

        Button frombackup = findViewById(R.id.frombackup);
        frombackup.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                 intent.addCategory(Intent.CATEGORY_OPENABLE);
                 intent.setType("application/*");
                 startActivityForResult(intent, RESTORE_BACKUP);
             }
        });

        importer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Button clicked");
                new AddDriveFolderTask().execute(context);
            }
        });
    }

    private static final String TAG = "AddDriveFolderActivity";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        // https://drive.google.com/folderview?id=1Zd5Ow8zgVvnKoNCPwJ0lzJIQp0X4-DvR
        sURIMatcher.addURI("drive.google.com", "folderview", 1);
        // https://drive.google.com/drive/u/1/folders/1Zd5Ow8zgVvnKoNCPwJ0lzJIQp0X4-DvR
        sURIMatcher.addURI("drive.google.com", "drive/u/#/folders/*", 2);
        // https://drive.google.com/drive/folders/1Zd5Ow8zgVvnKoNCPwJ0lzJIQp0X4-DvR?usp=sharing
        sURIMatcher.addURI("drive.google.com", "drive/folders/*", 2);
        // https://drive.google.com/drive/u/1/folders/1Zd5Ow8zgVvnKoNCPwJ0lzJIQp0X4-DvR
        sURIMatcher.addURI("drive.google.com", "drive/u/#/mobile/folders/*", 2);
    }

    public class AddDriveFolderTask extends AsyncTask<Context, Void, List<File>> {
        Context context;
        String authorStr, folderNameStr;
        Drive service;
        String folderId = null;
        String reason;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            importer.setEnabled(false);
            importer.setText("Loading Drive Folder");
            importer.setBackgroundColor(getResources().getColor(R.color.white));
        }

        @Override
        protected List<File> doInBackground(Context... contexts) {
            try {
                Log.d(TAG, "doInBackground: Now running background task");
                context = contexts[0];

                Log.d(TAG, "doInBackground: " + driveLink.getText().toString());

                service = DriveHelper.getDrive(context);

                folderId = DriveHelper.getId(driveLink.getText().toString());

                if (folderId.equals("")) {
                    reason = "Enter a Drive folder link.";
                    return null;
                }

                try (Cursor c = db.db.rawQuery("SELECT id FROM CollectionsDB WHERE id = ?", new String[]{folderId});) {
                    if (c.moveToFirst()) {
                        reason = "This folder is already in your library.";
                        return null;
                    }
                }

                Log.d(TAG, "doInBackground: " + folderId);

                FileList result = service.files().list().setQ("\"" + folderId + "\" in parents and mimeType contains \"image/\"")
                        .setPageSize(50)
                        .setFields("files(id, name, thumbnailLink, imageMediaMetadata)")
                        .execute();

                List<File> files = result.getFiles();

                if (files.size() == 0) {
                    reason = "Driver has found no images in this folder.";
                    return null;
                }
                
                File folderDetails = service.files().get(folderId)
                        .setFields("name, owners")
                        .execute();

                Log.d(TAG, "File count: " + files.size());

                //                    if (willAllowLandscape || file.getImageMediaMetadata().getWidth() < file.getImageMediaMetadata().getHeight()) {
                //                    }

//                Log.d(TAG, "doInBackground: " + files.size());

                folderNameStr = folderDetails.getName();
                authorStr = folderDetails.getOwners().get(0).getDisplayName();
                return files;
            } catch (IOException e) {
                reason = e.getMessage();
                if (reason.contains("authError")) {
                    reason = "You removed Driver's permission to access your Google Drive.";
                } else if (reason.contains("usageLimits")) {
                    reason = "You're using this app TOO MUCH. Try again tomorrow.";
                } else if (reason.contains("rateLimitExceeded") && reason.contains("403")) {
                    reason = "This app has become too popular and is hitting its limit. Try again tomorrow";
                } else if (reason.contains("insufficientFilePermissions")) {
                    reason = "You don't have permission to view the folder. Ask the folder owner to add your email to the list of shared people.";
                } else if (reason.contains("notFound")) {
                    reason = "This Drive folder doesn't exist or you don't have permission to view it.";
                } else if (reason.contains("rateLimitExceeded") && reason.contains("429")) {
                    reason = "You're doing too much in such a small amount of time. Try again later.";
                } else if (reason.contains("backendError")) {
                    reason = "Google Drive had a meltdown. Try again.";
                } else if (reason.contains("Unable to resolve")) {
                    reason = "You're not connected to the internet. Please check your internet connection.";
                }
                e.printStackTrace();
                return null;
            } catch (Exception f) {
                f.printStackTrace();
                reason = f.getMessage();
                return null;
            }
        }

        RecyclerView preview;

        @Override
        protected void onPostExecute(List<File> files) {
            super.onPostExecute(files);
            preview = findViewById(R.id.previewRecyclerView);
            if (files != null) {
                previewLayout = findViewById(R.id.previewLayout);

                TextView foldername = findViewById(R.id.folderName);
                TextView author = findViewById(R.id.authorName);
                foldername.setText(folderNameStr);
                author.setText(authorStr);
                previewLayout.setVisibility(View.VISIBLE);
                StaggeredGridLayoutManager layoutManager = new ImmovableLayoutManager(5, StaggeredGridLayoutManager.VERTICAL);
                layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
                preview.setLayoutManager(layoutManager);
                PreviewRecyclerAdapter recyclerView = new PreviewRecyclerAdapter(context, files);
                preview.setAdapter(recyclerView);
                Button import2 = findViewById(R.id.importFolder);
                import2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new AsyncTask<Void, Boolean, Boolean>() {
                            @Override
                            protected Boolean doInBackground(Void... voids) {
                                try {
                                    db.addCollection(service, folderId);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return true;
                                }
                                return false;
                            }

                            @Override
                            protected void onPostExecute(Boolean error) {
                                super.onPostExecute(error);
                                if (error) {
                                    Toast.makeText(context, "Failed to add folder.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Folder added successfully!", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    Intent intent = new Intent(context, CollectionActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }
                            }
                        }.execute();
                    }
                });
                Button cancel = findViewById(R.id.back);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        previewLayout.setVisibility(View.GONE);
                        preview.setAdapter(null);
                    }
                });
            } else {
                Toast.makeText(context, "Error: " + reason, Toast.LENGTH_LONG).show();
            }
            importer.setEnabled(true);
            importer.setText("Import Folder");
            importer.setBackgroundColor(getResources().getColor(R.color.solidblue));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESTORE_BACKUP){
            Log.d(TAG, "onActivityResult: Restoring backup");
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(context, ImportActivity.class);
                intent.setData(data.getData());
                startActivity(intent);
                setResult(RESULT_OK);
            } else {
                Toast.makeText(context, "Cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class ImmovableLayoutManager extends StaggeredGridLayoutManager {
        public ImmovableLayoutManager(int spanCount, int orientation) {
            super(spanCount, orientation);
        }

        @Override
        public boolean canScrollVertically() {
            return false;
        }

        @Override
        public boolean canScrollHorizontally() {
            return false;
        }
    }
}