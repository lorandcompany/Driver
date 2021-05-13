package lor.and.company.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.math.MathUtils;
import androidx.customview.widget.ViewDragHelper;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Date;

import lor.and.company.driver.adapters.CollectionRecyclerAdapter;
import lor.and.company.driver.adapters.filters.CollectionsFilter;
import lor.and.company.driver.helpers.ActivityHelper;
import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.helpers.DBHelper.CollectionsDB;
import lor.and.company.driver.helpers.DriveHelper;
import lor.and.company.driver.helpers.NetworkHelper;
import lor.and.company.driver.helpers.NetworkHelper.NetworkListener;
import lor.and.company.driver.models.Collection;

import static lor.and.company.driver.helpers.Constants.CODE_ADD_WALL;
import static lor.and.company.driver.helpers.Constants.CODE_SETTINGS;

public class CollectionActivity extends AppCompatActivity implements RefresherListener, DBHelper.FinishCallback {

    Context context;
    private static final String TAG = "CollectionActivity";

    @Override
    protected void onResume() {
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
        super.onResume();
    }

    RecyclerView recyclerView;
    ProgressBar progressBar;
    TextView loadText;
    ConstraintLayout loadingView;

    SwipeRefreshLayout refresher;

    public Boolean connected;

    SharedPreferences preferences;

