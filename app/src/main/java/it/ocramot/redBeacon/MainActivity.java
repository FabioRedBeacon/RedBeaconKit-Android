package it.ocramot.redBeacon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.greysonparrelli.permiso.Permiso;

import java.util.Locale;

import it.ocramot.rbk.IRBKUpdate;
import it.ocramot.rbk.model.RBKBeacon;

/**
 * Main Activity opened when the application is launched.
 * Implements the IRBKUpdate interface, so it receives notifications from the Red Beacon module
 * when a new beacon is found, and it shows them on the screen.
 * @author ocramot
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity implements IRBKUpdate {

    protected static final String TAG = "MainActivity";

    private final static int REQUEST_CHECK_SETTINGS = 1;
    // Layout elements

    private ViewGroup mRootView;
    private TextView mUUIDView;
    private TextView mIDView;
    private TextView mMajorView;
    private TextView mMinorView;
    private TextView mTypeView;
    private TextView mDistanceView;

    /**
     * The current beacon found by the module.
     */
    private RBKBeacon mCurrentBeacon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Sets this activity to receive permissions request.
        Permiso.getInstance().setActivity(this);

        mRootView = findViewById(R.id.root);
        mUUIDView = findViewById(R.id.text_uuid);
        mIDView = findViewById(R.id.text_id);
        mMajorView = findViewById(R.id.text_major);
        mMinorView = findViewById(R.id.text_minor);
        mTypeView = findViewById(R.id.text_type);
        mDistanceView = findViewById(R.id.text_distance);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                 @Override
                 public void onPermissionResult(Permiso.ResultSet resultSet) {
                     if (resultSet.isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                         if(BuildConfig.LOG_DEBUG) Log.d(TAG, "Coarse location permission granted");
                         enableGPS();
                     } else {
                         if(BuildConfig.LOG_DEBUG) Log.w(TAG, "Coarse location permission NOT granted - since location access has not been granted, this app will not be able to discover beacons when in the background.");
                         final AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                         builder.setTitle(R.string.limited_permission_title);
                         builder.setMessage(R.string.limited_permission_message);
                         builder.setPositiveButton(android.R.string.ok, null);
                         builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                             @Override
                             public void onDismiss(DialogInterface dialog) {
                             }

                         });
                         builder.show();
                     }
                 }

                 @Override
                 public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                     Permiso.getInstance().showRationaleInDialog(getString(R.string.permission_request_title), getString(R.string.permission_request_message), null, callback);
//                    callback.onRationaleProvided();
                 }
            },
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        enableBluetooth();
        ((RedBeaconApplication) getApplicationContext()).startCommunications();
    }

    /**
     * Automatically enables the Bluetooth if it is available and it is not already on.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void enableBluetooth(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            if(BuildConfig.LOG_DEBUG) Log.i(TAG, "Enabling Bluetooth");
            Snackbar snackbar = Snackbar.make(mRootView, R.string.notice_enable_bluetooth, Snackbar.LENGTH_SHORT);
            snackbar.show();
            bluetoothAdapter.enable();
        }
        if(Build.VERSION.SDK_INT < 18) {
            if(BuildConfig.LOG_DEBUG) Log.w(TAG, "Bluetooth LE not supported prior to API 18. API level is " + Build.VERSION.SDK_INT);
            showFinalAlertDialog(getString(R.string.warn_api_18_title), getString(R.string.warn_api_18_message));
        } else if(!getApplicationContext().getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            if(BuildConfig.LOG_DEBUG) Log.w(TAG, "This device does not support bluetooth LE.");
            showFinalAlertDialog(getString(R.string.warn_bt_not_supported_title), getString(R.string.warn_bt_not_supported_message));
        }
    }

    private void enableGPS(){
        LocationRequest lr1 = LocationRequest.create();
        lr1.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationRequest lr2 = LocationRequest.create();
        lr2.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(lr1)
                .addLocationRequest(lr2);
        builder.setNeedBle(true);

        Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    task.getResult(ApiException.class);
                    // If no exception is thrown, all location settings are satisfied.
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        MainActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    /**
     * Show a dialog before closing the application.
     * @param title the title to display.
     * @param message the message to diplay.
     */
    private void showFinalAlertDialog(String title, String message){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
            finish();
            System.exit(0);
            }
        });
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((RedBeaconApplication) this.getApplicationContext()).setMonitoringActivity(this);
        ((RedBeaconApplication) this.getApplicationContext()).setBackgroundMode(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((RedBeaconApplication) this.getApplicationContext()).setMonitoringActivity(null);
        ((RedBeaconApplication) this.getApplicationContext()).setBackgroundMode(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((RedBeaconApplication) this.getApplicationContext()).stopCommunications();
    }

    @Override
    public void onBeaconImmediate(RBKBeacon beacon, double distance) {
        mCurrentBeacon = beacon;
        checkBeacon("Immediate", distance);
    }

    @Override
    public void onBeaconNear(RBKBeacon beacon, double distance) {
        mCurrentBeacon = beacon;
        checkBeacon("Near", distance);
    }

    @Override
    public void onBeaconFar(RBKBeacon beacon, double distance) {
        mCurrentBeacon = beacon;
        checkBeacon("Far", distance);
    }

    @Override
    public void onBeaconUnknown(RBKBeacon beacon, double distance) {
        mCurrentBeacon = beacon;
        checkBeacon("Unknown", distance);
    }

    @Override
    public View getRootView() {
        return mRootView;
    }

    /**
     * Receives information about a beacon and shows them in the GUI.
     * @param type the beacon proximity (Unknown, Immediate, Near, Far).
     * @param distance the beacon distance in metres.
     */
    private void checkBeacon(final String type, final double distance){

        if(BuildConfig.LOG_DEBUG) Log.i(TAG, "Found new Beacon: " + mCurrentBeacon.getDescription() + ", " +mCurrentBeacon.getBeaconUUID() + ", " +mCurrentBeacon.getMajor() + ", " + mCurrentBeacon.getMinor() + ", " + type + ", " + distance);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mUUIDView.setText(mCurrentBeacon.getBeaconUUID().toString());
                mIDView.setText(mCurrentBeacon.getDescription());
                mMajorView.setText(mCurrentBeacon.getMajor());
                mMinorView.setText(mCurrentBeacon.getMinor());
                mTypeView.setText(type);
                mDistanceView.setText(String.format(Locale.getDefault(), "%.2f m", distance));
            }
        });
    }
}
