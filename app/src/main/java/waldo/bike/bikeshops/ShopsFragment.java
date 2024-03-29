package waldo.bike.bikeshops;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import Utilities.Constants;
import Utilities.DeviceConnection;
import Utilities.GlobalState;
import Utilities.Utility;
import data.ShopsContract;
import sync.SyncAdapter;
/**
 * Created by Narcis11 on 20.12.2014.
 */
public class ShopsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener {


    public SimpleCursorAdapter mShopsAdapter;
    private String mRadius;
    private static final String LOG_TAG = ShopsFragment.class.getSimpleName();
    private String mShopLatitude = "";
    private String mShopLongitude = "";
    private String mPlaceId;
    private String mShopName = "";
    private String mFormattedDuration = "";
    private String mFormattedDistance = "";
    private String mPreferredUnit = "";
    private boolean mIsPartner;
    private String mPromoText;
    private boolean mIsListRefreshed;
    private Double mNewSpeedDistanceToShop;
    private float mShopCameraBearing;
    private float mShopCameraTilt;
    private float mShopCameraZoom;
    private String mShopCameraPosition = "";
    private static final int SHOPS_LOADER_ID = 0;//loader identifier
    private ListView mListView;
    private boolean mIsSpeedChanged;
    private SwipeRefreshLayout swipeLayout;
    private int mPosition = ListView.INVALID_POSITION;
    private static final String SELECTED_KEY = "selected_position";
    //the intent filter used to determine the sync status
    private IntentFilter mSyncFilter;
    //used by Google Analytics
    private Tracker mGaTracker;
    //used for maintaining the listview state
    Bundle mOutBundle;
    String mTestShopName;
    private static Parcelable mListViewScrollPos = null;
    public static final String[] SHOPS_COLUMNS = {
            ShopsContract.ShopsEntry.TABLE_NAME + "." + ShopsContract.ShopsEntry._ID,
            ShopsContract.ShopsEntry.COLUMN_SHOP_NAME,
            ShopsContract.ShopsEntry.COLUMN_SHOP_ADDRESS,
            ShopsContract.ShopsEntry.COLUMN_IS_OPEN,
            ShopsContract.ShopsEntry.COLUMN_DISTANCE_TO_USER,
            ShopsContract.ShopsEntry.COLUMN_DISTANCE_DURATION,
            ShopsContract.ShopsEntry.COLUMN_SHOP_LATITUDE,
            ShopsContract.ShopsEntry.COLUMN_SHOP_LONGITUDE,
            ShopsContract.ShopsEntry.COLUMN_PLACE_ID,
            ShopsContract.ShopsEntry.COLUMN_IS_PARTNER,
            ShopsContract.ShopsEntry.COLUMN_SHOP_PROMO_TEXT,
            ShopsContract.ShopsEntry.COLUMN_DISCOUNT_VALUE,
           // ShopsContract.ShopsEntry.COLUMN_LOGO_VALUE
            ShopsContract.ShopsEntry.COLUMN_LOGO_URL,
            ShopsContract.ShopsEntry.COLUMN_SHOP_CAMERA_BEARING,
            ShopsContract.ShopsEntry.COLUMN_SHOP_CAMERA_TILT,
            ShopsContract.ShopsEntry.COLUMN_SHOP_CAMERA_ZOOM,
            ShopsContract.ShopsEntry.COLUMN_SHOP_CAMERA_POSITION
    };

    // These indices are tied to SHOPS_COLUMNS.  If SHOPS_COLUMNS changes, these
    // must change.
    public static final int COL_SHOP_ID = 0;
    public static final int COL_SHOP_NAME = 1;
    public static final int COL_SHOP_ADDRESS = 2;
    public static final int COL_IS_OPEN = 3;
    public static final int COL_DISTANCE_TO_USER = 4;
    public static final int COL_DISTANCE_DURATION = 5;
    public static final int COL_SHOP_LATITUDE = 6;
    public static final int COL_SHOP_LONGITUDE = 7;
    public static final int COL_PLACE_ID = 8;
    public static final int COL_IS_PARTNER = 9;
    public static final int COL_PROMO_TEXT = 10;
    public static final int COL_DISCOUNT_VALUE = 11;
    public static final int COL_LOGO_URL = 12;
    public static final int COL_SHOP_CAMERA_BEARING = 13;
    public static final int COL_SHOP_CAMERA_TILT = 14;
    public static final int COL_SHOP_CAMERA_ZOOM = 15;
    public static final int COL_SHOP_CAMERA_POSITION = 16;

