package lor.and.company.driver;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.IOException;
import java.util.ArrayList;

import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.adapters.WallpaperRecyclerAdapter;
import lor.and.company.driver.models.Collection;
import lor.and.company.driver.models.Wallpaper;

public class

WallpapersActivity extends AppCompatActivity implements RefresherListener {

    private static final String TAG = "Driver";
    Context context;
    Collection collection;
    DBHelper.WallpaperDB wallpaperDB;
    ArrayList<Wallpaper> wallpapers;
    TextView loadText;
    ConstraintLayout loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpapers);

        context = this;

        collection = getIntent().getExtras().getParcelable("collection");

        wallpaperDB = new DBHelper.WallpaperDB(context);
        wallpapers = wallpaperDB.getWallpapers(collection);

        ImageView back = findViewById(R.id.wallpaperBack);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        TextView title = findViewById(R.id.collectionName);

        title.setText(collection.getName());

        RecyclerView wallpaperView = findViewById(R.id.wallpaperRecyclerView);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 2);
        wallpaperView.setLayoutManager(gridLayoutManager);
        WallpaperRecyclerAdapter adapter = new WallpaperRecyclerAdapter(context, collection);
        adapter.setHasStableIds(true);
        wallpaperView.setAdapter(adapter);

        loadText = findViewById(R.id.loadText);
        loadingView = findViewById(R.id.loadingView);

        SwipeRefreshLayout refresher = findViewById(R.id.refresher);
        refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected void onPreExecute() {
                        loadText.setText("Fetching images...");
                        super.onPreExecute();
                        loadingView.setVisibility(View.VISIBLE);
                        wallpaperView.setAdapter(null);
                    }

                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            new DBHelper.CollectionsDB(context).updateCollection(collection, (RefresherListener) context);
                        } catch (IOException e) {
                            Toast.makeText(context, "Loading failed. Please try again.", Toast.LENGTH_SHORT).show();
                            refresher.setRefreshing(false);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        WallpaperRecyclerAdapter adapter = new WallpaperRecyclerAdapter(context, collection);
                        adapter.setHasStableIds(true);
                        wallpaperView.setAdapter(adapter);
                        refresher.setRefreshing(false);
                        loadingView.setVisibility(View.GONE);
                    }
                }.execute();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
    }

    @Override
    public void onRefreshUpdate(int progress, int max) {
        loadText.post(new Runnable() {
            @Override
            public void run() {
                loadText.setText("Found " + progress + " images");
            }
        });
    }
}