package socialmedia;

import android.os.AsyncTask;
import android.util.Log;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by nmihai on 23.01.2015.
 */
public class TwitterAsyncTask extends AsyncTask<String[],Void,Void> {
    private static final String LOG_TAG = TwitterAsyncTask.class.getSimpleName();
    @Override
    protected Void doInBackground(String[]... params) {
        String screenName = "startupkitio";
        //TODO: Replace Twitter keys with those of Waldo
        final String WALDO_CONSUMER_KEY = "amGg88FlHOkWJwFdxcZuwNCNd";
        final String WALDO_CONSUMER_SECRET_KEY = "npKv9jr06nWfCCNNbixntIxeZmcuLgQSlebZOYJ82jytuLWHrl";
        final String USER_TOKEN = params[0].toString();
        final String USER_SECRET_TOKEN = params[0].toString();
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(WALDO_CONSUMER_KEY)
                .setOAuthConsumerSecret(WALDO_CONSUMER_SECRET_KEY)
                .setOAuthAccessToken(USER_TOKEN)
                .setOAuthAccessTokenSecret(USER_SECRET_TOKEN);
        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();

        try {
            twitter.createFriendship(screenName);
            Log.i(LOG_TAG,"Followed you!");
        } catch (TwitterException e) {

            e.printStackTrace();
        }
        return null;
    }
}
