package lor.and.company.driver.adapters;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.Task;

import java.lang.reflect.Array;
import java.util.ArrayList;

import lor.and.company.driver.CollectionActivity;
import lor.and.company.driver.WallpapersActivity;
import lor.and.company.driver.R;
import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.helpers.DBHelper.CollectionsDB;
import lor.and.company.driver.models.Collection;

public class CollectionRecyclerAdapter extends RecyclerView.Adapter {

    private static final String TAG = "CollectionRecyclerAdapt";

    Context context;
    ArrayList<Collection> collections;
    String orientation;
    SharedPreferences preferences;

    public CollectionRecyclerAdapter(Context context, String orientation, ArrayList<Collection> collections) {
        this.collections = collections;
        this.context = context;
        this.orientation = orientation;

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public class CollectionViewHolder extends ViewHolder {
        TextView title, author, wallCount;
        ImageView image, deleteButton, copyButton;
        View gradient;
        ConstraintLayout background;
        ProgressBar progressBar;
        Button retry, retryCollection;
        LinearLayout failedView, errorView;
        CardView baseView;
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
        }

        public void bindData(final Collection collection, Context context) {
            progressBar.setVisibility(View.VISIBLE);
            failedView.setVisibility(View.GONE);
            title.setText(collection.getName());
            author.setText(collection.getOwner());

            Animation animFadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
            Animation animFadeInFaster = AnimationUtils.loadAnimation(context, R.anim.faster_fade);

            animFadeIn.reset();

            if (collection.isError()) {
                title.setTextColor(Color.WHITE);
                author.setTextColor(Color.WHITE);
                wallCount.setVisibility(View.GONE);
                copyButton.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);

                progressBar.setVisibility(View.GONE);
                image.setVisibility(View.INVISIBLE);

                GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ColorUtils.setAlphaComponent(Color.RED, 0), Color.RED});
                gradient.setBackground(gradientDrawable);
                gradient.startAnimation(animFadeInFaster);

                errorView.setVisibility(View.VISIBLE);

                retryCollection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "onClick: Clicked retry");
                        progressBar.setVisibility(View.VISIBLE);
                        errorView.setVisibility(View.INVISIBLE);
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                bindData(collection, context);
                            }
                        }, 1000);
                    }
                });
            } else {
                Glide.with(itemView.getContext())
                        .asBitmap()
                        .load(collection.getThumbnailLink())
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                Palette palette = Palette.from(resource).generate();
                                Swatch swatch = palette.getDominantSwatch();

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

                                title.setTextColor(swatch.getTitleTextColor());
                                author.setTextColor(swatch.getBodyTextColor());
                                wallCount.setTextColor(swatch.getBodyTextColor());
                                deleteButton.setColorFilter(swatch.getTitleTextColor(), android.graphics.PorterDuff.Mode.SRC_IN);
                                copyButton.setColorFilter(swatch.getTitleTextColor(), android.graphics.PorterDuff.Mode.SRC_IN);
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
                                        bindData(collection, context);
                                    }
                                });
                            }
                        });

                wallCount.setText(collection.getCount() + " Wallpapers");
                baseView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, WallpapersActivity.class);
                        intent.putExtra("collection", collection);
                        ((Activity)context).startActivity(intent);
                    }
                });
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
                    new DBHelper.CollectionsDB(context).deleteCollection(collection);
                    collections.remove(collections.indexOf(collection));
                    notifyDataSetChanged();
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (orientation.equals("horizontal")) {
            return new CollectionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_collection_horizontal, parent, false));
        } else {
            return new CollectionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_collection, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((CollectionViewHolder)holder).bindData(collections.get(position), context);
    }

    @Override
    public int getItemCount() {
        return collections.size();
    }
}
