package lor.and.company.driver.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lor.and.company.driver.AddDriveFolderActivity;
import lor.and.company.driver.ImportActivity;
import lor.and.company.driver.R;
import lor.and.company.driver.helpers.DriveHelper;

public class ImportRecyclerAdapter extends RecyclerView.Adapter {

    Context context;
    HashMap<Integer, ImportActivity.ImportObject> ids;
    ImportOptionsListener listener;

    Integer[] indexes;

    List<Integer> keys;

    private static final String TAG = "ImportRecyclerAdapter";

    public ImportRecyclerAdapter(Context context, HashMap<Integer, ImportActivity.ImportObject> ids, ImportOptionsListener importOptionsListener) {
        this.context = context;
        this.ids = ids;
        this.listener = importOptionsListener;

        keys = new ArrayList<>(ids.keySet());
    }

    public class ImportViewHolder extends RecyclerView.ViewHolder {
        RecyclerView previewPreviewRecyclerView;
        TextView title, author, wallcount;
        CheckBox addImport;
        LinearLayout errorView, loadingView;
        ConstraintLayout container;

        public ImportViewHolder(@NonNull View itemView) {
            super(itemView);
            previewPreviewRecyclerView = itemView.findViewById(R.id.previewPreviewRecyclerView);
            title = itemView.findViewById(R.id.preview_title);
            author = itemView.findViewById(R.id. preview_author);
            wallcount = itemView.findViewById(R.id.wallCount);
            addImport = itemView.findViewById(R.id.addImport);
            errorView = itemView.findViewById(R.id.errorContainer);
            loadingView = itemView.findViewById(R.id.container);
            container = itemView.findViewById(R.id.previewContainer);
        }

        public void bind(ImportActivity.ImportObject folder, int index) {
            if (!folder.ifError()) {
                container.setVisibility(View.VISIBLE);
                title.setText(folder.getFolderDetails().getName());
                author.setText("by " + folder.getFolderDetails().getOwners().get(0).getDisplayName());
                addImport.setChecked(true);
                ImportPreviewRecyclerAdapter adapter = new ImportPreviewRecyclerAdapter(context, folder.getFiles());
                adapter.setHasStableIds(true);
                previewPreviewRecyclerView.setAdapter(adapter);
                previewPreviewRecyclerView.setLayoutManager(new AddDriveFolderActivity.ImmovableLayoutManager(2, StaggeredGridLayoutManager.HORIZONTAL));

                addImport.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        listener.onWillImportChange(index, b);
                    }
                });
            } else {
                errorView.setVisibility(View.VISIBLE);
                TextView nameAuthor = (TextView) itemView.findViewById(R.id.nameAuthor);
                nameAuthor.setText(folder.getName() + " by " + folder.getAuthor()  + " failed to load.");
                errorView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://drive.google.com/folderview?id="+folder.getFolderId()));
                        context.startActivity(intent);
                    }
                });
            }
            loadingView.setVisibility(View.GONE);
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

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ImportViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_import, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ImportViewHolder)holder).bind(ids.get(keys.get(position)), position);
    }

    @Override
    public int getItemCount() {
        return keys.size();
    }
}


