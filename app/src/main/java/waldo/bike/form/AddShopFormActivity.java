package waldo.bike.form;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.concurrent.ExecutionException;

import Utilities.Constants;
import waldo.bike.bikeshops.BikeShopsDetector;
import waldo.bike.bikeshops.MainActivity;
import waldo.bike.bikeshops.R;

public class AddShopFormActivity extends Activity {

    public static final String LOG_TAG = AddShopFormActivity.class.getSimpleName();
    private static boolean mShopNameOk;
    private static boolean mShopWebsiteOk;
    private static boolean mShopPhoneNumberOk;
    private static final String OK_STATUS = "ok";
    private static final String ERROR_STATUS = "error";
    private EditText mShopName;
    private EditText mShopWebsite;
    private EditText mShopPhoneNumber;
    private TextView mInfoMessage;
    private TextView mShopNameTitle;
    private TextView mShopPhoneTitle;
    private TextView mShopWebsiteTitle;
    private Button mAddShopButton;
    //the Google Analytics tracker
    Tracker mGaTracker;
    private String screenName = "AddShopForm Activity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_shop_form);
        overridePendingTransition(R.xml.slide_in, R.xml.slide_out);
        mShopNameTitle = (TextView) findViewById(R.id.new_shop_name_title);
        mShopName = (EditText) findViewById(R.id.new_shop_name);
        mShopWebsiteTitle = (TextView) findViewById(R.id.new_shop_website_title);
        mShopWebsite = (EditText) findViewById(R.id.new_shop_website);
        mShopPhoneTitle = (TextView) findViewById(R.id.new_shop_phone_title);
        mShopPhoneNumber = (EditText) findViewById(R.id.new_shop_phone);
        mInfoMessage = (TextView) findViewById(R.id.add_shop_status);
        mAddShopButton = (Button) findViewById(R.id.add_shop_button);
        mShopNameOk = false;
        mShopWebsiteOk = true;
        mShopPhoneNumberOk = true;
        mGaTracker = ((BikeShopsDetector) getApplication()).getTracker(
                BikeShopsDetector.TrackerName.APP_TRACKER);
        //report to GA that the screen has been opened
        mGaTracker.setScreenName(screenName);
        mGaTracker.send(new HitBuilders.AppViewBuilder().build());
        mShopName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
               if (!hasFocus) {
                   checkShopName();
               }
            }
        });
        mShopWebsite.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkShopWebsite();
                }
            }
        });

        mShopPhoneNumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    checkShopPhoneNumber();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_shop_form, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    public boolean checkShopName () {
    //    Log.i(LOG_TAG, "In checkShopName");
        if (mShopName.getText().toString().length() > 0) {
            if (mShopName.getText().toString().length() < 5) {
                mInfoMessage.setVisibility(View.VISIBLE);
                mInfoMessage.setText(getResources().getString(R.string.short_shop_name));
                mShopNameOk = false;
                return false;
            } //255 is maximum allowed length by Google for the shop's name
            else if (mShopName.getText().toString().length() > 254) {
                mInfoMessage.setVisibility(View.VISIBLE);
                mInfoMessage.setText(getResources().getString(R.string.long_shop_name));
                mShopNameOk = false;
                return false;
            } else {
                mInfoMessage.setText("");
                mInfoMessage.setVisibility(View.INVISIBLE);
                if (mShopNameTitle.getCurrentTextColor() == Color.RED)
                    mShopNameTitle.setTextColor(getResources().getColor(R.color.header_text));
                mShopNameOk = true;
                return true;
            }
        }
        return false;
    }

    public boolean checkShopWebsite() {
        if (mShopWebsite.getText().toString().length() > 0) {
            if (!Patterns.WEB_URL.matcher(mShopWebsite.getText()).matches()) {
                mInfoMessage.setVisibility(View.VISIBLE);
                mInfoMessage.setText(getResources().getString(R.string.invalid_url));
                mShopWebsiteOk = false;
                return false;
            } else {
                mInfoMessage.setText("");
                mInfoMessage.setVisibility(View.INVISIBLE);
                mShopWebsiteOk = true;
                return true;
            }
        }
        else { //the field is not mandatory, so it's ok if it's empty
            mInfoMessage.setText("");
            mInfoMessage.setVisibility(View.INVISIBLE);
            if (mShopWebsiteTitle.getCurrentTextColor() == Color.RED)
                mShopWebsiteTitle.setTextColor(getResources().getColor(R.color.header_text));
            mShopWebsiteOk = true;
            return true;
        }
    }

    public boolean checkShopPhoneNumber() {
        if (mShopPhoneNumber.getText().toString().length() > 0) {
            if (mShopPhoneNumber.getText().toString().length() < 7) {
                mInfoMessage.setVisibility(View.VISIBLE);
                mInfoMessage.setText(getResources().getString(R.string.invalid_phone));
                mShopPhoneNumberOk = false;
                return false;
            } else {
                mInfoMessage.setText("");
                mInfoMessage.setVisibility(View.INVISIBLE);
                mShopPhoneNumberOk = true;
                return true;
            }
        }
        else { //the field is not mandatory, so it's ok if it's empty
            mInfoMessage.setText("");
            mInfoMessage.setVisibility(View.INVISIBLE);
            if (mShopPhoneTitle.getCurrentTextColor() == Color.RED)
                mShopPhoneTitle.setTextColor(getResources().getColor(R.color.header_text));
            mShopPhoneNumberOk = true;
            return true;
        }
    }
    //This method is when the user presses "Add shop". It checks the form and displays an info message accordingly.
    public void addShop(View v) {
        final long DELAY_TIME = 2000;
        if (mShopWebsite.getText().toString().length() == 0) {
            //this field is not mandatory
            mShopWebsiteOk = true;
        } //mShopWebsiteOk
        if (checkShopName() && checkShopWebsite() && checkShopPhoneNumber()) {
      //      Log.i(LOG_TAG,"OK to submit form");
            //styling the shop add status
            mInfoMessage.setVisibility(View.VISIBLE);
            mInfoMessage.setBackgroundColor(getResources().getColor(R.color.add_shop_pending));
            mInfoMessage.setText(getResources().getString(R.string.add_shop_pending));
            //styling the form's fields
            mShopName.setBackgroundColor(getResources().getColor(R.color.list_background));
            mShopWebsite.setBackgroundColor(getResources().getColor(R.color.list_background));
            mShopPhoneNumber.setBackgroundColor(getResources().getColor(R.color.list_background));
            Bundle bundle = getIntent().getExtras();
            Double latitude = bundle.getDouble(Constants.ADD_SHOP_BUNDLE_LAT_KEY);
            Double longitude = bundle.getDouble(Constants.ADD_SHOP_BUNDLE_LNG_KEY);
            String address = bundle.getString(Constants.ADD_SHOP_BUNDLE_ADDRESS_KEY);
            String[] postParameters = new String[10];
            //we follow the order of the parameters from the Google API request (https://developers.google.com/places/documentation/actions#adding_a_place)
            postParameters[0] = String.valueOf(latitude);
            postParameters[1] = String.valueOf(longitude);
            postParameters[2] = mShopName.getText().toString();
            if (mShopPhoneNumber.getText().toString() != null) {
                postParameters[3] = mShopPhoneNumber.getText().toString();
            }
            postParameters[4] = address;
            postParameters[5] = Constants.PLACE_TYPE;
            if (mShopWebsite.getText().toString() != null) {
                postParameters[6] = mShopWebsite.getText().toString();
            }
            //sending the form
            PostForm postForm = new PostForm(getApplication());
            postForm.execute(postParameters);
            String response = "";
            try {
                response = postForm.get();
            }
            catch (InterruptedException e) {
                mGaTracker.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("InterruptedException in AddShopFormActivity, addShop")
                        .setFatal(false)
                        .build());
            }
            catch (ExecutionException e) {
                mGaTracker.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("ExecutionException in AddShopFormActivity, addShop")
                        .setFatal(false)
                        .build());
            }

            if (response.indexOf(OK_STATUS) >= 0) {
                mInfoMessage.setVisibility(View.VISIBLE);
                mInfoMessage.setBackgroundColor(getResources().getColor(R.color.map_textview));
                //set the text title color back to black
                mShopNameTitle.setTextColor(Color.BLACK);
                mShopWebsiteTitle.setTextColor(Color.BLACK);
                mShopPhoneTitle.setTextColor(Color.BLACK);
                //the input text format disappears. We have to set it again.
                mShopPhoneNumber.setBackgroundResource(R.drawable.form_input_style);
                mShopWebsite.setBackgroundResource(R.drawable.form_input_style);
                mShopName.setBackgroundResource(R.drawable.form_input_style);
                //
                mInfoMessage.setText(getResources().getString(R.string.add_shop_finished));
                //open the main activity after two seconds
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                        mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(mainActivity);
                    }

                }, DELAY_TIME);
            }
            else if (response.indexOf(ERROR_STATUS) >= 0) {
                mInfoMessage.setVisibility(View.VISIBLE);
                mInfoMessage.setBackgroundColor(Color.RED);
                mInfoMessage.setText(getResources().getString(R.string.add_shop_failed));
                //open the main activity after two seconds
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                        //prevent the user from returning to this activity by pressing the back button
                        mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(mainActivity);
                    }

                }, DELAY_TIME);
            }
        }
        else {
            mInfoMessage.setVisibility(View.VISIBLE);
            mInfoMessage.setText(getResources().getString(R.string.invalid_form));
            //!mShopNameOk
            if (!checkShopName()) {
                //mShopName.setBackgroundColor(getResources().getColor(R.color.invalid_field_color));
                mShopNameTitle.setTextColor(Color.RED);
            }
            else { //check OK
                //mShopName.setBackgroundColor(getResources().getColor(R.color.list_background));
                mShopNameTitle.setTextColor(getResources().getColor(R.color.header_text));
                if (!checkShopWebsite()) {
                    //mShopWebsite.setBackgroundColor(getResources().getColor(R.color.invalid_field_color));
                    mShopWebsiteTitle.setTextColor(Color.RED);
                }
                else {//check OK
                    //mShopWebsite.setBackgroundColor(getResources().getColor(R.color.list_background));
                    mShopWebsiteTitle.setTextColor(getResources().getColor(R.color.header_text));
                    if (!checkShopPhoneNumber()) {
                        //mShopPhoneNumber.setBackgroundColor(getResources().getColor(R.color.invalid_field_color));
                        mShopPhoneTitle.setTextColor(Color.RED);
                    }
                    else {//check OK
                        //mShopPhoneNumber.setBackgroundColor(getResources().getColor(R.color.list_background));
                        mShopPhoneTitle.setTextColor(getResources().getColor(R.color.header_text));
                    }
                }
            }

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.xml.slide_in, R.xml.slide_out);
    }
}
