package waldo.bike.form;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;

import Places.FetchPlacesAutocomplete;
import Utilities.Constants;
import Utilities.Utility;
import waldo.bike.bikeshops.R;

/**
 * Created by Narcis11 on 03.01.2015.
 */

public class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private ArrayList<String> resultList;
    private boolean mFirstSync = true;
    private long mLastSyncDate;
    public static final int SYNC_INTERVAL = 1500;
    private Context mContext;
    private int mViewId;
    private static final String LOG_TAG = PlacesAutoCompleteAdapter.class.getSimpleName();
    private boolean mIsTextFormatted = false;
    private int countGetView = 1;
    private static final int RESULT_LINES = 2;
    private static final String SEPARATOR_PROPERTY = "line.separator";
    private String mAddress = "";
    private String mLocale = "";
    String mResultText = "";
    public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mViewId = textViewResourceId;
        mContext = context;
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public String getItem(int index) {
        return resultList.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

            //Format the results from the search in add shop map
            TextView textView = new TextView(mContext);
            textView.setPadding(Utility.convertDpToPixels(mContext,3), Utility.convertDpToPixels(mContext,2),Utility.convertDpToPixels(mContext,3),Utility.convertDpToPixels(mContext,2));
            textView.setLines(RESULT_LINES);
            textView.setBackgroundColor(mContext.getResources().getColor(R.color.list_background));
            mResultText = resultList.get(position);
            mAddress = mResultText.substring(0, mResultText.indexOf(Constants.COMMA_SEPARATOR));
            mLocale = mResultText.substring(mResultText.indexOf(Constants.COMMA_SEPARATOR) + 2);
            textView.setText(mAddress + System.getProperty(SEPARATOR_PROPERTY) + mLocale);
            countGetView++;
            return textView;
    }


    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
             //   Log.i(LOG_TAG,"In getFilter");
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    //we only sync if there are at least three letters
                  //      Log.i(LOG_TAG, "Started syncing");
                        //we need to reinitialise it at every sync
                        countGetView = 1;
                        FetchPlacesAutocomplete fetchPlacesAutocomplete = new FetchPlacesAutocomplete(mContext);
                        resultList = fetchPlacesAutocomplete.autocomplete(constraint.toString());
                    filterResults.values = resultList;
                    filterResults.count = resultList.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                //    Log.i(LOG_TAG,"notified DataSetChanged");
                }
                else {
                 //   Log.i(LOG_TAG,"DataSetInvalidated()");
                    notifyDataSetInvalidated();
                }
            }};
        return filter;
    }
}
