package waldo.bike.waldo;

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import Places.FetchGooglePlaces;
import Utilities.Constants;
import Utilities.GlobalState;
import Utilities.Utility;
import data.ShopsContract;
import data.ShopsProvider;
import sync.SyncAdapter;

/**
 * Created by Narcis11 on 20.12.2014.
 */
public class ShopsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    public SimpleCursorAdapter mShopsAdapter;
    private String mRadius;
    private static final String LOG_TAG = ShopsFragment.class.getSimpleName();
    private String mShopLatitude = "";
    private String mShopLongitude = "";
    private String mShopName = "";
    private String mFormattedDuration = "";
    private String mFormattedDistance = "";
    private String mPreferredUnit = "";
    private boolean mIsListRefreshed;
    private Double mNewSpeedDistanceToShop;
    private static final int SHOPS_LOADER_ID = 0;//loader identifier
    ListView mListView;
    private boolean mIsSpeedChanged;
    public static final String[] SHOPS_COLUMNS = {
            ShopsContract.ShopsEntry.TABLE_NAME + "." + ShopsContract.ShopsEntry._ID,
            ShopsContract.ShopsEntry.COLUMN_SHOP_NAME,
            ShopsContract.ShopsEntry.COLUMN_SHOP_ADDRESS,
            ShopsContract.ShopsEntry.COLUMN_IS_OPEN,
            ShopsContract.ShopsEntry.COLUMN_DISTANCE_TO_USER,
            ShopsContract.ShopsEntry.COLUMN_DISTANCE_DURATION,
            ShopsContract.ShopsEntry.COLUMN_SHOP_LATITUDE,
            ShopsContract.ShopsEntry.COLUMN_SHOP_LONGITUDE
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

    public ShopsFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        
        mShopsAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.list_item_shops,
                null,
                new String[] {
                        ShopsContract.ShopsEntry.COLUMN_SHOP_NAME,
                        ShopsContract.ShopsEntry.COLUMN_SHOP_ADDRESS,
                        ShopsContract.ShopsEntry.COLUMN_IS_OPEN,
                        ShopsContract.ShopsEntry.COLUMN_DISTANCE_TO_USER,
                        ShopsContract.ShopsEntry.COLUMN_DISTANCE_DURATION
                },
                new int[] {
                        R.id.list_item_shopname_textview,
                        R.id.list_item_shopaddress_textview,
                        R.id.list_item_shopisopen_textview,
                        R.id.list_item_distance_textview,
                        R.id.list_item_duration_textview
                },
                0
        );
        //we need to format the data from the database
        mShopsAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                switch (columnIndex) {
                    case COL_SHOP_NAME:
                        ((TextView) view).setText(cursor.getString(COL_SHOP_NAME));
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
                            return true;
                        }
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
                        return  true;
                }
                return false;
            }
        });
    //    Log.i(LOG_TAG,"Size of mShopsAdapter = " + mShopsAdapter.getCount());

        // Get a reference to the ListView, and attach this adapter to it.
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_shops);
        mListView = listView;
        listView.setAdapter(mShopsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mShopsAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    mShopName = cursor.getString(COL_SHOP_NAME);
                    mShopLatitude = cursor.getString(COL_SHOP_LATITUDE);
                    mShopLongitude = cursor.getString(COL_SHOP_LONGITUDE);
                    Intent openMap = new Intent(getActivity().getApplicationContext(),MapsActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.BUNDLE_SHOP_LAT,mShopLatitude);
                    bundle.putString(Constants.BUNDLE_SHOP_LNG,mShopLongitude);
                    bundle.putString(Constants.BUNDLE_SHOP_NAME,mShopName);
                    bundle.putString(Constants.BUNDLE_FRAGMENT,Constants.CALLED_FROM_FRAGMENT);
                    openMap.putExtras(bundle);
                    startActivity(openMap);
                }
            }
        });
        return rootView;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); //tells the system that we have button(s) in the menu
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.shopsfragment,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateShopList();
          //  GlobalState.GLOBAL_ADAPTER = mShopsAdapter;
          //  SyncAdapter.syncImmediately(getActivity());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public void onStart() {
        super.onStart(); 
       // Log.i(LOG_TAG, "In fragment onStart()");
       // updateShopList();
    }

    @Override
    public void onResume() {
        super.onResume();
        //onResume is called before the loader, so it's safe (read "doesn't affect the logic") to assign values to the booleans here
        mIsListRefreshed = false;
        mIsSpeedChanged = false;
        if (GlobalState.FRAGMENT_RANGE != null && !GlobalState.FRAGMENT_RANGE.equals(Utility.getPreferredRangeImperial(getActivity()))) {
            Log.i(LOG_TAG,"****UPDATED SHOP LIST****");
            updateShopList();
            mIsListRefreshed = true;
        }
        //we only restart the loader if the refresh caused by the change of range hasn't been performed. If it has, we already have an updated list
        if (GlobalState.FRAGMENT_SPEED != null && !GlobalState.FRAGMENT_SPEED.equals(Utility.getPreferredSpeed(getActivity())) && !mIsListRefreshed) {
            mIsSpeedChanged = true;
            getLoaderManager().restartLoader(SHOPS_LOADER_ID,null,this);
            //TODO: restarting the Loader doesn't mean that the speed is refreshed. Find another way to refresh the speed.
           /* mShopsAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                    Log.i(LOG_TAG,"columnIndex is: " + columnIndex);
                    switch (columnIndex){
                        case COL_DISTANCE_TO_USER:
                            //    Log.i(LOG_TAG,"Shopname / distance: " + cursor.getString(COL_SHOP_NAME) + " / " + cursor.getString(COL_DISTANCE_TO_USER));
                            mPreferredUnit = Utility.getPreferredUnit(getActivity());
                            if (mPreferredUnit.equals(getResources().getString(R.string.unit_array_metric))) {
                                mFormattedDistance = Utility.formatDistanceMetric(cursor.getString(COL_DISTANCE_TO_USER));
                            }
                            else {
                                mFormattedDistance = Utility.formatDistanceImperial(cursor.getString(COL_DISTANCE_TO_USER));
                            }
                            Log.i(LOG_TAG,"Classic mFormattedDistance/columnIndex = " + mFormattedDistance + "/" + columnIndex);
                            ((TextView) view).setText(mFormattedDistance);
                            return true;
                        case COL_DISTANCE_DURATION:
                            Log.i(LOG_TAG,"****NEW SET VIEW BINDER VALUES***");
                            Log.i(LOG_TAG,"columnIndex is: " + columnIndex);
                            int distanceToShop = Integer.valueOf(cursor.getString(COL_DISTANCE_TO_USER));
                            Log.i(LOG_TAG,"distanceToShop: " + distanceToShop);
                            mNewSpeedDistanceToShop = Utility.calculateDistanceDuration(distanceToShop,getActivity());
                            Log.i(LOG_TAG,"mNewSpeedDistanceToShop: " + mNewSpeedDistanceToShop);
                            mFormattedDuration = Utility.formatDistanceDuration(String.valueOf(mNewSpeedDistanceToShop));
                            Log.i(LOG_TAG,"New mFormattedDuration = " + mFormattedDuration);
                            ((TextView) view).setText(mFormattedDuration);
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
                            return  true;
                    }
                    return false;
                }
            });
           // */
        }
        //TODO: test if the sync succeeds even if the phone is rotated while syncing. Check if onLoadFinished is called. The commented code below might prove useful.
/*        LoaderManager lm = getLoaderManager();
        if (lm.getLoader(SHOPS_LOADER_ID) != null) {
            Log.i(LOG_TAG,"******init loader in onResume()*****");
            lm.initLoader(SHOPS_LOADER_ID, null, this);
        }*/
    }

    @Override
    public void onStop() {
        super.onStop();
        //used to determine of a refresh of the displayed speed or range is necessary
        GlobalState.FRAGMENT_RANGE = Utility.getPreferredRangeImperial(getActivity());
        GlobalState.FRAGMENT_SPEED = Utility.getPreferredSpeed(getActivity());
    }

    public void updateShopList() {
        String[] coordinates = new String[2];
        coordinates[0] = GlobalState.USER_LAT;
        coordinates[1] = GlobalState.USER_LNG;
        Log.i(LOG_TAG,"Lat/lng in updateShopList - " + coordinates[0] + "/" + coordinates[1]);
      //  new FetchGooglePlaces(getActivity()).execute(coordinates);
        SyncAdapter.syncImmediately(getActivity());
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
}

