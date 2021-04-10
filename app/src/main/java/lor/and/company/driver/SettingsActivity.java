package lor.and.company.driver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringJoiner;

import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.models.Collection;

public class SettingsActivity extends AppCompatActivity {

    Context context;

    LinearLayout signedOutView, signedInView;
    TextView nameView, emailView;

    Button signInOut, importDrive, exportDrive, cache, enableAnimations, disableAnimations;

    SharedPreferences prefs;

    int CREATE_BACKUP = 1;
    int RESTORE_BACKUP = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        context = this;

        signedInView = findViewById(R.id.signedInView);
        signedOutView = findViewById(R.id.signedOutView);

        emailView = findViewById(R.id.emailView);
        nameView = findViewById(R.id.nameView);

        ImageView back = findViewById(R.id.wallpaperBack);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        signInOut = findViewById(R.id.signInOut);
        importDrive = findViewById(R.id.importDrive);
        exportDrive = findViewById(R.id.exportDrive);
        enableAnimations = findViewById(R.id.enableAnimations);
        disableAnimations = findViewById(R.id.disableAnimations);
        cache = findViewById(R.id.cache);

        checkGoogleAccount();

        Context context;

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        enableAnimations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.edit().putBoolean("animations", true).apply();
                checkAnimations();
            }
        });

        disableAnimations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.edit().putBoolean("animations", false).apply();
                checkAnimations();
            }
        });

        checkAnimations();

        exportDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Choose a directory using the system's file picker.
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/*");
                intent.putExtra(Intent.EXTRA_TITLE, "driver-backup-"+ Calendar.getInstance().getTime()+".backup");
                startActivityForResult(intent, CREATE_BACKUP);
            }
        });

        importDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/*");
                startActivityForResult(intent, RESTORE_BACKUP);
            }
        });
    }

    public void checkAnimations(){
        if (prefs.getBoolean("animations", true)) {
            enableAnimations.setVisibility(View.GONE);
            disableAnimations.setVisibility(View.VISIBLE);
        } else {
            enableAnimations.setVisibility(View.VISIBLE);
            disableAnimations.setVisibility(View.GONE);
        }
    }

    public void checkGoogleAccount(){
        GoogleSignInAccount account;

        if (GoogleSignInActivity.ifSignedIn(context)) {
            signedOutView.setVisibility(View.GONE);
            signedInView.setVisibility(View.VISIBLE);

            account = GoogleSignIn.getLastSignedInAccount(context);

            emailView.setText(account.getEmail());
            nameView.setText(account.getDisplayName());

            signInOut.setText("SIGN OUT");
            signInOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(new Scope(DriveScopes.DRIVE_READONLY))
                            .build();

                    GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);

                    client.signOut();
                    checkGoogleAccount();
                }
            });
        } else {
            signedInView.setVisibility(View.GONE);
            signedOutView.setVisibility(View.VISIBLE);

            signInOut.setText("SIGN IN");
            signInOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    GoogleSignInActivity.signIn(context);
                }
            });
        }
    }

    private static final String TAG = "SettingsActivity";

    public void createBackup(Uri data) {
        DBHelper.CollectionsDB db = new DBHelper.CollectionsDB(context);
        db.getCollections();
        Log.d(TAG, "createBackup: " + data.getPath());
        ContentResolver contentResolver = context.getContentResolver();
        try {
            OutputStream outputStream =  contentResolver.openOutputStream(data);
            StringJoiner stringJoiner = new StringJoiner("\r\n");
            ArrayList<Collection> collections = db.getCollections();
            for (Collection collection: collections) {
                stringJoiner.add(collection.getId());
            }
            outputStream.write(stringJoiner.toString().getBytes());
            outputStream.flush();
            outputStream.close();
            Toast.makeText(context, "Successfully exported folders", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GoogleSignInActivity.REQUEST_CODE_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(context);

                Log.d(TAG, "Signed in as " + googleSignInAccount.getEmail());
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                sharedPreferences.edit().putString("email", googleSignInAccount.getEmail())
                        .putString("token", googleSignInAccount.getServerAuthCode())
                        .putString("userID", googleSignInAccount.getId()).apply();
            } else {
                Toast.makeText(context, "Failed to log in.", Toast.LENGTH_SHORT).show();
            }
            checkGoogleAccount();
            setResult(RESULT_OK);
        } else if (requestCode == CREATE_BACKUP){
            if (resultCode == RESULT_OK) {
                createBackup(data.getData());
            } else {
                Toast.makeText(context, "Cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == RESTORE_BACKUP){
            Log.d(TAG, "onActivityResult: Restoring backup");
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(context, ImportActivity.class);
                intent.setData(data.getData());
                startActivity(intent);
                setResult(RESULT_OK);
            } else {
                Toast.makeText(context, "Cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}