package lor.and.company.driver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lor.and.company.driver.adapters.PreviewRecyclerAdapter;
import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.helpers.DriveHelper;

public class LinkActivity extends AppCompatActivity {

    private static final String TAG = "LinkActivity";

    Context context;
    Uri driveLink;
    LinearLayout loadingView;
    ConstraintLayout previewLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);

        context = this;

        previewLayout = findViewById(R.id.previewLayout);
        loadingView = findViewById(R.id.loadingView);

        driveLink = getIntent().getData();

        if (DriveHelper.sURIMatcher.match(driveLink) > 0) {
            new AddDriveLinkTask().execute();
        } else {
            Toast.makeText(this, "This is not a valid Driver link.", Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
        }
    }

    public class AddDriveLinkTask extends AsyncTask<Void, Void, List<File>> {
        String authorStr, folderNameStr;
        Drive service;
        String folderId = null;
        String reason;

        @Override
        protected List<File> doInBackground(Void... voids) {
            try {
                service = DriveHelper.getDrive(context);

                folderId = DriveHelper.getId(driveLink.toString());

                Log.d(TAG, "doInBackground: " + folderId);

                FileList result = service.files().list().setQ("\"" + folderId + "\" in parents and mimeType contains \"image/\"")
                        .setPageSize(50)
                        .setFields("files(id, name, thumbnailLink, imageMediaMetadata)")
                        .execute();

                File folderDetails = service.files().get(folderId)
                        .setFields("name, owners")
                        .execute();

                List<File> files = result.getFiles();

                folderNameStr = folderDetails.getName();
                authorStr = folderDetails.getOwners().get(0).getDisplayName();
                return files;
            } catch (IOException e) {
                reason = DriveHelper.getError(e.getMessage());
                return null;
            } catch (Exception f) {
                reason = f.getMessage();
            }
            return null;
        }

        RecyclerView preview;

        @Override
        protected void onPostExecute(List<File> files) {
            super.onPostExecute(files);
            preview = findViewById(R.id.previewRecyclerView);
            if (files != null) {
                loadingView.setVisibility(View.GONE);
                previewLayout.setVisibility(View.VISIBLE);
                TextView foldername = findViewById(R.id.folderName);
                TextView author = findViewById(R.id.authorName);
                foldername.setText(folderNameStr);
                author.setText(authorStr);
                StaggeredGridLayoutManager layoutManager = new AddDriveFolderActivity.ImmovableLayoutManager(5, StaggeredGridLayoutManager.VERTICAL);
                layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
                preview.setLayoutManager(layoutManager);
                PreviewRecyclerAdapter recyclerView = new PreviewRecyclerAdapter(context, files);
                preview.setAdapter(recyclerView);
                Button import2 = findViewById(R.id.importFolder);

                Button cancel = findViewById(R.id.back);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "onClick: Clicked back");
                        ((Activity) context).finish();
                    }
                });

                DBHelper.CollectionsDB db = new DBHelper.CollectionsDB(context);
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
                                    finish();
                                } else {
                                    Toast.makeText(context, "Folder added successfully!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(context, CollectionActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }
                            }

                            @Override
                            protected void onCancelled() {
                                super.onCancelled();
                            }
                        }.execute();
                    }
                });
            } else {
                Toast.makeText(context, "An error has been encountered while importing the folder: \n\n" + reason, Toast.LENGTH_LONG).show();
                finishAndRemoveTask();
            }
        }
    }
}