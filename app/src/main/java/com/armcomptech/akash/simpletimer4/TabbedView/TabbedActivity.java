package com.armcomptech.akash.simpletimer4.TabbedView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.armcomptech.akash.simpletimer4.R;
import com.armcomptech.akash.simpletimer4.Settings.SettingsActivity;
import com.armcomptech.akash.simpletimer4.multiTimer.MultiTimerActivity;
import com.armcomptech.akash.simpletimer4.singleTimer.timerWithService;
import com.armcomptech.akash.simpletimer4.statistics.StatisticsActivity;
import com.armcomptech.akash.simpletimer4.stopwatch.stopwatchWithService;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

public class TabbedActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler{

    //TODO: Change disableFirebaseLogging to false when releasing
    public static Boolean disableFirebaseLogging = true;
    private static FirebaseAnalytics mFirebaseAnalytics;

    BillingProcessor bp;
    String activityToOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!disableFirebaseLogging) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "App Opened");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbled);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        notificationManager = NotificationManagerCompat.from(this);
        setTitle("   Timer and Stopwatch");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_timer_white);
        }

        bp = new BillingProcessor(this, getString(R.string.licence_key), this);
        bp.initialize();

        removeAds(); // this removes all ads for new users

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        boolean overrideActivityToOpen = getIntent().getBooleanExtra("overrideActivityToOpen", false);

        if (!overrideActivityToOpen) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            activityToOpen = sharedPreferences.getString("firstOpenActivity", "Timer and Stopwatch");

            switch (activityToOpen) {
                case "Timer and Stopwatch":
                    //do nothing
                    break;
                case "Multi Timer":
                    Intent intent = new Intent(this , MultiTimerActivity.class);
                    intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    break;
                case "Statistics":
                    startActivity(new Intent(this, StatisticsActivity.class));
                    break;
                default:
                    startActivity(new Intent(this, SettingsActivity.class));
                    break;
            }
        }


        if (disableFirebaseLogging) {
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false);
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
        } else {
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        }
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        if (productId.equals("remove_ads")) {
            removeAds();

            Toast.makeText(this, "Removed Ads", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeAds() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("removed_Ads", true);
        editor.apply();
    }

    @Override
    public void onPurchaseHistoryRestored() {
        bp.loadOwnedPurchasesFromGoogle();
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Log.d("Billing", "Something went wrong in billing. Errorcode : " + errorCode);
//        Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBillingInitialized() {

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);

        menu.findItem(R.id.check_sound).setChecked(getSharedPreferences("shared preferences", MODE_PRIVATE).getBoolean("SOUND_CHECKED", true));
        menu.findItem(R.id.timer_and_stopwatch).setVisible(false);
//        menu.add(0, R.id.multi_Timer_Mode, 1, menuIconWithText(getResources().getDrawable(R.drawable.ic_video_library_black), "Multi Timer Mode"));
//        menu.add(0, R.id.statistics_activity, 2, menuIconWithText(getResources().getDrawable(R.drawable.ic_data_usage_black), "Statistics"));
//        menu.add(0, R.id.setting_activity, 3, menuIconWithText(getResources().getDrawable(R.drawable.ic_settings_black), "Settings"));

        if (!isRemovedAds()) {
            menu.add(0, R.id.remove_Ads, 4, menuIconWithText(getResources().getDrawable(R.drawable.ic_baseline_remove_circle_outline_black), "Remove Ads"));
        }

        return true;
    }

    private CharSequence menuIconWithText(Drawable r, String title) {

        r.setBounds(0, 0, r.getIntrinsicWidth(), r.getIntrinsicHeight());
        SpannableString sb = new SpannableString("    " + title);
        ImageSpan imageSpan = new ImageSpan(r, ImageSpan.ALIGN_BOTTOM);
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sb;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        item.setChecked(!item.isChecked());

        switch (id) {
            case R.id.check_sound:
                SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
                boolean previousValue = sharedPreferences.getBoolean("SOUND_CHECKED", true);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("SOUND_CHECKED", !previousValue);
                editor.apply();
                logFirebaseAnalyticsEvents("Sound Checked");
                break;

            case R.id.statistics_activity:
                logFirebaseAnalyticsEvents("Opened Statistics");
                startActivity(new Intent(this, StatisticsActivity.class));
                break;

            case R.id.multi_Timer_Mode:
                //destroy services
                stopService(new Intent(this, timerWithService.class));
                stopService(new Intent(this, stopwatchWithService.class));

                Intent intent = new Intent(this, MultiTimerActivity.class);
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;

            case R.id.setting_activity:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.remove_Ads:
                bp.purchase(this, "remove_ads");

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isRemovedAds() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        return sharedPreferences.getBoolean("removed_Ads", false);
    }

    public static void logFirebaseAnalyticsEvents(String eventName) {
        if (!disableFirebaseLogging) {
            Bundle bundle = new Bundle();
            bundle.putString("Event", eventName);
            if (mFirebaseAnalytics != null) {
                mFirebaseAnalytics.logEvent(eventName.replace(" ", "_"), bundle);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}