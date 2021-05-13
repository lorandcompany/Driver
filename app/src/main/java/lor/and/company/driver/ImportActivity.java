package lor.and.company.driver;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lor.and.company.driver.adapters.ImportOptionsListener;
import lor.and.company.driver.adapters.ImportRecyclerAdapter;
import lor.and.company.driver.helpers.DBHelper.CollectionsDB;
import lor.and.company.driver.helpers.DriveHelper;

public class ImportActivity extends AppCompatActivity implements ImportOptionsListener {

    private static final String TAG = "ImportActivity";

    Context context;
    TextView loadText;
    ProgressBar progressBar;
    String[] ids;
    HashMap<Integer, ImportObject> dictionary = new HashMap<>();

    ConstraintLayout loadingView, container;

    ProgressBar importProgress;
    TextView importText;
    ConstraintLayout importlayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        context = this;

        loadingView = findViewById(R.id.loadingView);
        container = findViewById(R.id.container);

        importProgress = findViewById(R.id.importProgress);
        importText = findViewById(R.id.importText);
        importlayout = findViewById(R.id.importView);

        Uri data = getIntent().getData();

        Log.d("Logd", "onCreate: " + data.toString());

        loadText = findViewById(R.id.loadText);
        progressBar = findViewById(R.id.importProgress);

        ContentResolver contentResolver = this.getContentResolver();

        InputStream inputStream;
        try {
            inputStream = contentResolver.openInputStream(data);
            ids = IOUtils.toString(inputStream, "UTF-8").split("\r\n");
            inputStream.close();
            LoadFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Button importButton = findViewById(R.id.import2);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImport();
            }
        });

        Button back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Activity)context).finish();
            }
        });
    }

    int errorCount = 0;

    public void startImport() {
        container.setVisibility(View.GONE);
        importlayout.setVisibility(View.VISIBLE);

        CollectionsDB db = new CollectionsDB(context);
        Drive drive = DriveHelper.getDrive(context);
        ArrayList<ImportObject> importObjects = new ArrayList<>();
        for (Map.Entry<Integer, ImportObject> folder: dictionary.entrySet()) {
            if (!folder.getValue().ifError() && folder.getValue().ifImport()) {
                importObjects.add(folder.getValue());
            }
        }
        for (ImportObject importObject: importObjects) {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    try {
                        db.addCollection(drive, importObject.getFolderId());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean succeeded) {
                    super.onPostExecute(succeeded);
                    if (!succeeded) {
                        errorCount++;
                    }
                    importProgress(importObjects.size());
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    int progress = -1;
    int importCounter = 0;

    public void importProgress(int max) {
        importCounter++;
        importProgress.setIndeterminate(false);
        importProgress.setMax(max);
        importProgress.setProgress(importCounter);
        importText.setText("Imported " + importCounter + " out of " + max + " folders.");
        if (max == importCounter) {
            if (errorCount == 1) {
                Toast.makeText(context, "Partially imported folders. \nA folder failed to load.", Toast.LENGTH_SHORT).show();
            } else if (errorCount > 1) {
                Toast.makeText(context, "Partially imported folders. \n" + errorCount + " folders failed to load.", Toast.LENGTH_SHORT).show();
            }
            setResult(RESULT_OK);
            Intent intent = new Intent(context, CollectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    public void updateProgress() {
        int max = ids.length;
        progressBar.setProgress(++progress);
        progressBar.setMax(max);
        progressBar.setIndeterminate(progress == 0);
        loadText.setText("Loading folder " + (progress)  + " out of " + max);
        Log.d(TAG, "updateProgress: Finished loading "  + progress + "out of " + max);
        if (progress == max) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (dictionary.size() != 0) {
                        Log.d(TAG, "Finished all tasks");
                        container.setVisibility(View.VISIBLE);
                        loadingView.setVisibility(View.GONE);
                        RecyclerView list = findViewById(R.id.recyclerView);
                        list.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        ImportRecyclerAdapter adapter = new ImportRecyclerAdapter(context, dictionary, (ImportOptionsListener) context);
                        list.setAdapter(adapter);
                        list.smoothScrollToPosition(0);
                    } else {
                        Toast.makeText(context, "You already have all these folders in your library.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            });
        }
    }

    public void LoadFile() {
        Drive service = DriveHelper.getDrive(context);
        CollectionsDB collectionsDB = new CollectionsDB(context);
        updateProgress();
        try {
            for (int i = 0; i < ids.length; i++) {
                Log.d(TAG, "LoadFile: " + ids[i]);
                String folderId = ids[i].split(":::::")[0];
                String author = ids[i].split(":::::")[2];
                String name = ids[i].split(":::::")[1];
                Log.d(TAG, "LoadFile: Loading " + folderId);
                if (collectionsDB.ifExists(folderId)) {
                    updateProgress();
                    Log.d(TAG, "LoadFile: " + folderId + " was already imported.");
                    continue;
                }
                int finalI = i;
                Log.d(TAG, "LoadFile: finalI " + finalI);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            FileList result = service.files().list().setQ("\"" + folderId + "\" in parents and mimeType contains \"image/\"")
                                    .setPageSize(50)
                                    .setFields("files(id, name, thumbnailLink, imageMediaMetadata)")
                                    .execute();
                            List<File> files = result.getFiles();
                            File folderDetails = service.files().get(folderId)
                                    .setFields("name, owners")
                                    .execute();
                            dictionary.put(finalI, new ImportObject(folderId, files, folderDetails));
                        } catch (Exception e) {
                            dictionary.put(finalI, new ImportObject(folderId, name, author));
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        updateProgress();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error: The backup seems to be corrupted.", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    @Override
    public void onWillImportChange(int index, Boolean willImport) {
        dictionary.get(index).setWillImport(willImport);
    }

    @Keep
    public class ImportObject {
        String name;
        String author;
        String folderId;
        boolean error = false;
        Boolean willImport = true;

        ImportObject(String folderId, String name, String author) {
            this.name = name;
            this.author = author;
            this.error = true;
            this.folderId = folderId;
            this.willImport = false;
        }

        List<File> files;
        File folderDetails;

        ImportObject(String folderId, List<File> files, File folderDetails) {
            this.folderId = folderId;
            this.files = files;
            this.folderDetails = folderDetails;
        }

        public boolean ifError() {
            return this.error;
        }

        public boolean ifImport() {
            return this.willImport;
        }

        public List<File> getFiles() {
            return files;
        }

        public File getFolderDetails() {
            return folderDetails;
        }

        public String getFolderId() {
            return folderId;
        }

        public String getName() {
            return name;
        }
        public String getAuthor() {
            return author;
        }

        public void setWillImport(Boolean willImport) {
            this.willImport = willImport;
        }
    }
}