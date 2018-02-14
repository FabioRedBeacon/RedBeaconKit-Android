package it.ocramot.redBeacon;

import android.app.Application;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import it.ocramot.rbk.IRBKUpdate;
import it.ocramot.rbk.RedBeaconKit;
import it.ocramot.redBeacon.BuildConfig;
import it.ocramot.redBeacon.MainActivity;

/**
 * Entry point of the application. Declares and initialize an RBK module, and specifies the beacon layouts to monitor.
 * @author ocramot
 * @version 1.0
 */
public class RedBeaconApplication extends Application {

    private final static String TAG   = "RedBeaconApplication";

    private final static String BEACON_LAYOUT   = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    private final static String ESTIMOTE_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    /**
     * Your Secret Key for the Red Beacon services
     */
    private final static String SECRET_KEY      = "YOUR_SECRET_EKY";

    /**
     * Your App ID
     */
    private final static String APP_ID          = "YOUR_APP_ID";

    /**
     * Your User ID
     */
    private final static String USER_ID         = "testUser";

    private RedBeaconKit mRbk;

    @Override
    public void onCreate() {
        super.onCreate();

        List<String> layouts = new ArrayList<>();
        layouts.add(BEACON_LAYOUT);
        layouts.add(ESTIMOTE_LAYOUT);

        mRbk = new RedBeaconKit(this, SECRET_KEY, APP_ID, USER_ID, MainActivity.class, layouts);

        Log.d(TAG, "Red Beacon Kit initialized.");
    }

    /**
     * Method called by the Activity during the onResume or onPause calls
     * @param mode true if the Activity is calling onPause, false if the activity is calling onResume
     */
    public void setBackgroundMode(boolean mode) {
        if(mRbk != null){
            if(BuildConfig.LOG_DEBUG) Log.d(TAG, "Setting background mode: " + mode);
            mRbk.setBackgroundMode(mode);
        }
    }

	/**
     * Method called by the Activity during the onCreate call. Starts the Beacons monitoring.
     */
    public void startCommunications() {
        if(mRbk != null){
            if(BuildConfig.LOG_DEBUG) Log.d(TAG, "Starting communications.");
            mRbk.startCommunications();
        }
    }

	/**
     * Method called by the Activity during the onDestroy call. Stops the Beacons monitoring.
     */
    public void stopCommunications() {
        if(mRbk != null){
            if(BuildConfig.LOG_DEBUG) Log.d(TAG, "Stopping communications.");
            mRbk.stopCommunications();
        }
    }

    /**
     * Method called by the Activity during the onResume or onPause calls
     * @param activity the Activity itself, which has to be notifed by the RedBeacon module when a new beacon is found.
     */
    public void setMonitoringActivity(IRBKUpdate activity) {
        if(mRbk != null){
            if(BuildConfig.LOG_DEBUG) Log.d(TAG, "Setting monitoring activity: " + (activity!= null ? activity.getClass().getName() : "null"));
            mRbk.setMonitoringActivity(activity);
        }
    }
}