    public ShopsFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        mShopsAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.list_item_shops_cards,
                null,
                new String[] {
                        ShopsContract.ShopsEntry.COLUMN_SHOP_NAME,
                        ShopsContract.ShopsEntry.COLUMN_SHOP_ADDRESS,
                        ShopsContract.ShopsEntry.COLUMN_IS_OPEN,
                        ShopsContract.ShopsEntry.COLUMN_DISTANCE_TO_USER,
                        ShopsContract.ShopsEntry.COLUMN_DISTANCE_DURATION,
                        ShopsContract.ShopsEntry.COLUMN_DISCOUNT_VALUE,
                        ShopsContract.ShopsEntry.COLUMN_LOGO_URL
                },
                new int[] {
                        R.id.list_item_shopname_textview,
                        R.id.list_item_shopaddress_textview,
                        R.id.list_item_shopisopen_textview,
                        R.id.list_item_distance_textview,
                        R.id.list_item_duration_textview,
                        R.id.list_item_discount_view,
                        R.id.list_item_icon
                },
                0
        );

    //    Log.i(LOG_TAG,"Size of mShopsAdapter = " + mShopsAdapter.getCount());

        // Get a reference to the ListView, and attach this adapter to it.
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final ListView listView = (ListView) rootView.findViewById(R.id.listview_shops);
        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        mListView = listView;
        listView.setAdapter(mShopsAdapter);

        //we need to format the data from the database
        mShopsAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                switch (columnIndex) {
                    case COL_SHOP_NAME:
                        //check if there are records with the shop name populated (a mandatory column)
                        GlobalState.IS_DATABASE_POPULATED = cursor.getString(COL_SHOP_NAME) != null || !cursor.getString(COL_SHOP_NAME).equals("");
                        //Log.i(LOG_TAG,"Populated: " + GlobalState.IS_DATABASE_POPULATED);
                        //set the text
                        mTestShopName = cursor.getString(COL_SHOP_NAME);
                        ((TextView) view).setText(cursor.getString(COL_SHOP_NAME));
                        return true;
                    case COL_IS_OPEN:
                        if (cursor.getInt(COL_IS_OPEN) == 1){
                            ((TextView) view).setText(Constants.SHOP_OPEN); //"Open"
                        }
                        else if (cursor.getInt(COL_IS_OPEN) == 0) {
                            ((TextView) view).setText(Constants.SHOP_CLOSED);//"Closed"
                        }
                        else {
                            ((TextView) view).setText(Constants.SHOP_UNAVAILABLE);//""
                        }
                        return true;
                    case COL_DISTANCE_TO_USER:
                        //    Log.i(LOG_TAG,"Shopname / distance: " + cursor.getString(COL_SHOP_NAME) + " / " + cursor.getString(COL_DISTANCE_TO_USER));
                        mPreferredUnit = Utility.getPreferredUnit(getActivity());
                        if (mPreferredUnit.equals(getResources().getString(R.string.unit_array_metric))) {
                            mFormattedDistance = Utility.formatDistanceMetric(cursor.getString(COL_DISTANCE_TO_USER));
                        }
                        else {
                            mFormattedDistance = Utility.formatDistanceImperial(cursor.getString(COL_DISTANCE_TO_USER));
                        }
                        ((TextView) view).setText(mFormattedDistance);
                        return true;
                    case COL_DISTANCE_DURATION:
                        if (!mIsSpeedChanged) {
                            mFormattedDuration = Utility.formatDistanceDuration(cursor.getString(COL_DISTANCE_DURATION));
                            ((TextView) view).setText(mFormattedDuration);
                        }
                        else {
                            int distanceToShop = Integer.valueOf(cursor.getString(COL_DISTANCE_TO_USER));
                            mNewSpeedDistanceToShop = Utility.calculateDistanceDuration(distanceToShop,getActivity());
                            mFormattedDuration = Utility.formatDistanceDuration(String.valueOf(mNewSpeedDistanceToShop));
                            ((TextView) view).setText(mFormattedDuration);
                            //  return true;
                        }
                        return true;
                    case COL_DISCOUNT_VALUE:
                        if ((cursor.getInt(COL_IS_PARTNER) == 1)) {
                            try {
                                int discountValue = cursor.getInt(COL_DISCOUNT_VALUE); //this call sometimes throws an exception if the column
                                // is null/out of limits
                                if (discountValue != 0) {
                                    ((TextView) view).setText("-" + String.valueOf(discountValue) + "%");
                                    view.setVisibility(View.VISIBLE);
                                }
                            }
                            catch(NullPointerException e) {
                                view.setVisibility(View.GONE);
                            }
                        }
                        else{
                            view.setVisibility(View.GONE);
                        }
                        return true;
                    case COL_LOGO_URL:
                        if ((cursor.getInt(COL_IS_PARTNER) == 1)) {
                            ImageView imageView = (ImageView) view;
                            Picasso.with(getActivity().getApplicationContext())
                                    .load(cursor.getString(COL_LOGO_URL))
                                    .placeholder(R.drawable.bike_tool_kit)
                                    .error(R.drawable.bike_tool_kit)
                                    .into(imageView);
                        }
                        else {
                            ImageView imageViewNon = (ImageView) view;
                            imageViewNon.setImageDrawable(getResources().getDrawable(R.drawable.bike_tool_kit));
                            //non-partner shop
                        }
                        return true;
                }
                return false;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mShopsAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    Intent openDetailActivity = new Intent(getActivity().getApplicationContext(), ShopDetailActivity.class);
                    Bundle bundle = new Bundle();
                    mShopName = cursor.getString(COL_SHOP_NAME);
                    mShopLatitude = cursor.getString(COL_SHOP_LATITUDE);
                    mShopLongitude = cursor.getString(COL_SHOP_LONGITUDE);
                    mPlaceId = cursor.getString(COL_PLACE_ID);
                    mIsPartner = (cursor.getInt(COL_IS_PARTNER) == 1);
                    mPromoText = cursor.getString(COL_PROMO_TEXT);
                    //we get this data here, because in ShopDetailActivity onLoadFinished is called after onStreetViewPanoramaReady
                    mShopCameraBearing = (cursor.getFloat(COL_SHOP_CAMERA_BEARING) != 0) ? cursor.getFloat(COL_SHOP_CAMERA_BEARING) : 0;
                    mShopCameraTilt = (cursor.getFloat(COL_SHOP_CAMERA_TILT) != 0) ? cursor.getFloat(COL_SHOP_CAMERA_TILT) : 0;
                    mShopCameraZoom = (cursor.getFloat(COL_SHOP_CAMERA_ZOOM) != 0) ? cursor.getFloat(COL_SHOP_CAMERA_ZOOM) : 0;
                    mShopCameraPosition = (cursor.getString(COL_SHOP_CAMERA_POSITION) != null) ? cursor.getString(COL_SHOP_CAMERA_POSITION) : "";
                    //update the database row corresponding to this shop id
                   // updateShopList(getActivity(),mPlaceId);
                    //store the position
                    mPosition = position;
                    //set the event to GA
                    mGaTracker.send(new HitBuilders.EventBuilder()
                            .setCategory(getString(R.string.ga_open_shop_category_id))
                            .setAction(getString(R.string.ga_open_shop_action_id))
                            .setLabel(mShopName)
                            .build());
                    //assemble the bundle
                    bundle.putString(Constants.BUNDLE_SHOP_LAT, mShopLatitude);
                    bundle.putString(Constants.BUNDLE_SHOP_LNG, mShopLongitude);
                    bundle.putString(Constants.BUNDLE_SHOP_NAME,mShopName);
                    bundle.putString(Constants.BUNDLE_SHOP_PLACE_ID,mPlaceId);
                    bundle.putBoolean(Constants.BUNDLE_IS_PARTNER, mIsPartner);
                    bundle.putString(Constants.BUNDLE_FRAGMENT, Constants.CALLED_FROM_FRAGMENT);
                    bundle.putString(Constants.BUNDLE_PROMO_TEXT, mPromoText);
                    bundle.putFloat(Constants.BUNDLE_SHOP_CAMERA_BEARING, mShopCameraBearing);
                    bundle.putFloat(Constants.BUNDLE_SHOP_CAMERA_TILT, mShopCameraTilt);
                    bundle.putFloat(Constants.BUNDLE_SHOP_CAMERA_ZOOM, mShopCameraZoom);
                    bundle.putString(Constants.BUNDLE_SHOP_CAMERA_POSITION, mShopCameraPosition);
                    openDetailActivity.putExtras(bundle);
                    //lift off!
                    startActivity(openDetailActivity);
                }
            }
        });
        // Restore the ListView position
        if (mListViewScrollPos != null) {
            listView.onRestoreInstanceState(mListViewScrollPos);
        }
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                DeviceConnection deviceConnection = new DeviceConnection(getActivity());
                //determine whether the first item is *fully* visible
                boolean enableSwipe = false;
                if(mListView!= null && mListView.getChildCount() > 0){
                    // check if the first item of the list is visible
                    boolean firstItemVisible = mListView.getFirstVisiblePosition() == 0;
                    // check if the top of the first item is visible
                    boolean topOfFirstItemVisible = mListView.getChildAt(0).getTop() == 0;
                    // enabling or disabling the refresh layout
                    enableSwipe = firstItemVisible && topOfFirstItemVisible;
                }
                //we only refresh when the user is at the top of the list and the Internet is connected and we have the last user's location
                if (enableSwipe && deviceConnection.checkInternetConnected() && !GlobalState.USER_LAT.equals("") && !GlobalState.USER_LNG.equals("")) {
                    swipeLayout.setEnabled(true);
                }
                else {
                    swipeLayout.setEnabled(false);
                }
            }
        });
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        return rootView;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGaTracker = ((BikeShopsDetector)  getActivity().getApplication()).getTracker(
                BikeShopsDetector.TrackerName.APP_TRACKER);
        setHasOptionsMenu(true); //tells the system that we have button(s) in the menu

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.shopsfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case (R.id.home): {
                    mOutBundle.getInt(SELECTED_KEY,100);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        // Save the ListView position (for the case when the user presses the back button from the action bar
        mListViewScrollPos = mListView.onSaveInstanceState();
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        //onResume is called before the loader, so it's safe (read "doesn't affect the logic") to assign values to the booleans here
        mIsListRefreshed = false;
        mIsSpeedChanged = false;
        //register the sync receiver
        mSyncFilter = new IntentFilter();
        mSyncFilter.addAction(Constants.SYNC_BUNDLE_STATUS_ACTION);
        getActivity().registerReceiver(mSyncReceiver,mSyncFilter);
        //instantiate the DeviceConnection class
        DeviceConnection deviceConnection = new DeviceConnection(getActivity());
        //refresh if the range has changed, we have an internet connection and the user's last location
        if (GlobalState.FRAGMENT_RANGE != null && !GlobalState.FRAGMENT_RANGE.equals(Utility.getPreferredRangeImperial(getActivity()))
                && deviceConnection.checkInternetConnected() && !GlobalState.USER_LAT.equals("") && !GlobalState.USER_LNG.equals("") ) {
           // Log.i(LOG_TAG,GlobalState.USER_LAT + " / " + GlobalState.USER_LNG);
            updateShopList(getActivity());
            mIsListRefreshed = true;
        }
        //we only restart the loader if the refresh caused by the change of range hasn't been performed. If it has, we already have an updated list
        if (GlobalState.FRAGMENT_SPEED != null && !GlobalState.FRAGMENT_SPEED.equals(Utility.getPreferredSpeed(getActivity())) && !mIsListRefreshed) {
            mIsSpeedChanged = true;
            getLoaderManager().restartLoader(SHOPS_LOADER_ID,null,this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister the receiver
        getActivity().unregisterReceiver(mSyncReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        //used to determine if a refresh of the displayed speed or range is necessary
        GlobalState.FRAGMENT_RANGE = Utility.getPreferredRangeImperial(getActivity());
        GlobalState.FRAGMENT_SPEED = Utility.getPreferredSpeed(getActivity());
    }

    public void updateShopList(Context context) {
        String[] coordinates = new String[2];
        coordinates[0] = GlobalState.USER_LAT;
        coordinates[1] = GlobalState.USER_LNG;
       // Log.i(LOG_TAG,"Lat/lng in updateShopList - " + coordinates[0] + "/" + coordinates[1]);
        SyncAdapter.syncImmediately(context);
    }

    //loaders are initialised in onActivityCreated because their lifecycle is bound to the activity, not the fragment
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        //initiate loader to populate data in the Shops fragment
        getLoaderManager().initLoader(SHOPS_LOADER_ID,null,this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
        mShopsAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mListView.smoothScrollToPosition(mPosition);
        }
        if (swipeLayout.isRefreshing()) {
            swipeLayout.setRefreshing(false);
        }
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        mShopsAdapter.swapCursor(null);
    }
    //ShopsContract.ShopsEntry.CONTENT_URI
    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new android.content.CursorLoader(
                getActivity(),
                ShopsContract.ShopsEntry.CONTENT_URI,
                SHOPS_COLUMNS,
                null,
                null,
                ShopsContract.ShopsEntry.SORT_ORDER
        );
    }

    @Override
    public void onRefresh() {
         //   Log.i(LOG_TAG, "In onRefresh()");
            swipeLayout.setColorSchemeResources(R.color.waldo_light_blue);
            ShopsFragment shopsFragment = new ShopsFragment();
            shopsFragment.updateShopList(getActivity());
        }

    private BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle syncExtras = intent.getExtras();
            String syncStatus = "";
            String syncResult = "";
            if (syncExtras != null) {
                syncStatus = syncExtras.getString(Constants.SYNC_BUNDLE_STATUS_KEY,"");
                syncResult = syncExtras.getString(Constants.SYNC_BUNDLE_RESULT_KEY,"");
                if (!syncStatus.equals("")) {
                   // Log.i(LOG_TAG,"Sync status/result : " + syncStatus + "/" + syncResult);
                    if (syncStatus.equals(Constants.SYNC_BUNDLE_STATUS_STOPPED)) {
                        if (swipeLayout.isRefreshing()) swipeLayout.setRefreshing(false);//remove the refresh circle if it is present
                        if (syncResult.equals(Constants.SYNC_BUNDLE_STATUS_ZERO)) Toast.makeText(getActivity().getApplicationContext(),
                                getResources().getString(R.string.no_shops), Toast.LENGTH_SHORT).show();//inform the user
                        else
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getResources().getString(R.string.api_error), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

}

