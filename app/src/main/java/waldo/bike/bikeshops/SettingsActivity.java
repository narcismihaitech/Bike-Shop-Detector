package waldo.bike.bikeshops;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;


import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import Utilities.Constants;
import Utilities.Utility;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener
        {

    private static final String LOG_TAG = SettingsActivity.class.getSimpleName();
    private static Context mContext;
    private String mOldPreferenceUnit = "";
    private String mNewPreferenceUnit = "";
    private static boolean mFirstLoad;
    String mMetricRangeValue = "";
    String mImperialRangeValue = "";
    String mMetricSpeedValue = "";
    String mImperialSpeedValue = "";
    private String mRange = "Range";
    private String mSpeed = "speed";
    private boolean mIsMetricLoaded;
    private CheckBoxPreference mNotifCheckBox;
    boolean mIsChecked;
    private boolean mFirstCheck;
    String mCheckBoxStatus = "";
    private Tracker mGaTracker;
    private String screenName = "Settings activity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //animation between activities
        overridePendingTransition(R.xml.slide_in,R.xml.slide_out);
        // Add 'general' preferences, defined in the XML file
        mContext = getApplicationContext();
        mFirstLoad = true;
        mGaTracker = ((BikeShopsDetector)  getApplication()).getTracker(
                BikeShopsDetector.TrackerName.APP_TRACKER);
        //report to GA that this screen has been opened
        mGaTracker.setScreenName(screenName);
        mGaTracker.send(new HitBuilders.AppViewBuilder().build());
        loadPreferenceScreen(mFirstLoad);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_range_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_speed_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_unit_key)));


    }


            /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();
        String range = "";
        String speed = "";
        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
                mOldPreferenceUnit = ((ListPreference) preference).getValue().toString();
                mNewPreferenceUnit = listPreference.getEntries()[prefIndex].toString();
                //if the old pref!=new pref && (new pref is Imperial or Metric)
                if (!mOldPreferenceUnit.equals(mNewPreferenceUnit) && (mNewPreferenceUnit.indexOf(getResources().getString(R.string.unit_array_metric))== 0
                        || mNewPreferenceUnit.indexOf(getResources().getString(R.string.unit_array_imperial)) ==0)) {
                        mFirstLoad = false;
                        loadPreferenceScreen(mFirstLoad);
                }

            } else { //here we control the display when the user exists the Settings screen and the returns
                // For other preferences, set the summary to the value's simple string representation.
                //stringValue represents the values that do not correspond to the selected unit. e.g if it is imperial, it holds the range/speed in km
               // Log.i(LOG_TAG,"stringValue is " + stringValue);
               // Log.i(LOG_TAG,"preference is " + preference.toString());
                if (mIsMetricLoaded) {
                    if (preference.toString().indexOf(mRange) >= 0 && stringValue != null && !stringValue.equals("")) {
                        range = Utility.formatPreferredRangeImperialToMetric(stringValue);
                        ((ListPreference) preference).setValue(range);
                        preference.setSummary(range);
                    }
                    if (preference.toString().indexOf(mSpeed) >= 0 && stringValue != null && !stringValue.equals("")) {
                        speed = Utility.formatPreferredSpeedImperialToMetric(stringValue);
                        ((ListPreference) preference).setValue(speed);
                        preference.setSummary(speed);
                    }
                }
                else {
                    if (preference.toString().indexOf(mRange) >= 0 && stringValue != null && !stringValue.equals("")) {
                        range = Utility.formatPreferredRangeMetricToImperial(stringValue);
                        ((ListPreference) preference).setValue(range);
                        preference.setSummary(range);
                    }
                    if (preference.toString().indexOf(mSpeed) >= 0 && stringValue != null && !stringValue.equals("")) {
                        speed = Utility.formatPreferredSpeedMetricToImperial(stringValue);
                        ((ListPreference) preference).setValue(speed);
                        preference.setSummary(speed);
                    }
                }

            }
        }
        else {
        }
        return true;
    }


    private void loadPreferenceScreen(boolean firstLoad) {
        String oldPreferredUnit = Utility.getPreferredUnit(getApplicationContext());
        String toUsePreferredUnit = "";
        String formattedSummaryRange = "";
        String correctSummaryRange = "";
        String correctSummarySpeed = "";
            if (!mNewPreferenceUnit.equals("")) {
                toUsePreferredUnit = mNewPreferenceUnit;
            } else {
                toUsePreferredUnit = oldPreferredUnit;
            }
            if (toUsePreferredUnit.equals(getResources().getString(R.string.unit_array_metric))) { //metric screen formatting
                if (firstLoad) {
                    addPreferencesFromResource(R.xml.pref_metric_general);
                    mIsMetricLoaded = true;
                }
                else { //Metric
                    mIsMetricLoaded = true;
                    //****Range option***
                    ListPreference rangeListPreference = (ListPreference) findPreference(getResources().getString(R.string.pref_range_key));
                    rangeListPreference.setEntries(R.array.range_values_array);
                    formattedSummaryRange = Utility.formatPreferredRangeImperialToMetric(Utility.getPreferredRangeMetric(getApplicationContext()));
                    //if the formated range is null, we assign the default value
                    correctSummaryRange = (formattedSummaryRange != null && !formattedSummaryRange.equals("")) ? formattedSummaryRange : getResources().getString(R.string.pref_range_default);
                    rangeListPreference.setSummary(correctSummaryRange);
                    //***Speed option***
                    ListPreference speedListPreference = (ListPreference) findPreference(getResources().getString(R.string.pref_speed_key));
                    speedListPreference.setEntries(R.array.speed_values_array);
                    correctSummarySpeed = Utility.formatPreferredSpeedImperialToMetric(Utility.getPreferredSpeed(getApplicationContext()));
                    speedListPreference.setSummary(correctSummarySpeed);
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    //***Unit option***
                    ListPreference unitListPreference = (ListPreference) findPreference(getResources().getString(R.string.pref_unit_key));
                    unitListPreference.setSummary(getResources().getString(R.string.unit_array_metric));
                    unitListPreference.setDefaultValue(getResources().getString(R.string.unit_array_metric));
                }

            } else { //imperial screen formatting
                if (firstLoad) {
                    addPreferencesFromResource(R.xml.pref_imperial_general);
                    mIsMetricLoaded = false;

                }
                else { //Imperial
                    mIsMetricLoaded = false;
                    //***Range option***
                    ListPreference rangeListPreference = (ListPreference) findPreference(getResources().getString(R.string.pref_range_key));
                    rangeListPreference.setEntries(R.array.range_values_imperial_array);
                    formattedSummaryRange = Utility.formatPreferredRangeMetricToImperial(Utility.getPreferredRangeImperial(getApplicationContext()));
                    //if the formated range is null, we use the default value
                    correctSummaryRange = (formattedSummaryRange != null && !formattedSummaryRange.equals("")) ? formattedSummaryRange : getResources().getString(R.string.pref_range_imperial_default);
                    rangeListPreference.setSummary(correctSummaryRange);
                    //***Speed option***
                    ListPreference speedListPreference = (ListPreference) findPreference(getResources().getString(R.string.pref_speed_key));
                    speedListPreference.setEntries(R.array.speed_values_imperial_array);
                    correctSummarySpeed = Utility.formatPreferredSpeedMetricToImperial(Utility.getPreferredSpeed(getApplicationContext()));
                    speedListPreference.setSummary(correctSummarySpeed);
                    //***Unit option***
                    ListPreference unitListPreference = (ListPreference) findPreference(getResources().getString(R.string.pref_unit_key));
                    unitListPreference.setSummary(getResources().getString(R.string.unit_array_imperial));
                    unitListPreference.setDefaultValue(getResources().getString(R.string.unit_array_imperial));
                }
            }
          mNotifCheckBox  = (CheckBoxPreference) getPreferenceManager().findPreference(getResources().getString(R.string.pref_enable_notifications_key));
          mFirstCheck = (mNotifCheckBox.isChecked());
          mIsChecked = (mNotifCheckBox.isChecked());
          mNotifCheckBox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
              @Override
              public boolean onPreferenceClick(Preference preference) {
                  mIsChecked = (mNotifCheckBox.isChecked()) ? true : false;
                  return true;
              }
          });
    }

            @Override
            protected void onStop() {
                super.onStop();
                //we check if the status of the check box has changed since the user opened the Settings Activity
                if (mIsChecked != mFirstCheck) {
                    mCheckBoxStatus = (mIsChecked) ? "checked" : "unchecked";
                    //set the event to GA
                    mGaTracker.send(new HitBuilders.EventBuilder()
                            .setCategory(getString(R.string.ga_checkbox_category_id))
                            .setAction(getString(R.string.ga_checkbox_action_id))
                            .setLabel(mCheckBoxStatus)
                            .build());
                }
                else {
                }
            }

    @Override
    public void onBackPressed() {
       super.onBackPressed();
       overridePendingTransition(R.xml.slide_in, R.xml.slide_out);
    }
    //we implement our own receiver for the push notifications
    public static class ParseCustomReceiver extends ParsePushBroadcastReceiver {
        public static boolean mIsCheckedInner;
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            mIsCheckedInner = Utility.getPreferredNotification(context);
            final String PARSE_KEY = "com.parse.Data";
            final String TITLE_KEY = "custom_title";
            final String BODY_KEY = "custom_alert";
            final String URI_KEY = "custom_uri";
            try {
                JSONObject json = new JSONObject(intent.getExtras().getString(PARSE_KEY));
                String title = json.getString(TITLE_KEY);
                String body = json.getString(BODY_KEY);
                String uri = json.getString(URI_KEY);
                if (mIsCheckedInner) {
                    Utility.sendNotification(title, body, uri, context);
                }
            }
            catch(JSONException e) {
                Log.e(LOG_TAG,e.getMessage());
            }
        }
    }
}