    LinearLayout networkNotification;
    NetworkListener listener = new NetworkListener() {
        @Override
        public void isNetworkConnected(Boolean b) {
            runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    connected = b;
                    if (b) {
                        networkNotification.setVisibility(View.GONE);
                    } else {
                        networkNotification.setVisibility(View.VISIBLE);
                    }
                }
            });

        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    TextInputEditText searchBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        onResume();

        setContentView(R.layout.activity_collection);

        collectionsDB = new DBHelper.CollectionsDB(context);

        networkNotification = findViewById(R.id.networkNotification);
        new NetworkHelper(context).registerNetworkCallback(listener);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (!preferences.getBoolean("adfree", false)) {
            AdView adView = findViewById(R.id.adView);
            adView.setVisibility(View.VISIBLE);

            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                }
            });
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }

        progressBar = findViewById(R.id.importProgress);
        loadText = findViewById(R.id.loadText);
        loadingView = findViewById(R.id.loadingView);

        recyclerView = findViewById(R.id.collectionRecyclerView);

        refresher = findViewById(R.id.refresher);
        SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadingView.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                progressBar.setIndeterminate(true);
                recyclerView.setAdapter(null);

                new DBHelper.CollectionsDB(context).updateCollections(DriveHelper.getDrive(context), (RefresherListener) context, (DBHelper.FinishCallback) context);
            }
        };
        refresher.setOnRefreshListener(refreshListener);

        ImageView add = findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("BUTTON", "onClick: Clicked");
                Intent intent = new Intent(context, AddDriveFolderActivity.class);
                startActivityForResult(intent, CODE_ADD_WALL);
            }
        });

        ImageView settings = findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("BUTTON", "onClick: Clicked");
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivityForResult(intent, CODE_SETTINGS);
            }
        });

        //Setup filters
        searchBox = findViewById(R.id.searchBox);
        AutoCompleteTextView orderByDropdown = findViewById(R.id.orderByDropdown);
        ImageView orderButton = findViewById(R.id.orderButton);

        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: CLICKED ORDERBUTTON");
                if (preferences.getString("collectionOrder", "asc").equals("asc")) {
                    preferences.edit().putString("collectionOrder", "desc").apply();
                } else {
                    preferences.edit().putString("collectionOrder", "asc").apply();
                }
                String order = preferences.getString("collectionOrder","asc");
                adapter.getFilter().filter(searchBox.getText()+"::__::"+order+"::__::"+orderByDropdown.getText().toString());
            }
        });

        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: CLICKED ORDERBUTTON");
                if (preferences.getString("collectionOrder", "asc").equals("asc")) {
                    preferences.edit().putString("collectionOrder", "desc").apply();
                } else {
                    preferences.edit().putString("collectionOrder", "asc").apply();
                }
                String order = preferences.getString("collectionOrder","asc");
                adapter.getFilter().filter(searchBox.getText()+"::__::"+order+"::__::"+orderByDropdown.getText().toString());
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String order = preferences.getString("collectionOrder","asc");
                adapter.getFilter().filter(s+"::__::"+order+"::__::"+orderByDropdown.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        String[] list = getResources().getStringArray(R.array.orderBy);
        ArrayAdapter<String> orderByAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, list);
        orderByDropdown.setAdapter(orderByAdapter);
        orderByDropdown.setText(preferences.getString("collectionOrderBy", "Last Updated"),false);
        orderByDropdown.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                preferences.edit().putString("position", orderByAdapter.getItem(position)).apply();
                String order = preferences.getString("collectionOrder","asc");
                adapter.getFilter().filter(searchBox.getText()+"::__::"+order+"::__::"+orderByDropdown.getText().toString());
            }
        });

        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: CLUCKED BITCHJ");
                if (preferences.getString("collectionOrder", "desc").equals("desc")) {
                    orderButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.sort_descending, null));
                    preferences.edit().putString("collectionOrder", "asc").apply();
                } else {
                    orderButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.sort_ascending, null));
                    preferences.edit().putString("collectionOrder", "desc").apply();
                }
                String order = preferences.getString("collectionOrder","asc");
                adapter.getFilter().filter(searchBox.getText()+"::__::"+order+"::__::"+orderByDropdown.getText().toString());
            }
        });

        // Fully reload folders to refresh thumbnails after one hour
        long timestamp = new Date().getTime();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getLong("lastRefresh", timestamp) + 3600000 < timestamp) {
            refresher.setRefreshing(true);
            refreshListener.onRefresh();
        } else {
            new LoadCollectionsTask().execute(this);

        }

        ConstraintLayout filterView = findViewById(R.id.filterLayout);
        ImageView filterToggle = findViewById(R.id.filterToggle);
        ImageView filterToggle2 = findViewById(R.id.filterToggle2);
        filterToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filterView.getVisibility() == View.GONE) {
                    filterView.setVisibility(View.VISIBLE);
                } else {
                    filterView.setVisibility(View.GONE);
                }
            }
        });
        filterToggle2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filterView.getVisibility() == View.GONE) {
                    filterView.setVisibility(View.VISIBLE);
                } else {
                    filterView.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onRefreshUpdate(int progress, int max) {
        progressBar.setIndeterminate(progress == 0);
        progressBar.setMax(max);
        progressBar.setProgress(progress);
        loadText.post(new Runnable() {
                          @Override
                          public void run() {
                              loadText.setText("Loading Folder " + progress + " out of " + max);
                          }
                      });
    }

    @Override
    public void finished(int errorcount) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        new LoadCollectionsTask().execute(context);

        refresher.setRefreshing(false);
        loadingView.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);

        long timestamp = new Date().getTime();

        if (errorcount == 0) {
            Toast.makeText(context, "Folders successfully updated!", Toast.LENGTH_SHORT).show();
            preferences.edit().putLong("lastRefresh", timestamp).apply();
        } else {
            if (new DBHelper.CollectionsDB(context).getCollections().size() == errorcount) {
                if (!connected){
                    Toast.makeText(context, "Refresh failed. You're probably not connected to the internet.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Refresh failed. All collections failed to refresh.", Toast.LENGTH_SHORT).show();
                }
            } else if (errorcount == 1) {
                Toast.makeText(context, "Folders partially updated!\n\nA collection failed to load.", Toast.LENGTH_SHORT).show();
                preferences.edit().putLong("lastRefresh", timestamp).apply();
            } else {
                Toast.makeText(context, "Folders partially updated!\n\n" + errorcount + " collections failed to load.", Toast.LENGTH_SHORT).show();
                preferences.edit().putLong("lastRefresh", timestamp).apply();
            }
        }
    }

    CardView undoContainer;
    TextView undoText;
    Button undoButton;

    public void onDelete(Collection collection, int position, boolean irreversible) {
        undoContainer = findViewById(R.id.undoView);
        undoText = findViewById(R.id.undoText);
        undoButton = findViewById(R.id.undo);

        undoContainer.setVisibility(View.VISIBLE);

        Runnable showUndo = new Runnable() {
            @Override
            public void run() {
                undoContainer.setVisibility(View.GONE);
            }
        };

        Handler handler = new Handler();
        handler.postDelayed(showUndo, 3000);

        if (!irreversible) {
            undoText.setText("Deleted " + collection.getName());
            undoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handler.removeCallbacks(showUndo);
                    DBHelper.DeletedDB deletedDB = new DBHelper.DeletedDB(context);
                    deletedDB.undo();
                    adapter.undo(collection, position);
                    undoContainer.setVisibility(View.GONE);
                }
            });
        } else {
            undoText.setText("Deleted " + position + " folder/s.");
            undoButton.setVisibility(View.GONE);
        }

    }

    PagerSnapHelper snapHelper = new PagerSnapHelper();
    CollectionRecyclerAdapter adapter;
    ConstraintLayout selectionView;
    TextView selectedText;
    ImageView stopMultiselection;
    ImageView massDelete;

    CollectionRecyclerAdapter.CollectionController deleteListener = new CollectionRecyclerAdapter.CollectionController() {
        @Override
        public void deleteCollection(Collection collection, int position) {
            collectionsDB.deleteCollection(collection);
            adapter.deleteCollection(position);
            onDelete(collection, position, false);
        }

        @Override
        public void loadCollection(Collection collection) {
            Intent intent = new Intent(context, WallpapersActivity.class);
            intent.putExtra("collection", collection);
            startActivity(intent);
        }

        @Override
        public void onMultiSelectionChanged(int selectedAmount) {
            selectedText.setText(selectedAmount + " selected");
        }

        @Override
        public void startMultiselection() {
            Log.d(TAG, "startMultiselection: started multiselection1");


            refresher.setEnabled(false);
            selectionView = findViewById(R.id.selectionControls);
            massDelete = findViewById(R.id.deleteSelected);
            stopMultiselection = findViewById(R.id.cancelSelection);
            selectedText = findViewById(R.id.selectedText);

            selectionView.setVisibility(View.VISIBLE);

            selectedText.setText("1 selected");

            stopMultiselection.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.stopMultiselection();
                }
            });

            massDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConstraintLayout deleteDialog = findViewById(R.id.deleteConfirmation);
                    Button yes = findViewById(R.id.deleteYes);
                    Button no = findViewById(R.id.deleteNo);
                    TextView deleteText = findViewById(R.id.deleteText);

                    int counter = 0;
                    for (Collection collection: adapter.collections){
                        if (collection.getSelected()) {
                            counter += 1;
                        }
                    }

                    deleteText.setText("Are you sure you want to delete " + counter + " folder/s? This action cannot be undone.");

                    int finalCounter = counter;
                    yes.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for (Collection collection: adapter.collections){
                                if (collection.getSelected()) {
                                    collectionsDB.deleteCollection(collection);
                                }
                            }
                            adapter.stopMultiselection();
                            adapter.notifyDataSetChanged();
                            deleteDialog.setVisibility(View.GONE);
                            onDelete(null, finalCounter, true);
                            new LoadCollectionsTask().execute(context);
                        }
                    });

                    no.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteDialog.setVisibility(View.GONE);
                        }
                    });

                    deleteDialog.setVisibility(View.VISIBLE);
                }
            });

            Log.d(TAG, "startMultiselection: started multiselection3");

            refresher.setEnabled(false);
        }

        @Override
        public void stopMultiselection() {
            selectionView.setVisibility(View.GONE);
            refresher.setEnabled(true);
        }
    };

    CollectionsDB collectionsDB;

    public class LoadCollectionsTask extends AsyncTask<Context, Void, Void> {
        ArrayList<Collection> collections;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            recyclerView.setVisibility(View.GONE);
            loadingView.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            progressBar.setIndeterminate(true);
            recyclerView.setAdapter(null);
            refresher.setRefreshing(true);
        }

        @Override
        protected Void doInBackground(Context... contexts) {
            collections = collectionsDB.getCollections();

            adapter = new CollectionRecyclerAdapter(context, collections, deleteListener);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Resources r = getResources();

            RecyclerView recyclerView = findViewById(R.id.collectionRecyclerView);
            if (preferences.getString("layout", "compact").equals("immersive")) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
                try {
                    snapHelper.attachToRecyclerView(recyclerView);
                } catch (Exception ignored) {

                }
                float side = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32,r.getDisplayMetrics());
                float topbottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,r.getDisplayMetrics());
                recyclerView.setPadding((int)side,(int)topbottom,(int)side,(int)topbottom);
                ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP) {
                    @Override
                    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                        if (adapter.isOnMultiselection()) {
                            return 0;
                        } else {
                            return super.getSwipeDirs(recyclerView, viewHolder);
                        }
                    }

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                        int position = viewHolder.getAdapterPosition();
                        DBHelper.CollectionsDB collectionsDB = new DBHelper.CollectionsDB(context);
                        Collection collection = collectionsDB.getCollections().get(position);
                        deleteListener.deleteCollection(collection, position);
                    }

                    final Drawable background = context.getDrawable(R.drawable.rounded_rectangle);
                    final Drawable icon = context.getDrawable(R.drawable.delete);

                    float dip = 1f;
                    Resources r = getResources();
                    float px = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            dip,
                            r.getDisplayMetrics()
                    );
                    Paint font = new Paint();

                    @Override
                    public void onChildDraw (Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive){
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                        if (isCurrentlyActive) {
                            View child = recyclerView.getLayoutManager().findViewByPosition(viewHolder.getAdapterPosition());
                            int threshold = (child.getBottom()-child.getTop())/2;
                            Log.d(TAG, "dY: " + dY);
                            Log.d(TAG, "threshold: " + MathUtils.clamp((255f/threshold)*Math.abs(dY), 0, 255));
                            background.setAlpha((int) MathUtils.clamp((255f/threshold)*Math.abs(dY), 0, 255));
                            background.setBounds(child.getLeft(), child.getTop(),child.getRight(), child.getBottom());
                            background.draw(c);
                            int centerX =  ((child.getRight() - child.getLeft())/2) + child.getLeft();
                            icon.setBounds((int)((centerX)-(18*px)),
                                    (int)((child.getBottom())-(60*px)),
                                    (int) (centerX+(px*18)),
                                    (int)(child.getBottom()-(24*px)));
                            icon.draw(c);
                            font.setTextSize(30);
                            font.setColor(Color.WHITE);
                            font.setUnderlineText(false);
                            font.setFakeBoldText(false);
                            font.setStrikeThruText(false);
                            font.setAntiAlias(true);
                            font.setSubpixelText(true);
                            font.setTextAlign(Paint.Align.CENTER);
                            if (MathUtils.clamp((255f/threshold)*Math.abs(dY), 0,255) != 255) {
                                Log.d(TAG, "onChildDraw: Not on threshold");
                                c.drawText("Swipe more to delete", centerX, child.getBottom() - (px*72), font);
                            } else {
                                Log.d(TAG, "onChildDraw: On threshold");
                                c.drawText("Release to delete", centerX, child.getBottom() - (px*72), font);
                            }

                            icon.setTint(Color.WHITE);
                        }

                    }
                };
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
                itemTouchHelper.attachToRecyclerView(recyclerView);
            } else {
                try {
                    snapHelper.attachToRecyclerView(null);
                } catch (Exception ignored) {}
                recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
                ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    @Override
                    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                        if (adapter.isOnMultiselection()) {
                            return 0;
                        } else {
                            return super.getSwipeDirs(recyclerView, viewHolder);
                        }
                    }

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;
                    }

                    final Drawable background = context.getDrawable(R.drawable.rounded_rectangle);
                    final Drawable icon = context.getDrawable(R.drawable.delete);

                    float dip = 1f;
                    Resources r = getResources();
                    float px = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            dip,
                            r.getDisplayMetrics()
                    );
                    Paint font = new Paint();

                    @Override
                    public void onChildDraw (Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive){
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                        if (isCurrentlyActive) {
                            View child = recyclerView.getLayoutManager().findViewByPosition(viewHolder.getAdapterPosition());
                            int threshold = (child.getRight()-child.getLeft())/2;
                            background.setAlpha((int) MathUtils.clamp((255f/threshold)*Math.abs(dX), 0, 255));
                            background.setBounds(child.getLeft(), child.getTop(),child.getRight(), child.getBottom());
                            background.draw(c);
                            int top =  ((child.getBottom()-child.getTop())/2 + child.getTop());
                            icon.setBounds((int)(child.getRight()-(px*36)),
                                    (int)((top)-(12*px)),
                                    (int) (child.getRight()-(px*12)),
                                    (int)((top)+(12*px)));
                            icon.draw(c);
                            font.setTextSize(30);
                            font.setColor(Color.WHITE);
                            font.setUnderlineText(false);
                            font.setFakeBoldText(false);
                            font.setStrikeThruText(false);
                            font.setAntiAlias(true);
                            font.setSubpixelText(true);
                            font.setTextAlign(Paint.Align.RIGHT);
                            float fontHeight = font.getFontMetrics().top + font.getFontMetrics().bottom;
                            if (MathUtils.clamp((255f/threshold)*Math.abs(dX), 0,255) != 255) {
                                Log.d(TAG, "onChildDraw: Not on threshold");
                                c.drawText("Swipe more to delete", child.getRight() - (px*48), top - (fontHeight/2), font);
                            } else {
                                Log.d(TAG, "onChildDraw: On threshold");
                                c.drawText("Release to delete", child.getRight() - (px*48), top - (fontHeight/2), font);
                            }

                            icon.setTint(Color.WHITE);
                        }
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                        int position = viewHolder.getAdapterPosition();
                        DBHelper.CollectionsDB collectionsDB = new DBHelper.CollectionsDB(context);
                        Collection collection = collectionsDB.getCollections().get(position);
                        deleteListener.deleteCollection(collection, position);
                    }
                };
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
                itemTouchHelper.attachToRecyclerView(recyclerView);
                float topbottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,r.getDisplayMetrics());
                recyclerView.setPadding((int)topbottom,0,(int)topbottom,0);
            }
            adapter.setHasStableIds(true);
            recyclerView.setAdapter(adapter);
            String order = preferences.getString("collectionOrder","asc");
            String orderBy = preferences.getString("collectionOrderBy","asc");
            adapter.getFilter().filter(searchBox.getText()+"::__::"+order+"::__::"+orderBy);
            refresher.setRefreshing(false);
            recyclerView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            new LoadCollectionsTask().execute(context);
        }
    }
}