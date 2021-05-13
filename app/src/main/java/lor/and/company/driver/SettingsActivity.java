package lor.and.company.driver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
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
import java.util.List;
import java.util.StringJoiner;

import lor.and.company.driver.helpers.DBHelper;
import lor.and.company.driver.models.Collection;

public class SettingsActivity extends AppCompatActivity {

    Context context;

    LinearLayout signedOutView, signedInView;
    TextView nameView, emailView;

    Button signInOut, importDrive, exportDrive, buyAds, restorePurchases;
    Switch animations;

    SharedPreferences preferences;

    LinearLayout adLoadingView;
    LinearLayout adBuyView;
    LinearLayout adErrorView;

    public static final int CREATE_BACKUP = 1;
    public static final int RESTORE_BACKUP = 2;

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

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        signInOut = findViewById(R.id.signInOut);
        importDrive = findViewById(R.id.importDrive);
        exportDrive = findViewById(R.id.exportDrive);
        animations = findViewById(R.id.animationToggle);
        buyAds = findViewById(R.id.buyAds);
        restorePurchases = findViewById(R.id.restorePurchases);

        adLoadingView = findViewById(R.id.adLoadingContainer);
        adBuyView = findViewById(R.id.adBuyRestoreContainer);
        adErrorView = findViewById(R.id.adFailedContainer);

        checkGoogleAccount();
        connectToBilling();

        buyAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Clicked Remove Ads");
                List<String> skuList = new ArrayList<>();
                skuList.add("driver_remove_ads");
                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
                billingClient.querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(BillingResult billingResult,
                                                             List<SkuDetails> skuDetailsList) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    Log.d(TAG, "onSkuDetailsResponse: DOes it even go here?");
                                    for (SkuDetails skuDetails: skuDetailsList) {
                                        if (skuDetails.getSku().equals("driver_remove_ads")) {
                                            Log.d(TAG, "onSkuDetailsResponse: It does!");
                                            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                                    .setSkuDetails(skuDetails)
                                                    .build();
                                            int responseCode = billingClient.launchBillingFlow((Activity) context, billingFlowParams).getResponseCode();
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to open in-app billing. Check your internet connection.", Toast.LENGTH_SHORT).show();
                                }
                                Log.d(TAG, "onSkuDetailsResponse: " + billingResult.getResponseCode());
                            }
                        });
            }
        });

        restorePurchases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                if (purchasesResult.getPurchasesList().size() == 0) {
                    Toast.makeText(context, "A valid purchase has not been found.", Toast.LENGTH_SHORT).show();
                }
                handlePurchase(purchasesResult.getPurchasesList());
            }
        });

        animations.setChecked(preferences.getBoolean("animations", true));

        animations.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean("animations", isChecked).apply();
            }
        });

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

        CardView compact = findViewById(R.id.compact);
        RadioButton compactButton = findViewById(R.id.compactRadio);
        CardView immersive = findViewById(R.id.immersive);
        RadioButton immersiveButton = findViewById(R.id.immersiveRadio);

        View.OnClickListener compactListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                preferences.edit().putString("layout", "compact").apply();
                compactButton.setChecked(true);
                immersiveButton.setChecked(false);
                setResult(RESULT_OK);
            }
        };

        View.OnClickListener immersiveListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                preferences.edit().putString("layout", "immersive").apply();
                compactButton.setChecked(false);
                immersiveButton.setChecked(true);
                setResult(RESULT_OK);
            }
        };

        compact.setOnClickListener(compactListener);
        compactButton.setOnClickListener(compactListener);
        immersive.setOnClickListener(immersiveListener);
        immersiveButton.setOnClickListener(immersiveListener);

        compactButton.setChecked(!preferences.getString("layout", "compact").equals("immersive"));
        immersiveButton.setChecked(preferences.getString("layout", "compact").equals("immersive"));
    }

    private final PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                handlePurchase(purchases);
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                Toast.makeText(context, "Cancelled purchase.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void handlePurchase(List<Purchase> purchases) {
        if (purchases.size() == 0) {
        } else {
            boolean seen = false;
            for (Purchase purchase: purchases) {
                if (purchase.getSku().equals("driver_remove_ads")) {
                    preferences.edit().putBoolean("adfree", true).apply();
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                Toast.makeText(context, "A valid purchase has not been found.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Successfully removed ads! Thank you for your support!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private BillingClient billingClient;

    private void connectToBilling() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "onBillingSetupFinished: Connected to Google Play");
                    adLoadingView.setVisibility(View.GONE);
                    if (!preferences.getBoolean("adfree", false)) {
                        checkPurchases();
                    }
                    adBuyView.setVisibility(View.VISIBLE);
                    adErrorView.setVisibility(View.GONE);
                } else {
                    adBuyView.setVisibility(View.GONE);
                    adErrorView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                adLoadingView.setVisibility(View.GONE);
                adBuyView.setVisibility(View.GONE);
                adErrorView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkPurchases() {
        List<Purchase> purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
        handlePurchase(purchases);
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
                StringJoiner row = new StringJoiner(":::::");
                row.add(collection.getId());
                row.add(collection.getName());
                row.add(collection.getOwner());
                Log.d(TAG, "createBackup: " + row.toString());
                stringJoiner.add(row.toString());
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
                preferences.edit().putString("email", googleSignInAccount.getEmail())
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