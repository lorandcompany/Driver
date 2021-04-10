package lor.and.company.driver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.drive.Drive;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.helpers.DriveHelper;

public class ImportActivity extends AppCompatActivity implements RefresherListener{

    private static final String TAG = "ImportActivity";

    Context context;
    TextView loadText;
    ProgressBar progressBar;
    LinearLayout whatToDo, statusLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        context = this;

        Button append = findViewById(R.id.append);
        Button replace = findViewById(R.id.replace);

        Uri data = getIntent().getData();

        Log.d("Logd", "onCreate: " + data.toString());

        TextView status = findViewById(R.id.status);
        progressBar = findViewById(R.id.progressBar3);

        whatToDo = findViewById(R.id.whatToDo);
        statusLayout = findViewById(R.id.statusLayout);

        loadText = findViewById(R.id.loadText);
        append.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                status.setText("Appending Folders");
                whatToDo.setVisibility(View.INVISIBLE);
                statusLayout.setVisibility(View.VISIBLE);
                ContentResolver contentResolver = context.getContentResolver();
                new AsyncTask<Void, Integer, Integer>(){
                    int count;
                    int max;

                    @Override
                    protected Integer doInBackground(Void... voids) {
                        try {
                            DBHelper.CollectionsDB collectionsDB = new DBHelper.CollectionsDB(context);

                            InputStream inputStream = contentResolver.openInputStream(data);
                            String[] ids = IOUtils.toString(inputStream, "UTF-8").split("\r\n");
                            inputStream.close();

                            max = ids.length;
                            count = 0;
                            publishProgress(count);
                            int errorcount = 0;
                            for (String id : ids) {
                                count += 1;
                                if (!collectionsDB.ifExists(id)) {
                                    Drive drive = DriveHelper.getDrive(context);
                                    publishProgress(count);
                                    try {
                                        collectionsDB.addCollection(drive, id);
                                    } catch (Exception e) {
                                        errorcount += 1;
                                    }

                                }
                            }
                            return errorcount;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return -1;
                        }

                    }

                    @Override
                    protected void onPostExecute(Integer errorcount) {
                        super.onPostExecute(errorcount);
                        if (errorcount == 0) {
                            Toast.makeText(context, "Folders imported successfully!", Toast.LENGTH_SHORT).show();
                        } else if (errorcount > 0) {
                            if (errorcount == 1) {
                                Toast.makeText(context, "Folders partially imported!\n\nA collection failed to import.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Folders partially imported!\n\n" + errorcount + " collections failed to import.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Importing failed.", Toast.LENGTH_SHORT).show();
                        }
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    protected void onProgressUpdate(Integer... values) {
                        super.onProgressUpdate(values);
                        onRefreshUpdate(values[0], max);
                    }
                }.execute();
            }
        });

        replace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                status.setText("Replacing Folders");
                whatToDo.setVisibility(View.INVISIBLE);
                statusLayout.setVisibility(View.VISIBLE);
                ContentResolver contentResolver = context.getContentResolver();

                new AsyncTask<Void, Integer, Integer>() {
                    int count = 0;
                    int max;

                    @Override
                    protected Integer doInBackground(Void... voids) {
                        try {
                            DBHelper.CollectionsDB collectionsDB = new DBHelper.CollectionsDB(context);
                            collectionsDB.db.execSQL("DELETE FROM CollectionsDB");
                            new DBHelper.WallpaperDB(context).db.execSQL("DELETE FROM WallpaperDB");

                            InputStream inputStream = contentResolver.openInputStream(data);
                            String[] ids = IOUtils.toString(inputStream, "UTF-8").split("\r\n");
                            inputStream.close();

                            max = ids.length;
                            count = 0;
                            publishProgress(count);
                            int errorcount = 0;
                            for (String id : ids) {
                                count += 1;
                                if (!collectionsDB.ifExists(id)) {
                                    Drive drive = DriveHelper.getDrive(context);
                                    publishProgress(count);
                                    try {
                                        collectionsDB.addCollection(drive, id);
                                    } catch (Exception e) {
                                        errorcount += 1;
                                    }
                                }
                            }
                            return errorcount;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return -1;
                        }
                    }

                    @Override
                    protected void onPostExecute(Integer errorcount) {
                        super.onPostExecute(errorcount);
                        if (errorcount == 0) {
                            Toast.makeText(context, "Folders imported successfully!", Toast.LENGTH_SHORT).show();
                        } else if (errorcount > 0) {
                            if (errorcount == 1) {
                                Toast.makeText(context, "Folders partially imported!\n\nA collection failed to import.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Folders partially imported!\n\n" + errorcount + " collections failed to import.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Importing failed.", Toast.LENGTH_SHORT).show();
                        }
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    protected void onProgressUpdate(Integer... values) {
                        super.onProgressUpdate(values);
                        onRefreshUpdate(values[0], max);
                    }
                }.execute();
            }
        });
    }

    @Override
    public void onRefreshUpdate(int progress, int max) {
        progressBar.setMax(max);
        progressBar.setIndeterminate(progress == 0);
        progressBar.setProgress(progress);
        loadText.setText("Loading folder " + progress + " out of " + max);
    }
}