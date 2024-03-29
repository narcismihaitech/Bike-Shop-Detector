package sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.facebook.android.Util;
import com.google.android.gms.wallet.fragment.BuyButtonAppearance;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.ByteArrayBuffer;
import org.json.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import Utilities.Constants;
import Utilities.GlobalState;
import Utilities.Utility;
import data.ShopsContract;
import waldo.bike.bikeshops.R;

/**
 * Created by Narcis11 on 25.12.2014.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = SyncAdapter.class.getSimpleName();
    private Context mContext;
    private boolean DEBUG = false;
    //indices of the fields collected from the Place Details API call
    final int PHONE_NUMBER_ID = 0;
    final int WEEKDAY_TEXT_ID = 1;
    final int RATING_ID = 2;
    final int WEBSITE_ID = 3;
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

          //  Log.i(LOG_TAG, "Starting sync...");
          //  if (extras.getString(Constants.BUNDLE_SHOP_PLACE_ID,"").equals("")) { //get all shops
                String[] finalResult = new String[100];
                String preferredUnit = Utility.getPreferredUnit(mContext);
                String metric = "Metric";
                String radius = "";
                radius = (preferredUnit.equals(metric)) ? Utility.formatPreferredRangeMetric(mContext) : Utility.formatPreferredRangeImperial(mContext);
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;
                String placesJsonStr = "";//used for storing the response from the API call
                //used for querying the Google Places API
                final String types = Constants.PLACE_TYPE;
                final String key = Constants.API_KEY;
                final String latLng = GlobalState.USER_LAT + Constants.COMMA_SEPARATOR + GlobalState.USER_LNG;
                //final String latLng = "44.4391463,26.1428946";
                final String output = "json";
                Vector<ContentValues> cVVector = new Vector<ContentValues>(120);//The current API can't return more than 60 results, but we'll take a buffer
                String[] place_ids = new String[120];//used for getting the partner shops at a later stage
                boolean flag_next_page = true;
                String next_page_token_value = "";
                int countLoops = 0;
                int j = 0;//used to populate the vector of place_ids
        while (flag_next_page) {
            try {
                //******Getting the info for all shops*****
                //the query parameters used in the Nearby search call
                final String BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/" + output + "?";
                final String QUERY_LOCATION = "location";
                final String QUERY_RADIUS = "radius";
                final String QUERY_KEY = "key";
                final String QUERY_TYPES = "types";
                final String PAGE_TOKEN = "pagetoken";
                Uri builtUri;
                //build up the URI
                if (next_page_token_value.equals("")) {
                    builtUri = Uri.parse(BASE_URL).buildUpon()
                            .appendQueryParameter(QUERY_LOCATION, latLng)
                            .appendQueryParameter(QUERY_RADIUS, radius)
                            .appendQueryParameter(QUERY_KEY, key)
                            .appendQueryParameter(QUERY_TYPES, types)
                            .build();
                }
                else {
                    builtUri = Uri.parse(BASE_URL).buildUpon()
                            .appendQueryParameter(QUERY_LOCATION, latLng)
                            .appendQueryParameter(QUERY_RADIUS, radius)
                            .appendQueryParameter(QUERY_KEY, key)
                            .appendQueryParameter(QUERY_TYPES, types)
                            .appendQueryParameter(PAGE_TOKEN,next_page_token_value)
                            .build();
                }
                  Log.i(LOG_TAG, "Places Uri is: " + builtUri.toString());

                URL url = new URL(builtUri.toString());

                //Create the request to Google, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(Constants.HTTP_GET);
                urlConnection.connect();
                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    //  Log.i(LOG_TAG, "No input stream");
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    //  Log.i(LOG_TAG, "buffer.length() == 0");
                }
                placesJsonStr = buffer.toString();
                //   Log.i(LOG_TAG,"Response is: " + placesJsonStr);
            } catch (IOException e) {
                // Log.e(LOG_TAG, "Error in fetching places: " + e);
                flag_next_page = false;//exit at the end of the loop if there's no Internet connection
                //Log.i(LOG_TAG,"At line 154 set flag_next_page to " + flag_next_page);

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        //  Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            //*****Parsing the info for all shops*******
            final String API_RESULT = "results";//root
            final String API_STATUS = "status";//we'll perform some checks on this one
            // Location information
            final String API_PLACE_ID = "place_id";
            final String API_NAME = "name";
            final String API_OPENING_HOURS = "opening_hours";//root
            final String API_OPEN_NOW = "open_now"; //child of opening_hours
            final String API_ADDRESS = "vicinity";

            final String API_GEOMETRY = "geometry";
            //child of geometry
            final String API_LOCATION = "location";
            //children of location
            final String API_COORD_LAT = "lat";
            final String API_COORD_LONG = "lng";
            final String NEXT_PAGE_TOKEN = "next_page_token";
            int distanceToShop;
            double distanceDuration;
            String apiCallStatus = "";
            int isShopOpen = 2; //means that this info is not available
            try {
              //  Log.i(LOG_TAG,"placesJsonStr: " + placesJsonStr);
                JSONObject placesJson = new JSONObject(placesJsonStr);
                apiCallStatus = placesJson.getString(API_STATUS);
                try {
                    next_page_token_value = placesJson.getString(NEXT_PAGE_TOKEN);
                    //Log.i(LOG_TAG,"Token is: " + next_page_token_value);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage());
                    flag_next_page = false;//no more information to collect, so this is the last loop
                   // Log.i(LOG_TAG,"At line 201 set flag_next_page to " + flag_next_page);
                }
                //  Log.i(LOG_TAG, "Status is " + apiCallStatus);
                //we need an intent to signal when the sync has finished
                Intent syncIntent = new Intent();
                syncIntent.setAction(Constants.SYNC_BUNDLE_STATUS_ACTION);
                if (apiCallStatus.equals(Constants.OK_STATUS)) { //we only parse if the result is OK
                    JSONArray placesArray = placesJson.getJSONArray(API_RESULT); //root node
                    for (int i = 0; i < placesArray.length(); i++) {
                        // These are the values that will be collected.
                        String place_id;
                        String placeName;
                        String address;
                        //some shops do not have this piece of info, so we presume from the start that it's unavailable
                        String openNow = Constants.NOT_AVAILABLE;
                        String latitude;
                        String longitude;

                        // placeDetails is the whole object representing a shop
                        JSONObject placeDetails = placesArray.getJSONObject(i);
                        JSONObject geometry = placeDetails.getJSONObject(API_GEOMETRY); //geometry object
                        JSONObject location = geometry.getJSONObject(API_LOCATION); //location object
                        latitude = location.getString(API_COORD_LAT);
                        longitude = location.getString(API_COORD_LONG);
                        //getting info from opening_hours
                        try {
                            //some shops don't have opening hours, that's why we put this request into a try/catch
                            JSONObject openingHours = placeDetails.getJSONObject(API_OPENING_HOURS);
                            openNow = openingHours.getString(API_OPEN_NOW);
                        } catch (JSONException e) {
                            //  Log.e(LOG_TAG, "Opening Hours JSON Exception: " + e.getMessage());
                        }
                        //44.4391463,26.1428946
                        Location userLocation = new Location(Constants.PROVIDER);
                        //      Log.i(LOG_TAG, "User location = " + GlobalState.USER_LAT + "/" + GlobalState.USER_LNG);
              /*        userLocation.setLatitude(Double.valueOf("44.4391463"));
                        userLocation.setLongitude(Double.valueOf("26.1428946"));*/
                        userLocation.setLatitude(Double.valueOf(GlobalState.USER_LAT));
                        userLocation.setLongitude(Double.valueOf(GlobalState.USER_LNG));
                        Location shopLocation = new Location(Constants.PROVIDER);
                        shopLocation.setLatitude(Double.valueOf(latitude));
                        shopLocation.setLongitude(Double.valueOf(longitude));
                        distanceToShop = (int) Math.round(userLocation.distanceTo(shopLocation));
                        distanceDuration = Utility.calculateDistanceDuration(distanceToShop, getContext());
                        //main info from the root object
                        place_id = placeDetails.getString(API_PLACE_ID);
                        //Log.i(LOG_TAG,"place_id = " + place_id);
                        placeName = placeDetails.getString(API_NAME);
                        address = placeDetails.getString(API_ADDRESS);

                        if (!openNow.equals(Constants.NOT_AVAILABLE) && openNow != null) {
                            isShopOpen = Boolean.valueOf(openNow) ? 1 : 0;
                        }
                        //get the details for each shop
                        String[] placeDetailRequest = getPlaceDetails(place_id);

                        //build the vector of place ids
                        place_ids[j] = place_id;
                        j++;//i is reset every time a new loop is entered, so we need a different counter
                        ContentValues shopsValues = new ContentValues();//the shops will be stored temporarily here before we insert them in the DB
                        //creating the vector and inserting the values
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_NAME, placeName);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_ADDRESS, address);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_LATITUDE, latitude);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_LONGITUDE, longitude);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_IS_OPEN, isShopOpen);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_DISTANCE_TO_USER, distanceToShop);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_DISTANCE_DURATION, distanceDuration);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_PLACE_ID, place_id);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_WEBSITE, placeDetailRequest[WEBSITE_ID]);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_PHONE_NUMBER, placeDetailRequest[PHONE_NUMBER_ID]);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_OPENING_HOURS, placeDetailRequest[WEEKDAY_TEXT_ID]);
                        shopsValues.put(ShopsContract.ShopsEntry.COLUMN_RATING, placeDetailRequest[RATING_ID]);
                        cVVector.add(shopsValues);
                      //  shopsValues.clear();
                    }
                    if (cVVector.size() > 0 && !flag_next_page) {
                        //if flag_next_page is false, we are in the last loop
                        //we empty the database before inserting the new data
                        mContext.getContentResolver().delete(ShopsContract.ShopsEntry.CONTENT_URI, null, null);
                        ContentValues[] cvArray = new ContentValues[cVVector.size()];
                        cVVector.toArray(cvArray);
                        int rowsInserted;
                        rowsInserted = mContext.getContentResolver().bulkInsert(
                                ShopsContract.ShopsEntry.CONTENT_URI,
                                cvArray);

                        //final step: get the partner shops
                        if (rowsInserted > 0) getPartnerShops(place_ids);

                        if (DEBUG) {
                            Log.i(LOG_TAG,"Vector size: " + cVVector.size());
                            Log.i(LOG_TAG, "No of bulk rows inserted = " + rowsInserted);
                            int noOfElements = 0;
                            for (int a = 0; a < place_ids.length; a++) {
                                if (place_ids[a] != null)
                                    noOfElements++;
                            }
                            Log.i(LOG_TAG, "place_ids size: " + noOfElements);
                            Cursor shopsCursor = mContext.getContentResolver().query(
                                    ShopsContract.ShopsEntry.CONTENT_URI,
                                    null,
                                    null,
                                    null,
                                    null
                            );
                            Log.i(LOG_TAG, "No of rows in shops = " + shopsCursor.getCount());
                            if (shopsCursor.moveToFirst()) {
                                ContentValues resultValues = new ContentValues();
                                DatabaseUtils.cursorRowToContentValues(shopsCursor, resultValues);
                                Log.i(LOG_TAG, "Query succeeded! **********");
                               /* for (String loopKey : resultValues.keySet()) {
                                    Log.i(LOG_TAG, loopKey + ": " + resultValues.getAsString(loopKey));
                                }*/
                            } else {
                                Log.i(LOG_TAG, "Query failed! :( **********");
                            }
                            shopsCursor.close();
                        }
                    }
                } else {//notify that the sync is complete (actually, it never happened
                    //we notify only when an error occured/there were no results. If the sync goes well, we remove the refresh circle in the onLoadFinished
                    //from Shop Fragment
                    syncIntent.putExtra(Constants.SYNC_BUNDLE_STATUS_KEY, Constants.SYNC_BUNDLE_STATUS_STOPPED);//the sync has stopped
                    syncIntent.putExtra(Constants.SYNC_BUNDLE_RESULT_KEY, apiCallStatus);//the status of the sync
                    mContext.sendBroadcast(syncIntent);
                }

            } catch (JSONException e) {
                //  Log.e(LOG_TAG, "Caught JSON Exception: " + e.getMessage());
                flag_next_page = false;
               // Log.i(LOG_TAG,"At line 335 set flag_next_page to " + flag_next_page);
                e.printStackTrace();
            }
            //}

        }
       // Log.i(LOG_TAG,"Number of loops: " + countLoops);
        }

    public static void syncImmediately(Context context/*, String placeId*/) {
       // Log.i(LOG_TAG, "In syncImmediately");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
/*        if (placeId != null) {
            bundle.putString(Constants.BUNDLE_SHOP_PLACE_ID,placeId);
        }*/
        if (ContentResolver.isSyncPending(getSyncAccount(context), context.getString(R.string.content_authority))  ||
                ContentResolver.isSyncActive(getSyncAccount(context), context.getString(R.string.content_authority))) {
         //   Log.i("ContentResolver", "SyncPending, canceling");
            ContentResolver.cancelSync(getSyncAccount(context), context.getString(R.string.content_authority));
        }
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */


        }
        return newAccount;
    }
    //get the details for each shop
    private String[] getPlaceDetails (String place_id) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String placeDetailsJsonStr = "";//used for storing the response from the Place Details API call
        final String key = Constants.API_KEY;
        final String QUERY_KEY = "key";
        final String output = "json";
        final String placeId = "placeid";
        String returnPlaceDetails[] = new String[5];
        try {
            final String BASE_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/" + output + "?";
            Uri builtPlaceUri = Uri.parse(BASE_DETAILS_URL).buildUpon()
                    .appendQueryParameter(placeId,place_id)
                    .appendQueryParameter(QUERY_KEY,key)
                    .build();
            //Log.i(LOG_TAG, "Place Details Uri is: " + builtPlaceUri.toString());
            URL url = new URL(builtPlaceUri.toString());
            //Create the request to Google, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(Constants.HTTP_GET);
            urlConnection.connect();
            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
               // Log.i(LOG_TAG, "No input stream");
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
               // Log.i(LOG_TAG, "buffer.length() == 0");
            }
            placeDetailsJsonStr = buffer.toString();
        }
        catch(IOException e) {
          //  Log.e(LOG_TAG, "Error in fetching place details for place_id: + " + place_id + ". Error: " + e);
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                   // Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

       //******Beginning to parse the result*****
        final String API_RESULT = "result";
        final String API_PHONE_NUMBER = "formatted_phone_number";
        final String API_WEEKDAY_TEXT = "weekday_text";
        final String API_RATING = "rating";
        final String API_WEBSITE = "website";
        final String API_STATUS = "status";
        final String API_OPENING_HOURS = "opening_hours";
        final String API_REVIEWS = "reviews";
        final String API_ASPECTS = "aspects";
        Double sumOfRatings = 0.0;
        String placePhoneNumber = "";
        String placeOpeningHours = "";
        Double rating = 0.0;
        String website = "";
        String apiCallStatus = "";
        String placeReviews = "";
        JSONObject singleReviewJson;
        JSONArray aspectsArray;
        try {
            JSONObject placeDetailsJson = new JSONObject(placeDetailsJsonStr);
            apiCallStatus = placeDetailsJson.getString(API_STATUS);
            //we only parse if the result is OK. Otherwise, we return an empty string[].
            if (apiCallStatus.equals(Constants.OK_STATUS)) {
                //We have to wrap all of the extractions from the response in a try-catch phrase, because not all of the fields are available.
                JSONObject placeDetailsObject = placeDetailsJson.getJSONObject(API_RESULT);//root node
                try {//get phone number
                    placePhoneNumber = placeDetailsObject.getString(API_PHONE_NUMBER);
                }
                catch(JSONException phoneException) {
                   // Log.i(LOG_TAG,phoneException.getMessage());
                }
                //get opening hours
                try {
                    JSONObject openingHours = placeDetailsObject.getJSONObject(API_OPENING_HOURS);
                    JSONArray jsonWeekdayArray = (JSONArray) openingHours.get(API_WEEKDAY_TEXT);
                    for (int i = 0; i < jsonWeekdayArray.length(); i++) {
                        //Log.i(LOG_TAG,"jsonWeekdayArray.get(i): " + jsonWeekdayArray.get(i));
                        placeOpeningHours = placeOpeningHours + jsonWeekdayArray.get(i) + Constants.HASH_SEPARATOR;
                    }
                }
                catch (JSONException e) {
                   // Log.i(LOG_TAG,e.getMessage());
                }
                try {//get rating
                    rating = placeDetailsObject.getDouble(API_RATING);
                    returnPlaceDetails[RATING_ID] = String.valueOf(rating);
                }
                catch (JSONException e) {
                  //  Log.i(LOG_TAG,e.getMessage());
                  //  Log.i(LOG_TAG,"Wir rechnen den durchschnittlichen Wert");
                    try {//if there's no "rating" field in the root node, we calculate the mean rating from the reviews
                        JSONArray placeReviewsArray = (JSONArray) placeDetailsObject.get(API_REVIEWS);
                        for (int i = 0; i < placeReviewsArray.length(); i++) {
                            singleReviewJson = placeReviewsArray.getJSONObject(i);//a whole review
                            //aspectsArray = singleReviewJson.getJSONArray(API_ASPECTS);
                            rating = singleReviewJson.getDouble(API_RATING);
                            sumOfRatings += rating;
                        }
                        //format the result to one decimal before returning
                        BigDecimal returnValueRating = new BigDecimal(sumOfRatings/placeReviewsArray.length());
                        returnValueRating = returnValueRating.setScale(1, RoundingMode.HALF_UP);
                        returnPlaceDetails[RATING_ID] = returnValueRating.toString();
                    }
                    catch (JSONException jsonExcep) {
                        //Log.i(LOG_TAG,jsonExcep.getMessage());
                    }

                }
                try {
                    website = placeDetailsObject.getString(API_WEBSITE);
                }
                catch (JSONException json) {
                    //Log.i(LOG_TAG,json.getMessage());
                }
                returnPlaceDetails[PHONE_NUMBER_ID] = placePhoneNumber;
                returnPlaceDetails[WEEKDAY_TEXT_ID] = placeOpeningHours;
                returnPlaceDetails[WEBSITE_ID] = website;
            }
        }
        catch(JSONException e) {
            //Log.e(LOG_TAG, "Parsing place details | JSON Exception: " + e.getMessage());
        }
        return returnPlaceDetails;
    }

    //the HTTP POST to get the partner shops
    private void getPartnerShops(String[] place_ids) {
        String shopsJson = createJsonObject(place_ids);
        String url = "http://app.waldo.bike:8888/querypartners";
        HttpPost httpPost = new HttpPost(url);
        HttpClient httpClient = new DefaultHttpClient();
        final String HEADER_KEY = "Content-Type";
        final String HEADER_VALUE = "application/json";
        String responseString = "";
        //used for the DB update
        ContentValues updateValues = new ContentValues();
        String whereClause;
        final int IS_PARTNER = 1;
        try {
            StringEntity stringEntity = new StringEntity(shopsJson, HTTP.UTF_8);
            httpPost.setEntity(stringEntity);
            httpPost.setHeader(HEADER_KEY,HEADER_VALUE);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = httpClient.execute(httpPost, responseHandler);
            responseString = response.toString();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        //*******Parsing the result*******
        //the keys
        final String ROOT_NODE = "places";
        final String PARTNER_ID = "_id";
        final String PARTNER_DISCOUNT = "discountValue"; //int
        final String PARTNER_TEXTA = "texta"; //first promo text
        final String PARTNER_TEXTB = "textb"; //second promo text
        final String PARTNER_NAME = "name";
        final String PARTNER_NUMBER = "telephoneNumber";
        final String PARTNER_LOGO = "logoUrl"; //the URL of the partner's logo
        final String PARTNER_ADDRESS = "address";
        final String PARTNER_SCHEDULE = "opening_hours";
        final String PARTNER_CAMERA_BEARING = "bearing";
        final String PARTNER_CAMERA_TILT = "tilt";
        final String PARTNER_CAMERA_ZOOM = "zoom";
        final String PARTNER_CAMERA_POSITION = "position";
        //the values
        String place_id = "";
        int shop_discount;
        String shop_textA = "";
        String shop_textB = "";
        String shop_name = "";
        String shop_number = "";
        String shop_logo = "";
        String shop_address = "";
        String shop_schedule = "";
        double shop_camera_bearing;
        float shop_camera_bearing_float;
        double shop_camera_tilt;
        float shop_camera_tilt_float;
        double shop_camera_zoom;
        float shop_camera_zoom_float;
        String shop_camera_position;
        try {
           // Log.i(LOG_TAG,"Response is: " + responseString);
            JSONObject partnersJson = new JSONObject(responseString);
            JSONArray partnersArray = partnersJson.getJSONArray(ROOT_NODE);
            JSONObject partnerShopDetails;
            //parsing each shop
            for (int i = 0; i < partnersArray.length(); i ++) {
                partnerShopDetails = partnersArray.getJSONObject(i);
                //get the place_id
                try {
                    place_id = partnerShopDetails.getString(PARTNER_ID);
                }
                catch (JSONException a) {
                   // a.printStackTrace();
                }
                //get the discount
                try{
                    shop_discount = partnerShopDetails.getInt(PARTNER_DISCOUNT);
                    updateValues.put(ShopsContract.ShopsEntry.COLUMN_DISCOUNT_VALUE,shop_discount);
                }
                catch (JSONException f) {
                  //  f.printStackTrace();
                }
                //get the first promo text
                try{
                    shop_textA = partnerShopDetails.getString(PARTNER_TEXTA);
                }
                catch (JSONException c) {
                    shop_textA = "";
                  //  c.printStackTrace();
                }
                //get the second promo text
                try {
                    shop_textB = partnerShopDetails.getString(PARTNER_TEXTB);
                    updateValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_PROMO_TEXT,shop_textA + Constants.HASH_SEPARATOR + shop_textB);
                }
                catch (JSONException d) {
                    shop_textB = "";
                    updateValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_PROMO_TEXT,shop_textA + Constants.HASH_SEPARATOR + shop_textB);
                  //  d.printStackTrace();
                }

                //get the logo URL and value
                try {
                    shop_logo = partnerShopDetails.getString(PARTNER_LOGO);
                    updateValues.put(ShopsContract.ShopsEntry.COLUMN_LOGO_URL, shop_logo);
                } catch (JSONException e) {
                  //  e.printStackTrace();
                }

                //get the camera's bearing
                try {
                    shop_camera_bearing = partnerShopDetails.getDouble(PARTNER_CAMERA_BEARING);
                    shop_camera_bearing_float = (float) shop_camera_bearing;
                    updateValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_CAMERA_BEARING,shop_camera_bearing_float);//
                }
                catch (JSONException e) {
                 //   e.printStackTrace();
                }
                //the camera's details are more likely to be changed, so we place them outside of the if
                //get the camera's tilt
                try {
                    shop_camera_tilt = partnerShopDetails.getDouble(PARTNER_CAMERA_TILT);
                    shop_camera_tilt_float = (float) shop_camera_tilt;
                    updateValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_CAMERA_TILT,shop_camera_tilt_float);
                }
                catch (JSONException e) {
                  //  e.printStackTrace();
                }

                //get the camera's zoom
                try {
                    shop_camera_zoom = partnerShopDetails.getDouble(PARTNER_CAMERA_ZOOM);
                    shop_camera_zoom_float = (float) shop_camera_zoom;
                    updateValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_CAMERA_ZOOM,shop_camera_zoom_float);
                }
                catch (JSONException e) {
                   // e.printStackTrace();
                }

                //get the camera's position
                try {
                    shop_camera_position = partnerShopDetails.getString(PARTNER_CAMERA_POSITION);
                    updateValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_CAMERA_POSITION, shop_camera_position);
                }
                catch (JSONException e) {
                  //  e.printStackTrace();
                }
                //if necessary, get the rest of the details for the partner shop
                if (partnerShopDetails.length() > 4) {

                    //get the name
                    try {
                        shop_name = partnerShopDetails.getString(PARTNER_NAME);
                        updateValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_NAME, shop_name);
                    } catch (JSONException n) {
                        n.printStackTrace();
                    }

                    //get the phone number
                    try {
                        shop_number = partnerShopDetails.getString(PARTNER_NUMBER);
                        updateValues.put(ShopsContract.ShopsEntry.COLUMN_PHONE_NUMBER, shop_number);
                    } catch (JSONException b) {
                        b.printStackTrace();
                    }

                    //get the address
                    try {
                        shop_address = partnerShopDetails.getString(PARTNER_ADDRESS);
                        updateValues.put(ShopsContract.ShopsEntry.COLUMN_SHOP_ADDRESS, shop_address);
                    } catch (JSONException g) {
                        g.printStackTrace();
                    }

                    //get the schedule
                    try {
                        JSONArray jsonWeekdayArray = (JSONArray) partnerShopDetails.get(PARTNER_SCHEDULE);
                        for (int j = 0; j < jsonWeekdayArray.length(); j++) {
                            shop_schedule = shop_schedule + jsonWeekdayArray.get(j) + Constants.HASH_SEPARATOR;
                        }
                        updateValues.put(ShopsContract.ShopsEntry.COLUMN_OPENING_HOURS,shop_schedule);
                    }
                    catch (JSONException s) {
                        s.printStackTrace();
                    }

                }
            //update the DB
            updateValues.put(ShopsContract.ShopsEntry.COLUMN_IS_PARTNER,IS_PARTNER);
            whereClause = ShopsContract.ShopsEntry.COLUMN_PLACE_ID + " = '" + place_id + "'";
            mContext.getContentResolver().update(ShopsContract.ShopsEntry.CONTENT_URI,updateValues,whereClause,null);
            shop_textA = "";
            shop_textB = "";
            updateValues.clear();
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //This method creates the JSON object that will be used in getPartnerShops()
    private String createJsonObject(String[] place_ids) {
        JSONObject allShops = new JSONObject();
        final String JSON_KEY = "places";
        final String JSON_KEY_BUCKET = "phone_bucket";
        String phoneBucket = Utility.getPhoneBucket(mContext);
        try {
            for (int i = 0; i < place_ids.length; i++) {
                allShops.accumulate(JSON_KEY, place_ids[i]);
            }
            allShops.accumulate(JSON_KEY_BUCKET, phoneBucket);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return allShops.toString();
    }
}
