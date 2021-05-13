package lor.and.company.driver.helpers;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

@Keep
public class DriveHelper {
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

    public static String getError(String reason) {
        if (reason.contains("authError")) {
            reason = "You removed Driver's permission to access your Google Drive.";
        } else if (reason.contains("usageLimits")) {
            reason = "You're using this app TOO MUCH. Try again tomorrow.";
        } else if (reason.contains("rateLimitExceeded") && reason.contains("403")) {
            reason = "You are using this app too much! Calm down and try again tomorrow.";
        } else if (reason.contains("insufficientFilePermissions")) {
            reason = "You don't have permission to view the folder. Ask the folder owner to add your email to the list of shared people.";
        } else if (reason.contains("notFound")) {
            reason = "This Driver folder doesn't exist or you don't have permission to view it.";
        } else if (reason.contains("rateLimitExceeded") && reason.contains("429")) {
            reason = "You are using this app too much in such a short amount of time! Calm down and try again later.";
        } else if (reason.contains("backendError") || reason.contains("Bad Gateway") || reason.contains("Service Unavailable") || reason.contains("Gateway Timeout")) {
            reason = "Google Drive had a meltdown. Try again.";
        } else if (reason.contains("Unable to resolve")) {
            reason = "You're not connected to the internet. Please check your internet connection.";
        } else if (reason.contains("badRequest") || reason.contains("Bad Request")) {
            reason = "Bad request";
        } else if (reason.contains("Invalid Credentials")) {
            reason = "The app lost access to your Google Drive account. Try logging out and logging in again.";
        } else if (reason.contains("dailyLimitExceeded ")) {
            reason = "Driver is getting a lot of traffic and has reached its daily quota. Try again tomorrow.";
        } else if (reason.contains("userRateLimitExceeded")) {
            reason = "You are using this app too much in such a short amount of time! Calm down and try again later.";
        } else if (reason.contains("appNotAuthorizedToFile")) {
            reason = "Driver is not allowed to open this folder.";
        } else if (reason.contains("disabled Drive apps")){
            reason = "Your admin disabled Drive apps.";
        }
        return reason;
    }

    public static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        // https://drive.google.com/folderview?id=1Zd5Ow8zgVvnKoNCPwJ0lzJIQp0X4-DvR
        sURIMatcher.addURI("drive.google.com", "folderview", 1);
        // https://drive.google.com/drive/u/1/folders/1Zd5Ow8zgVvnKoNCPwJ0lzJIQp0X4-DvR
        sURIMatcher.addURI("drive.google.com", "drive/u/#/folders/*", 2);
        // https://drive.google.com/drive/folders/1Zd5Ow8zgVvnKoNCPwJ0lzJIQp0X4-DvR?usp=sharing
        sURIMatcher.addURI("drive.google.com", "drive/folders/*", 2);
        // https://drive.google.com/drive/u/1/folders/1Zd5Ow8zgVvnKoNCPwJ0lzJIQp0X4-DvR
        sURIMatcher.addURI("drive.google.com", "drive/u/#/mobile/folders/*", 2);
        //https://drive.google.com/drive/mobile/folders/1bYKLLIj--QpXeWrKAkwWcwKLwvDZqSdz?usp=sharing
        sURIMatcher.addURI("drive.google.com", "drive/mobile/folders/*", 2);
    }

    public static String getId(String url) {
        switch (sURIMatcher.match(Uri.parse(url))) {
            case 1: {
                return Uri.parse(url).getQueryParameter("id");
            }
            case 2: {
                return Uri.parse(url).getLastPathSegment();
            }
            default: {
                return url;
            }
        }
    }
}
