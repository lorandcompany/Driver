package lor.and.company.driver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.ArrayList;
import java.util.Date;

import lor.and.company.driver.adapters.CollectionRecyclerAdapter;
import lor.and.company.driver.helpers.ActivityHelper;
import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.helpers.DBHelper.CollectionsDB;
import lor.and.company.driver.helpers.DriveHelper;
import lor.and.company.driver.models.Collection;
import lor.and.company.driver.services.WallpaperDownloaderService;

import static lor.and.company.driver.helpers.Constants.CODE_ADD_WALL;
import static lor.and.company.driver.helpers.Constants.CODE_SETTINGS;

public class CollectionActivity extends AppCompatActivity implements RefresherListener{

    Context context;
    private static final String TAG = "CollectionActivity";

    @Override
    protected void onResume() {
        super.onResume();
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(context);

        if (googleSignInAccount == null) {
            Intent intent = new Intent(this, GoogleSignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else if (new DBHelper.CollectionsDB(context).getCollections().size() == 0) {
            Intent intent = new Intent(this, AddDriveFolderActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    RecyclerView recyclerView;
    ProgressBar progressBar;
    TextView loadText;
    ConstraintLayout loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        onResume();

        setContentView(R.layout.activity_collection);

        progressBar = findViewById(R.id.progressBar3);
        loadText = findViewById(R.id.loadText);
        loadingView = findViewById(R.id.loadingView);

        recyclerView = findViewById(R.id.collectionRecyclerView);

        SwipeRefreshLayout refresher = findViewById(R.id.refresher);
        SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new AsyncTask<Void, Void, Integer>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        loadingView.setVisibility(View.VISIBLE);
                        progressBar.setProgress(0);
                        progressBar.setIndeterminate(true);
                        recyclerView.setAdapter(null);
                    }

                    @Override
                    protected Integer doInBackground(Void... voids) {
                        Log.d(TAG, "doInBackground: Now refreshing");
                        int errorcount = new DBHelper.CollectionsDB(context).updateCollections(DriveHelper.getDrive(context), (RefresherListener) context);
                        return errorcount;
                    }

                    @Override
                    protected void onPostExecute(Integer errorcount) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

                        super.onPostExecute(errorcount);
                        new LoadCollectionsTask().execute(context);
                        refresher.setRefreshing(false);
                        loadingView.setVisibility(View.GONE);
                        progressBar.setIndeterminate(true);

                        long timestamp = new Date().getTime();

                        if (errorcount == 0) {
                            Toast.makeText(context, "Folders successfully updated!", Toast.LENGTH_SHORT).show();
                            preferences.edit().putLong("lastRefresh", timestamp);
                        } else {
                            if (new DBHelper.CollectionsDB(context).getCollections().size() == errorcount) {
                                Toast.makeText(context, "Refresh failed. All collections failed to refresh.", Toast.LENGTH_SHORT).show();
                            } else if (errorcount == 1) {
                                Toast.makeText(context, "Folders partially updated!\n\nA collection failed to load.", Toast.LENGTH_SHORT).show();
                                preferences.edit().putLong("lastRefresh", timestamp);
                            } else {
                                Toast.makeText(context, "Folders partially updated!\n\n" + errorcount + " collections failed to load.", Toast.LENGTH_SHORT).show();
                                preferences.edit().putLong("lastRefresh", timestamp);
                            }
                        }
                    }
                }.execute();
            }
        };

        refresher.setOnRefreshListener(refreshListener);

        ActivityHelper.setupActionBar(this);

        long timestamp = new Date().getTime();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            if (preferences.getLong("lastRefresh", timestamp) + 3600000 < timestamp) {
                refresher.setRefreshing(true);
                refreshListener.onRefresh();
            }
        } finally {
            new LoadCollectionsTask().execute(this);
        }
    }

    @Override
    public void onRefreshUpdate(int progress, int max) {
        progressBar.setIndeterminate(false);
        progressBar.setMax(max);
        progressBar.setProgress(progress);
        loadText.post(new Runnable() {
                          @Override
                          public void run() {
                              loadText.setText("Loading Folder " + progress + " out of " + max);
                          }
                      });
    }

    String orientation = "horizontal";

    public class LoadCollectionsTask extends AsyncTask<Context, Void, Void> {

        Context context;
        CollectionRecyclerAdapter adapter;
        CollectionsDB collectionsDB;
        ArrayList<Collection> collections;

        @Override
        protected Void doInBackground(Context... contexts) {
            context = contexts[0];
            collectionsDB = new DBHelper.CollectionsDB(context);
            collections = collectionsDB.getCollections();
            adapter = new CollectionRecyclerAdapter(context, orientation, collections);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            RecyclerView recyclerView = findViewById(R.id.collectionRecyclerView);
            if (orientation.equals("horizontal")) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
                try {
                    PagerSnapHelper snapHelper = new PagerSnapHelper();
                    snapHelper.attachToRecyclerView(recyclerView);
                } catch (Exception ignored) {}
            } else {
                recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
                ItemTouchHelper.Callback itemTouchHelperCallback;
                ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                        int position = viewHolder.getAdapterPosition();
                        DBHelper.CollectionsDB collectionsDB = new DBHelper.CollectionsDB(context);
                        Collection collection = collectionsDB.getCollections().get(position);
                        collectionsDB.deleteCollection(collection);
                        Toast.makeText(context, "Collection deleted.", Toast.LENGTH_SHORT).show();
                        new LoadCollectionsTask().execute(context);
                    }
                };
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
                itemTouchHelper.attachToRecyclerView(recyclerView);
            }
            recyclerView.setAdapter(adapter);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CODE_ADD_WALL || requestCode == CODE_SETTINGS) {
                new LoadCollectionsTask().execute(context);
            }
        }
    }
}