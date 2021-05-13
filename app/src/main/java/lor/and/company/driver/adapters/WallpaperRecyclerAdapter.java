package lor.and.company.driver.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;

import lor.and.company.driver.R;
import lor.and.company.driver.WallpaperViewerActivity;
import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.models.Collection;
import lor.and.company.driver.models.Wallpaper;

public class WallpaperRecyclerAdapter extends RecyclerView.Adapter {

    private static final String TAG = "WallpaperRecyclerAdapte";

    DBHelper.WallpaperDB wallpaperDB;
    Context context;
    ArrayList<Wallpaper> wallpapers;
    SharedPreferences preferences;
    DBHelper.FavouriteDB favDB;

    public WallpaperRecyclerAdapter(Context context, Collection collection) {
        this.context = context;
        wallpaperDB = new DBHelper.WallpaperDB(context);
        wallpapers = wallpaperDB.getWallpapers(collection);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        favDB = new DBHelper.FavouriteDB(context);
    }

    public class WallpaperViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView image, favourite;
        Button retry;
        View gradient;
        LinearLayout background;
        ProgressBar progressBar;
        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            image = itemView.findViewById(R.id.heroImage);
            retry = itemView.findViewById(R.id.retry);
            gradient = itemView.findViewById(R.id.wallGradientOverlay);
            background = itemView.findViewById(R.id.background);
            progressBar = itemView.findViewById(R.id.progressBar4);
            favourite = itemView.findViewById(R.id.favourite);
            retry.setVisibility(View.INVISIBLE);
        }

        public void bindData(Wallpaper wallpaper) {
            Log.d(TAG, "onResourceReady: " + wallpaper.getThumbnailLink());

            if (wallpaper.isFavourite()) {
                favourite.setImageDrawable(context.getDrawable(R.drawable.favourited));
            } else {
                favourite.setImageDrawable(context.getDrawable(R.drawable.favourite));
            }

            favourite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!wallpaper.isFavourite()){
                        favDB.addFromFavourites(wallpaper);
                        wallpaper.setFavourite(true);
                        favourite.setImageDrawable(context.getDrawable(R.drawable.favourited));
                    } else {
                        favDB.removeFromFavourites(wallpaper);
                        wallpaper.setFavourite(false);
                        favourite.setImageDrawable(context.getDrawable(R.drawable.favourite));
                    }
                }
            });

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, WallpaperViewerActivity.class);
                    intent.putExtra("wallpaper", wallpaper);
                    context.startActivity(intent);
                }
            });

            title.setText(wallpaper.getName());

            Glide.with(context)
                    .asBitmap()
                    .load(wallpaper.getThumbnailLink())
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Animation animFadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);

                            animFadeIn.reset();

                            progressBar.setVisibility(View.INVISIBLE);
                            Palette palette = Palette.from(resource).generate();

                            int textColor;

                            if (ColorUtils.calculateContrast(Color.WHITE, palette.getDominantColor(Color.WHITE)) > ColorUtils.calculateContrast(Color.BLACK,palette.getDominantColor(Color.WHITE))) {
                                textColor = Color.WHITE;
                            } else {
                                textColor = Color.BLACK;
                            }

                            Log.d(TAG, "onResourceReady: " + wallpaper.getName() + " " + palette.getDominantColor(0));
                            GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{ColorUtils.setAlphaComponent(palette.getDominantColor(0), 0), palette.getDominantColor(0)});
                            if (preferences.getBoolean("animations", true)) {
                                gradient.setBackground(gradientDrawable);
                                gradient.startAnimation(animFadeIn);
                                image.setImageBitmap(resource);
                                image.startAnimation(animFadeIn);
                            } else {
                                image.setImageBitmap(resource);
                                gradient.setBackground(gradientDrawable);
                            }
                            try {
                                favourite.setColorFilter(textColor);
                                title.setTextColor(textColor);
                            } catch (Exception e) {
                                Log.d(TAG, "Oopsie on " + wallpaper.getThumbnailLink());
                                favourite.setColorFilter(
                                        ColorUtils.calculateLuminance(palette.getDominantColor(0)) > 0.179 ? Color.BLACK : Color.WHITE);
                                title.setTextColor(
                                        ColorUtils.calculateLuminance(palette.getDominantColor(0)) > 0.179 ? Color.BLACK : Color.WHITE);
                            }
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            progressBar.setVisibility(View.INVISIBLE);
                            retry.setVisibility(View.VISIBLE);
                            retry.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    progressBar.setVisibility(View.VISIBLE);
                                    retry.setVisibility(View.GONE);
                                    bindData(wallpaper);
                                }
                            });
                        }
                    });

            Log.d(TAG, "bindData: Binding " + wallpaper.getThumbnailLink());
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_wallpaper, parent, false);

        return new WallpaperViewHolder(view);
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
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((WallpaperViewHolder)holder).bindData(wallpapers.get(position));
    }

    @Override
    public int getItemCount() {
        return wallpapers.size();
    }
}
