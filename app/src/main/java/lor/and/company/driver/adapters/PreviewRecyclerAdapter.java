package lor.and.company.driver.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.api.services.drive.model.File;

import java.util.List;

import lor.and.company.driver.R;

public class PreviewRecyclerAdapter extends RecyclerView.Adapter {

    List<File> files;
    Context context;

    public PreviewRecyclerAdapter(Context context, List<File> files) {
        this.context = context;
        this.files = files;
    }

    private static final String TAG = "PreviewRecyclerAdapter";

    public class WallpaperViewHolder extends ViewHolder {
        ConstraintLayout base;
        ImageView image;
        ProgressBar progressBar;
        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.heroImage);
            progressBar = itemView.findViewById(R.id.progressBar);
            base = itemView.findViewById(R.id.base);
        }

        public void bind (File file) {

            Log.d(TAG, "bind: " + file);

            base.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    ViewGroup.LayoutParams layoutParams = base.getLayoutParams();

                    Log.d(TAG, "bind: " + file.getName());

                    float ratio;
                    try {
                        if (file.getImageMediaMetadata().getRotation() == 1 | file.getImageMediaMetadata().getRotation() ==3 ) {
                            ratio = (float) image.getMeasuredWidth() / (float) file.getImageMediaMetadata().getHeight();
                            layoutParams.width = image.getMeasuredWidth();
                            layoutParams.height = (int) (file.getImageMediaMetadata().getWidth() * ratio);
                        } else {
                            ratio = image.getMeasuredWidth() / (float) file.getImageMediaMetadata().getWidth();
                            layoutParams.width = image.getMeasuredWidth();
                            layoutParams.height = (int) (file.getImageMediaMetadata().getHeight() * ratio);
                        }
                    } catch (NullPointerException e) {
                        ratio = image.getMeasuredWidth() / (float) file.getImageMediaMetadata().getWidth();
                        layoutParams.width = image.getMeasuredWidth();
                        layoutParams.height = (int) (file.getImageMediaMetadata().getHeight() * ratio);
                    }


                    base.setLayoutParams(layoutParams);

                    base.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });

            Glide.with(context).load(Uri.parse(file.getThumbnailLink()))
                    .into(new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            image.setBackground(resource);
                            image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_preview, parent, false);
        return new WallpaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((WallpaperViewHolder) holder).bind(files.get(position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
