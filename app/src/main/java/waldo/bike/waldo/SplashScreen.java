package waldo.bike.waldo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import Utilities.Constants;
import Utilities.DeviceConnection;

/**
 * Created by nmihai on 09.12.2014.
 */
public class SplashScreen extends Activity{
    static AlertDialog staticDialog = null;
    private static final String LOG_TAG = SplashScreen.class.getSimpleName();
    private static Context mContext;
    private static boolean isGPSEnabled = false;
    private static boolean isInternetEnabled = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
/*        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            Intent MainActivityIntent = new Intent(SplashScreen.this,MainActivity.class);
            startActivity(MainActivityIntent);
            finish();
            }
        }, Constants.SPLASH_TIME_OUT);*/
        mContext = getApplicationContext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DeviceConnection deviceConnection = new DeviceConnection(getApplicationContext());
        //check if there's Internet Connection
        if (!deviceConnection.checkGpsEnabled()) {
           // Log.i(LOG_TAG, "in OnResume() GPS");
            showGPSDisabledAlertToUser();
        }
        else {
            isGPSEnabled = true;
        }

        if (!deviceConnection.checkInternetConnection()){
            Log.i(LOG_TAG, "in OnResume() Internet");
            showInternetDisabledAlertToUser();
        }
        else {
            isInternetEnabled = true;
        }
        //Log.i(LOG_TAG,"Outside if in onResume()");
        if (isGPSEnabled && isInternetEnabled) {
            startMainActivity(mContext);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG,"In onStart()");
    }

    private void showInternetDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(Constants.INTERNET_IS_DISABLED)
                .setCancelable(false)
                .setPositiveButton(Constants.ENABLE_INTERNET,
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                Intent callSettingIntent = new Intent(
                                        Settings.ACTION_SETTINGS); //open Settings screen
                                startActivity(callSettingIntent);
                            }
                        });
        staticDialog = alertDialogBuilder.create();

        staticDialog.show();

    }

    private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(Constants.GPS_IS_DISABLED)
                .setCancelable(false)
                .setPositiveButton(Constants.ENABLE_GPS,
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);//open GPS Settings screen
                                startActivity(callGPSSettingIntent);
                            }
                        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }


    public static class NetworkChangeReceiver extends BroadcastReceiver {
        public NetworkChangeReceiver() {
            super();
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if ((staticDialog != null) && staticDialog.isShowing()) {
                staticDialog.cancel();
            }
            Log.i(LOG_TAG,"Splash Screen: Network state changed!");
            if (isGPSEnabled) {
                startMainActivity(mContext);
            }
        }
    }

    public static void startMainActivity(Context c) {
        Intent MainActivityIntent = new Intent(c,MainActivity.class);
        MainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(MainActivityIntent);
    }
}
