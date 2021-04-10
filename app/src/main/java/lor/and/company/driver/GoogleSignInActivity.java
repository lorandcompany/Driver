package lor.and.company.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.DriveScopes;

import static android.Manifest.permission.READ_CONTACTS;

public class GoogleSignInActivity extends AppCompatActivity {

    private static final String TAG = "GoogleSignInActivity";

    Context context;

    public static final int REQUEST_CODE_SIGN_IN = 100;
    public static final int REQUEST_CODE_SIGN_IN_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_sign_in);

        Button login = findViewById(R.id.login);
        Button finish = findViewById(R.id.finish);

        context = this;

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAndRemoveTask();
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn(view.getContext());
            }
        });
    }

    public static boolean ifSignedIn(Context context) {
        return GoogleSignIn.getLastSignedInAccount(context) != null;
    }

    public static void signIn(Context context) {
        Log.d(TAG, "signIn: " + Boolean.toString(context.checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED));
        if (context.checkSelfPermission(READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{READ_CONTACTS}, REQUEST_CODE_SIGN_IN_PERMISSION);
        } else {
            GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestScopes(new Scope(DriveScopes.DRIVE_READONLY))
                    .build();

            GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);

            ((Activity)context).startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }

    public void afterLogin(Intent data, Context context) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Signed in as " + googleSignInAccount.getEmail());
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                        sharedPreferences.edit().putString("email", googleSignInAccount.getEmail())
                                .putString("token", googleSignInAccount.getServerAuthCode())
                                .putString("userID", googleSignInAccount.getId()).apply();
                        data.getClass();
                        Intent intent = new Intent(context, CollectionActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SIGN_IN) {
            afterLogin(data ,context);
        } else {
            Toast.makeText(context, "Failed to log in.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_SIGN_IN_PERMISSION) {
            Log.d(TAG, "onRequestPermissionsResult: " +permissions[0]);
            Log.d(TAG, "onRequestPermissionsResult: " +grantResults[0]);
            if (permissions[0].equals(READ_CONTACTS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_READONLY))
                        .build();

                GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);

                ((Activity)context).startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
            } else {
                if (((Activity)context).shouldShowRequestPermissionRationale(READ_CONTACTS)) {
                    Toast.makeText(context, "Getting your Google Account info requires this permission to work. Please allow the app to have access to your contacts.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}