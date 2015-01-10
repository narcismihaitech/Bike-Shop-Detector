package waldo.bike.form;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import Utilities.Constants;

/**
 * Created by Narcis11 on 10.01.2015.
 */
public class PostForm extends AsyncTask<String, Void, String> {

    private static final String LOG_TAG = PostForm.class.getSimpleName();
    @Override
    protected String doInBackground(String... params) {
        String jsonString = createJSONObject(params);
        String url = "https://maps.googleapis.com/maps/api/place/add/json?key=" + Constants.API_KEY;
        HttpPost httpPost = new HttpPost(url);
        HttpClient httpClient = new DefaultHttpClient();
        try {
            StringEntity stringEntity = new StringEntity(jsonString, HTTP.UTF_8);
            httpPost.setEntity(stringEntity);
            HttpResponse response = httpClient.execute(httpPost);
            Log.i(LOG_TAG,"Response is " + response.toString());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    protected String createJSONObject (String[] parameters) {
        JSONObject fullJson = new JSONObject();
        JSONObject locationJson = new JSONObject();
        String lat = "lat";
        String lng = "lng";
        String location = "location";
        String name = "name";
        String phone_number = "phone_number";
        String address = "address";
        String types = "types";
        String website = "website";
        try {
            fullJson.put(name,parameters[2]);
            locationJson.put(lat, parameters[0]);
            locationJson.put(lng,parameters[1]);
            fullJson.put(location,locationJson);
            if (parameters[3] != null) {
                fullJson.put(phone_number,parameters[3]);
            }
            fullJson.put(address,parameters[4]);
            fullJson.put(types,parameters[5]);
            if (parameters[6] != null) {
                fullJson.put(website,parameters[6]);
            }
            Log.i(LOG_TAG,"JSON String is: " + fullJson.toString(2));
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
        catch(NullPointerException e) {
            e.printStackTrace();
        }
        return fullJson.toString();
    }
}