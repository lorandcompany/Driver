package lor.and.company.driver.adapters;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.palette.graphics.Palette.Swatch;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import lor.and.company.driver.WallpapersActivity;
import lor.and.company.driver.R;
import lor.and.company.driver.adapters.filters.CollectionsFilter;
import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.models.Collection;

public class CollectionRecyclerAdapter extends RecyclerView.Adapter implements Filterable {

    private static final String TAG = "CollectionRecyclerAdapt";

    Context context;
    public ArrayList<Collection> collections;
    public ArrayList<Collection> filteredCollections;
    String orientation;
    SharedPreferences preferences;
    CollectionController collectionController;
    DBHelper.CollectionsDB collectionsDB;

    boolean multiselection = false;

    public CollectionRecyclerAdapter(Context context, ArrayList<Collection> collections, CollectionController collectionController) {
        this.collections = collections;
        this.context = context;

        filteredCollections = collections;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        orientation = preferences.getString("layout", "compact");
        this.collectionController = collectionController;
        collectionsDB = new DBHelper.CollectionsDB(context);
    }

    @Override
    public Filter getFilter() {
        return new CollectionsFilter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint, String order, String orderBy) {
                String charString = constraint.toString().toLowerCase();
                ArrayList<Collection> filteredList;
                if (charString.isEmpty()) {
                    filteredList = collections;
                } else {
                    filteredList = new ArrayList<>();
                    for (Collection collection : collections) {
                        if (collection.getName().toLowerCase().contains(charString)) {
                            filteredList.add(collection);
                        }
                    }
                }
                switch (orderBy) {
                    case "Folder Name": {
                        filteredList.sort(new Comparator<Collection>() {
                            @Override
                            public int compare(Collection o1, Collection o2) {
                                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                            }
                        });
                        break;
                    }
                    case "Author": {
                        filteredList.sort(new Comparator<Collection>() {
                            @Override
                            public int compare(Collection o1, Collection o2) {
                                return o1.getOwner().toLowerCase().compareTo(o2.getOwner().toLowerCase());
                            }
                        });
                        break;
                    }
                    case "Image count": {
                        filteredList.sort(new Comparator<Collection>() {
                            @Override
                            public int compare(Collection o1, Collection o2) {
                                return Integer.compare(o2.getCount(), o1.getCount());
                            }
                        });
                        break;
                    }
                    case "Recently Updated": {
                        Log.d(TAG, "performFiltering: Started last updated");
                        filteredList.sort(new Comparator<Collection>() {
                            @Override
                            public int compare(Collection o1, Collection o2) {
                                return Long.compare(o2.getModifiedTime(), o1.getModifiedTime());
                            }
                        });
                        break;
                    }
                }
                filteredCollections = filteredList;

                if (preferences.getString("collectionOrder", "desc").equals("desc")){
                    Collections.reverse(filteredCollections);
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredCollections;
                return filterResults;
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                Log.d(TAG, "performFiltering: " +constraint);
                String[] variables = constraint.toString().split("::__::");
                if (variables[0] == null) {
                    variables[0] = "";
                }
                Log.d(TAG, "Search: " + variables[0]);
                Log.d(TAG, "Order: " + variables[1]);
                Log.d(TAG, "OrderBy: " + variables[2]);
                return performFiltering(variables[0], variables[1], variables[2]);
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredCollections = (ArrayList<Collection>)results.values;
                notifyDataSetChanged();
            }
        };
    }



    public class CollectionViewHolder extends ViewHolder {
        TextView title, author, wallCount;
        ImageView image, deleteButton, copyButton;
        View gradient;
        ProgressBar progressBar;
        Button retry, retryCollection;
        LinearLayout failedView, errorView;
        CardView baseView;
        ConstraintLayout selectionView;
        OnClickListener open;
        int position;

        public CollectionViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar2);
            title = itemView.findViewById(R.id.title);
            author = itemView.findViewById(R.id.author);
            wallCount = itemView.findViewById(R.id.wallCount);
            image = itemView.findViewById(R.id.heroImage);
            deleteButton = itemView.findViewById(R.id.deleteIcon);
            copyButton = itemView.findViewById(R.id.copyIcon);
            gradient = itemView.findViewById(R.id.gradientOverlay);
            retry = itemView.findViewById(R.id.retryLoad);
            failedView = itemView.findViewById(R.id.failedView);
            errorView = itemView.findViewById(R.id.errorView);
            retryCollection = itemView.findViewById(R.id.retryCollectionsLoad);
            baseView = itemView.findViewById(R.id.baseView);
            selectionView = itemView.findViewById(R.id.selectedView);
        }

        public void bindData(final Collection collection, final int position) {
            this.position = position;
            progressBar.setVisibility(View.VISIBLE);
            failedView.setVisibility(View.GONE);
            title.setText(collection.getName());
            author.setText(collection.getOwner());

            Animation animFadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
            Animation animFadeInFaster = AnimationUtils.loadAnimation(context, R.anim.faster_fade);

            animFadeIn.reset();

            if (multiselection){
                if (collection.getSelected()) {
                    selectionView.setVisibility(View.VISIBLE);
                    deleteButton.setVisibility(View.GONE);
                } else {
                    selectionView.setVisibility(View.GONE);
                }
                deleteButton.setVisibility(View.GONE);
            } else {
                selectionView.setVisibility(View.GONE);
            }

            if (collection.isError()) {
                TextView errorMessage = itemView.findViewById(R.id.errorMessage);
                errorMessage.setText(collection.getErrorMessage());

                title.setTextColor(Color.WHITE);
                author.setTextColor(Color.WHITE);
                wallCount.setVisibility(View.GONE);
                copyButton.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);

                progressBar.setVisibility(View.GONE);
                image.setVisibility(View.INVISIBLE);

                GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ColorUtils.setAlphaComponent(Color.RED, 0), ColorUtils.setAlphaComponent(Color.RED, 100)});
                if (preferences.getBoolean("animations", true)) {
                    gradient.setBackground(gradientDrawable);
                    gradient.startAnimation(animFadeInFaster);
                } else {
                    gradient.setBackground(gradientDrawable);
                }

                errorView.setVisibility(View.VISIBLE);

                if (multiselection) {
                    retryCollection.setOnClickListener(null);
                } else {
                    retryCollection.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick: Clicked retry");

                            errorMessage.setText(collection.getErrorMessage());

                            progressBar.setVisibility(View.VISIBLE);
                            errorView.setVisibility(View.INVISIBLE);
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    bindData(collection, position);
                                }
                            }, 1000);
                        }
                    });
                }
            }
            else {
                Glide.with(itemView.getContext())
                        .asBitmap()
                        .load(collection.getThumbnailLink())
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                Palette palette = Palette.from(resource).generate();
                                Swatch swatch = palette.getDominantSwatch();

                                int textColor;

                                if (ColorUtils.calculateContrast(Color.WHITE, palette.getDominantColor(Color.WHITE)) > ColorUtils.calculateContrast(Color.BLACK,palette.getDominantColor(Color.WHITE))) {
                                    textColor = Color.WHITE;
                                } else {
                                    textColor = Color.BLACK;
                                }

                                GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ColorUtils.setAlphaComponent(palette.getDominantColor(0), 0), palette.getDominantColor(0)});

                                if (preferences.getBoolean("animations", true) == true) {
                                    image.clearAnimation();
                                    gradient.clearAnimation();
                                    image.setImageBitmap(resource);
                                    gradient.setBackground(gradientDrawable);

                                    image.startAnimation(animFadeIn);
                                    gradient.startAnimation(animFadeIn);
                                } else {
                                    image.setImageBitmap(resource);
                                    gradient.setBackground(gradientDrawable);
                                }

                                progressBar.setVisibility(View.GONE);

                                title.setTextColor(textColor);
                                author.setTextColor(textColor);
                                wallCount.setTextColor(textColor);
                                deleteButton.setColorFilter(textColor, android.graphics.PorterDuff.Mode.SRC_IN);
                                copyButton.setColorFilter(textColor, android.graphics.PorterDuff.Mode.SRC_IN);
                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                super.onLoadFailed(errorDrawable);
                                progressBar.setVisibility(View.GONE);
                                failedView.setVisibility(View.VISIBLE);
                                retry.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        progressBar.setVisibility(View.VISIBLE);
                                        bindData(collection, position);
                                    }
                                });
                            }
                        });

                wallCount.setText(collection.getCount() + " Wallpapers");

                if (!multiselection) {
                    open = new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            collectionController.loadCollection(collection);
                        }
                    };
                } else {
                    open = new View.OnClickListener(){
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "onClick: Clicked");
                            boolean hit = false;
                            int mainPos = collections.indexOf(collection);
                            Collection mainCollection = collections.get(mainPos);
                            mainCollection.setSelected(!mainCollection.getSelected());
                            collections.set(mainPos, mainCollection);
                            filteredCollections.set(position, mainCollection);
                            notifyItemChanged(position);
                            int count = 0;
                            for (Collection c: collections) {
                                if (c.getSelected()) {
                                    hit = true;
                                    count += 1;
                                }
                            }
                            if (!hit) {
                                stopMultiselection();
                            }
                            collectionController.onMultiSelectionChanged(count);
                        }
                    };
                }
                baseView.setOnClickListener(open);
            }
            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager clipboard = context.getSystemService(ClipboardManager.class);
                    ClipData clip = ClipData.newPlainText("Drive Link", collection.getFolderLink());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Drive link copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    new DBHelper.CollectionsDB(context).deleteCollection(collection);
                    collectionController.deleteCollection(collection, position);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (multiselection) {
                        return false;
                    } else {
                        multiselection = true;
                        int mainPos = collections.indexOf(collection);
                        Collection mainCollection = collections.get(mainPos);
                        mainCollection.setSelected(true);
                        collections.set(mainPos, mainCollection);
                        filteredCollections.set(position, mainCollection);
                        notifyItemChanged(position);
                        collectionController.startMultiselection();
                        ((SimpleItemAnimator)recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
                        notifyDataSetChanged();
                        return true;
                    }
                }
            });
        }
    }

    public boolean isOnMultiselection(){
        return multiselection;
    }

    public void stopMultiselection() {
        multiselection = false;
        for (Collection collection: collections){
            collection.setSelected(false);
        }
        for (Collection collection: filteredCollections){
            collection.setSelected(false);
        }
        notifyDataSetChanged();
        collectionController.stopMultiselection();
        ((SimpleItemAnimator)recyclerView.getItemAnimator()).setSupportsChangeAnimations(true);
    }

    RecyclerView recyclerView;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }


    public void undo(Collection collection, int position){
        collections.add(position, collection);
        notifyItemChanged(position);
        notifyItemRangeChanged(position, collections.size());
    }

    public void deleteCollection(int position) {
        collections.remove(position);
        notifyItemChanged(position);
        notifyItemRangeChanged(position, collections.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (orientation.equals("immersive")) {
            return new CollectionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_collection_horizontal, parent, false));
        } else {
            return new CollectionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_collection, parent, false));
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((CollectionViewHolder)holder).bindData(filteredCollections.get(position), position);
    }

    @Override
    public int getItemCount() {
        return filteredCollections.size();
    }

    public interface CollectionController {
        void deleteCollection(Collection collection, int position);
        void loadCollection(Collection collection);
        void startMultiselection();
        void stopMultiselection();
        void onMultiSelectionChanged(int selectedAmount);
    }
}
