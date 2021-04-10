package lor.and.company.driver.helpers;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

public class DriveHelper {
    DriveHelper() {

    }

    private static final String TAG = "DriveHelper";
    public static Drive getDrive(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String email = sharedPreferences.getString("email", null);

        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_READONLY));

        Log.d(TAG, "getDrive: " + email);

//        credential.setSelectedAccount(credential.getSelectedAccount());
        credential.setSelectedAccountName(email);
        return new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new JacksonFactory(),
                        credential)
                        .setApplicationName("Driver")
                        .build();
    }
}
