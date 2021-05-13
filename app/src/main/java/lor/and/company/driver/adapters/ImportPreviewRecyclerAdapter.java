package lor.and.company.driver.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.List;

import lor.and.company.driver.R;
import lor.and.company.driver.helpers.DriveHelper;

public class ImportPreviewRecyclerAdapter extends RecyclerView.Adapter {
    Context context;
    List<File> thumbnails;

    ImportPreviewRecyclerAdapter(Context context, List<File> thumbnails) {
        this.context = context;
        this.thumbnails = thumbnails;
    }

    public class PreviewViewHolder extends RecyclerView.ViewHolder {
        CardView base;
        ImageView previewImageView;
        ProgressBar progressBar;

        public PreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            base = itemView.findViewById(R.id.previewPreviewContainer);
            previewImageView = itemView.findViewById(R.id.previewImageView);
            progressBar = itemView.findViewById(R.id.progressBar5);
        }

        public void bind(File file) {
            base.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int height = base.getMeasuredHeight();

                    ViewGroup.LayoutParams layoutParams = base.getLayoutParams();

                    float ratio;

                    try {
                        if (file.getImageMediaMetadata().getRotation().equals(1) | file.getImageMediaMetadata().getRotation().equals(3)) {
                            ratio = base.getMeasuredHeight() / (float) file.getImageMediaMetadata().getWidth();
                            layoutParams.height = height;
                            layoutParams.width = (int) (file.getImageMediaMetadata().getHeight() * ratio);
                        } else {
                            ratio = base.getMeasuredHeight() / (float) file.getImageMediaMetadata().getHeight();
                            layoutParams.height = height;
                            layoutParams.width = (int) (file.getImageMediaMetadata().getWidth() * ratio);
                        }
                    } catch (Exception e) {
                        ratio = base.getMeasuredHeight() / (float) file.getImageMediaMetadata().getHeight();
                        layoutParams.height = height;
                        layoutParams.width = (int) (file.getImageMediaMetadata().getWidth() * ratio);
                    }


                    base.setLayoutParams(layoutParams);

                    base.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });

            Glide.with(context).load(Uri.parse(file.getThumbnailLink())).into(new SimpleTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    previewImageView.setImageDrawable(resource);
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PreviewViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_previewpreview, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((PreviewViewHolder)holder).bind(thumbnails.get(position));
    }

    @Override
    public int getItemCount() {
        return thumbnails.size();
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
