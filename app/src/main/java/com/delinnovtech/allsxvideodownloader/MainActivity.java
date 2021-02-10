package com.delinnovtech.allsxvideodownloader;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.delinnovtech.allsxvideodownloader.browsing_feature.FacebookFragment;
import com.delinnovtech.allsxvideodownloader.history_feature.History;
import com.delinnovtech.allsxvideodownloader.whatsapp_feature.Whatsapp;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.delinnovtech.allsxvideodownloader.browsing_feature.BrowserManager;
import com.delinnovtech.allsxvideodownloader.download_feature.fragments.Downloads;
import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.ads.InterstitialAd;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener {

    private static final String TAG = "VD_Debug";
    private EditText searchTextBar;
    private ImageView btn_search_cancel;
    private ImageView btn_search;
    private BrowserManager browserManager;
    private Uri appLinkData;
    private FragmentManager manager;
    public BottomNavigationView navView;
    private RelativeLayout toolbar;
    private int click = 0;
    private int adshow = 3;
    private LinearLayout btnSetting2;
    DrawerLayout drawer;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private RecyclerView videoSites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.intersitial_ad));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });
        final SharedPreferences prefs = getSharedPreferences("settings", 0);
        boolean adBlockOn = prefs.getBoolean(getString(R.string.adBlockON), false);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view2);
        Menu menu = navigationView.getMenu();

        final SwitchCompat switchcompat = (SwitchCompat) MenuItemCompat.getActionView(menu.findItem(R.id.menu_addBlocker)).findViewById(R.id.switch_id);
        switchcompat.setChecked(adBlockOn);

        switchcompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (switchcompat.isChecked()) {
                    prefs.edit().putBoolean(getString(R.string.adBlockON), isChecked).apply();

                }
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.menu_ratUs) {
                    rateApp();
                } else if (id == R.id.menu_shareUs) {
                    shareApp();
                } else if (id == R.id.menu_Privacy) {
                    privacyPolicyClicked();
                }
                return true;
            }
        });

        btnSetting2 = findViewById(R.id.btn_settings2);
        btnSetting2.setOnClickListener(this);
        toolbar = findViewById(R.id.toolbar);
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        //String appLinkAction = appLinkIntent.getAction();
        appLinkData = appLinkIntent.getData();

        manager = this.getSupportFragmentManager();
        // This is for creating browser manager fragment
        if ((browserManager = (BrowserManager) this.getSupportFragmentManager().findFragmentByTag("BM")) == null) {
            manager.beginTransaction()
                    .add(browserManager = new BrowserManager(), "BM").commit();
        }

        // Bottom navigation
        navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navView.setItemIconTintList(null);


        setUPBrowserToolbarView();
        setUpVideoSites();

    }

    private void rateApp() {
        try {
            Intent rateIntent = rateIntentForUrl("market://details");
            startActivity(rateIntent);
        } catch (ActivityNotFoundException e) {
            Intent rateIntent = rateIntentForUrl("https://play.google.com/store/apps/details");
            startActivity(rateIntent);
        }
    }

    private Intent rateIntentForUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, getPackageName())));
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        if (Build.VERSION.SDK_INT >= 21)
            flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        else
            flags |= Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        intent.addFlags(flags);
        return intent;
    }

    public void shareApp() {
        StringBuilder msg = new StringBuilder();
        msg.append(getString(R.string.share_msg));
        msg.append("\n");
        msg.append("https://play.google.com/store/apps/details?id=com.encoderbytes.downloader");
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, msg.toString());
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private void privacyPolicyClicked() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_link)));
        startActivity(browserIntent);
    }


    private void setUPBrowserToolbarView() {

        // Toolbar search
        btn_search_cancel = findViewById(R.id.btn_search_cancel);
        btn_search_cancel.setOnClickListener(this);

        btn_search = findViewById(R.id.btn_search);
        searchTextBar = findViewById(R.id.et_search_bar);

        /*hide/show clear button in search view*/
        TextWatcher searchViewTextWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    btn_search_cancel.setVisibility(View.GONE);
                } else {
                    btn_search_cancel.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        searchTextBar.addTextChangedListener(searchViewTextWatcher);
        searchTextBar.setOnEditorActionListener(this);
        btn_search.setOnClickListener(this);

        //   Toolbar home button
        ImageView toolbar_home = findViewById(R.id.btn_home);
        toolbar_home.setOnClickListener(this);

        //Settings
        ImageView settings = findViewById(R.id.btn_settings);
        settings.setOnClickListener(this);
    }

    private void setUpVideoSites() {
        // Video sites link
        videoSites = findViewById(R.id.rvVideoSitesList);
        videoSites.setLayoutManager(new GridLayoutManager(MainActivity.this, 3));
        videoSites.setAdapter(new VideoSitesList(this));
    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {

                case R.id.navigation_home:
                    click++;
                    if (click == adshow && mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                        click = 0;
                    } else {
                        showToolbar();
                        searchTextBar.getText().clear();
                        //   getBrowserManager().closeAllWindow();
                        homeClicked();
                    }


                    return true;

                case R.id.navigation_downloads:
                    click++;
                    if (click == adshow && mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                        click = 0;
                    } else {
                        downloadClicked();
                    }


                    return true;
                case R.id.navigation_history:
                    click++;
                    if (click == adshow && mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                        click = 0;
                    } else {
                        historyClicked();
                    }


                    return true;
            }
            return false;
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_search_cancel:
                searchTextBar.getText().clear();
                break;
            case R.id.btn_home:
                showToolbar();
                searchTextBar.getText().clear();
                getBrowserManager().closeAllWindow();
                break;
            case R.id.btn_settings2:
                drawer.open();
                break;
            case R.id.btn_search:
                new WebConnect(searchTextBar, this).connect();
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_GO) {
            new WebConnect(searchTextBar, this).connect();
        }
        return handled;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isOpen()) {
            drawer.close();
        } else if (manager.findFragmentByTag("Downloads") != null ||
                manager.findFragmentByTag("History") != null ||
                manager.findFragmentByTag("Whatsapp") != null) {
            VDApp.getInstance().getOnBackPressedListener().onBackpressed();
            browserManager.resumeCurrentWindow();
            navView.setSelectedItemId(R.id.navigation_home);
        } else if (manager.findFragmentByTag("Settings") != null) {
            VDApp.getInstance().getOnBackPressedListener().onBackpressed();
            browserManager.resumeCurrentWindow();
            navView.setVisibility(View.VISIBLE);
        } else if (VDApp.getInstance().getOnBackPressedListener() != null) {
            VDApp.getInstance().getOnBackPressedListener().onBackpressed();
        } else {

            MainActivity.super.onBackPressed();
//            new AlertDialog.Builder(this)
//                    .setMessage("Are you sure you want to exit?")
//                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            MainActivity.super.onBackPressed();
//                        }
//                    })
//                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//
//                        }
//                    })
//                    .create()
//                    .show();
        }
    }

    public BrowserManager getBrowserManager() {
        return browserManager;
    }


    public interface OnBackPressedListener {
        void onBackpressed();
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        VDApp.getInstance().setOnBackPressedListener(onBackPressedListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (appLinkData != null) {
            browserManager.newWindow(appLinkData.toString());
        }
        browserManager.updateAdFilters();
    }

    public void browserClicked() {
        browserManager.unhideCurrentWindow();
    }

    private void downloadClicked() {
        closeHistory();
        closeWhatsapp();
        if (manager.findFragmentByTag("Downloads") == null) {
            browserManager.hideCurrentWindow();
            browserManager.pauseCurrentWindow();
            manager.beginTransaction().add(R.id.main_content, new Downloads(), "Downloads").commit();
        }
    }

    public void whatsappClicked() {
        click++;
        if (click == adshow && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
            click = 0;
        } else {
            closeDownloads();
            closeHistory();

            if (manager.findFragmentByTag("Whatsapp") == null) {
                browserManager.hideCurrentWindow();
                browserManager.pauseCurrentWindow();
                manager.beginTransaction().add(R.id.main_content, new Whatsapp(), "Whatsapp").commit();
            }

        }

    }


    private void historyClicked() {
        closeDownloads();
        closeWhatsapp();
        if (manager.findFragmentByTag("History") == null) {
            browserManager.pauseCurrentWindow();
            manager.beginTransaction().add(R.id.main_content, new History(), "History").commit();

        }
    }


    public void homeClicked() {

        browserManager.unhideCurrentWindow();
        browserManager.resumeCurrentWindow();
        closeDownloads();
        closeHistory();
        closeWhatsapp();
    }

    private void closeDownloads() {
        Fragment fragment = manager.findFragmentByTag("Downloads");
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commit();
        }
    }

    private void closeHistory() {
        Fragment fragment = manager.findFragmentByTag("History");
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commit();
        }
    }

    private void closeWhatsapp() {
        Fragment fragment = manager.findFragmentByTag("Whatsapp");
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        onRequestPermissionsResultCallback.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
    }

    private ActivityCompat.OnRequestPermissionsResultCallback onRequestPermissionsResultCallback;

    public void setOnRequestPermissionsResultListener(ActivityCompat
                                                              .OnRequestPermissionsResultCallback
                                                              onRequestPermissionsResultCallback) {
        this.onRequestPermissionsResultCallback = onRequestPermissionsResultCallback;
    }

    public void hideToolbar() {
        toolbar.setVisibility(View.GONE);
    }

    public void showToolbar() {
        toolbar.setVisibility(View.VISIBLE);
    }


}
