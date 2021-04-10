package lor.and.company.driver.helpers;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import lor.and.company.driver.AddDriveFolderActivity;
import lor.and.company.driver.R;
import lor.and.company.driver.SettingsActivity;

import static lor.and.company.driver.helpers.Constants.CODE_ADD_WALL;
import static lor.and.company.driver.helpers.Constants.CODE_SETTINGS;

public class ActivityHelper {
    public static void setupActionBar(Activity activity) {
        ImageView add = activity.findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("BUTTON", "onClick: Clicked");
                Intent intent = new Intent(activity, AddDriveFolderActivity.class);
                activity.startActivityForResult(intent, CODE_ADD_WALL);
            }
        });

        ImageView settings = activity.findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("BUTTON", "onClick: Clicked");
                Intent intent = new Intent(activity, SettingsActivity.class);
                activity.startActivityForResult(intent, CODE_SETTINGS);
            }
        });
    }
}
