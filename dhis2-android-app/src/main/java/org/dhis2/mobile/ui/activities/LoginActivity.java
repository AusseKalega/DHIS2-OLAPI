/*
 * Copyright (c) 2014, Araz Abishov
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.dhis2.mobile.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aussekalega.openlocator.OpenLocatorAppCompatActivity;
import com.aussekalega.openlocator.OpenLocatorRequest;
import com.aussekalega.openlocator.OpenLocatorRequestBuilder;
import com.google.android.gms.location.LocationRequest;

import org.dhis2.mobile.R;
import org.dhis2.mobile.WorkService;
import org.dhis2.mobile.network.HTTPClient;
import org.dhis2.mobile.network.NetworkUtils;
import org.dhis2.mobile.network.Response;
import org.dhis2.mobile.processors.OrgUnitLocationProcessor;
import org.dhis2.mobile.processors.UserRoleProcessor;
import org.dhis2.mobile.utils.PrefUtils;
import org.dhis2.mobile.utils.ToastManager;
import org.dhis2.mobile.utils.ViewUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;

public class LoginActivity extends OpenLocatorAppCompatActivity /*implements OnLocationUpdatedListener, OnActivityUpdatedListener, OnGeofencingTransitionListener*/ {
    public static final String TAG = LoginActivity.class.getSimpleName();
    public static final String USERNAME = "username";
    public static final String SERVER = "server";
    public static final String CREDENTIALS = "creds";
    public static final String IS_FIRST_PULL = "isfirstpull";
    private static double LAT = 0.0;
    private static double LNG = 0.0;

    // Locationg settings
    // private LocationGooglePlayServicesProvider provider;
    // private static final int LOCATION_PERMISSION_ID = 1001;

    private Button mLoginButton;
    private EditText mUsername;
    private EditText mPassword;
    private ImageView mDhis2Logo;

    // Disabled serverUrl EditText in order to allow
    // developers to build app with custom server address
    private EditText mServerUrl;
    private ProgressBar mProgressBar;
    private LoginActivity mLoginActivity;

    // BroadcastReceiver which aim is to listen
    // for network response on login post request
    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            int code = extras.getInt(Response.CODE);
           // String userRole = extras.getString(UserRoleProcessor.USER_ROLE);
            Boolean isFirstPull = extras.getBoolean(LoginActivity.IS_FIRST_PULL);

            // If response code is 200, then MenuActivity is started
            // If not, user is notified with error message
            if (!HTTPClient.isError(code)) {
                if(PrefUtils.getServerVersion(context)!=null && !PrefUtils.getServerVersion(context).equals("")) {
                    // Prepare Intent and start service
                    if(isFirstPull == null || !isFirstPull) {
                        intent = new Intent(mLoginActivity, WorkService.class);
                        intent.putExtra(WorkService.METHOD, WorkService.METHOD_FIRST_PULL_DATASETS);
                        mLoginActivity.startService(intent);
                    }else {
                        finish();
                        SharedPreferences sharedPreferences = getSharedPreferences(UserRoleProcessor.SHARED_PREFS, MODE_PRIVATE);
                        String userRole = sharedPreferences.getString(UserRoleProcessor.USER_ROLE, "");

                        SharedPreferences sharedPreferences2 = getSharedPreferences(OrgUnitLocationProcessor.SHARED_PREFS, MODE_PRIVATE);
                        String coordinates = sharedPreferences2.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION, "");
                        String rad = sharedPreferences2.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION_RADIUS, "");
                        String starttime = sharedPreferences2.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION_STARTTIME, "");
                        String stoptime = sharedPreferences2.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION_STOPTIME, "");

                        if(!userRole.equals("") || userRole != null){

                            if(userRole.equalsIgnoreCase("Data entry clerk")){
                                double requiredRadius = Double.parseDouble(rad);
                                String [] coord = coordinates.split("[,]");
                                Double lat = Double.parseDouble(coord[1].substring(0, coord[1].length()-1));
                                Double lng = Double.parseDouble(coord[0].substring(1));

                                Log.i("TESTING SHAREDPERF", userRole + ", " + lat + " : " + lng + "," + "Radius: " + rad);
                                // check time and location restriction

                                DateTime dt = new DateTime();
                                int hourOfDay = dt.get(DateTimeFieldType.hourOfDay());
                                hourOfDay = hourOfDay + 3;
                                int start_time = Integer.parseInt(starttime);
                                int stop_time = Integer.parseInt(stoptime);

                                if(hourOfDay < start_time || hourOfDay > stop_time){
                                    showMessage("System access DENIED at this time. Please try again between " +
                                            start_time +":00 and "+ stop_time + ":00 (EAT). Thank you!");
                                    finish();
                                }

                                if (LAT != 0.0 && LNG != 0.0){
                                    // calculate radius
                                    Location locRecorded = new Location("LocationA");
                                    locRecorded.setLatitude(lat);
                                    locRecorded.setLongitude(lng);

                                    Location myLocation = new Location("LocationB");
                                    myLocation.setLatitude(LAT);
                                    myLocation.setLongitude(LNG);

                                    double radius = myLocation.distanceTo(locRecorded);
                                    Log.i("CALCULATED RADIUS", String.valueOf(radius));
                                    if(radius > requiredRadius){

                                        // logout and clear shared preferences
                                        showMessage("Location fence exceeded. Please contact administrator");
                                        sharedPreferences.edit().clear().apply();
                                        sharedPreferences2.edit().clear().apply();
                                        // start service in order to remove data
                                        Intent removeDataIntent = new Intent(LoginActivity.this, WorkService.class);
                                        removeDataIntent.putExtra(WorkService.METHOD, WorkService.METHOD_REMOVE_ALL_DATA);
                                        startService(removeDataIntent);
                                        // start LoginActivity
                                        Intent startLoginActivity = new Intent(LoginActivity.this, LauncherActivity.class);
                                        startActivity(startLoginActivity);
                                        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
                                        finish();
//                                        Intent intent1 = new Intent(LoginActivity.this, LauncherActivity.class);
//                                        startActivity(intent1);
//                                        finish();

                                    } else {
                                        //showMessage(String.valueOf(radius));
                                        finish();
                                        Intent menuActivity = new Intent(LoginActivity.this, MenuActivity.class);
                                        // menuActivity.putExtra("lat", LAT);
                                        // menuActivity.putExtra("lng", LNG);
                                        startActivity(menuActivity);
                                        overridePendingTransition(R.anim.activity_open_enter,
                                                R.anim.activity_open_exit);
                                    }


                                } else {
                                    // user role = data entry clerk but lat and lng == 0
                                    showMessage("Location is null, please check network connection and try again: FROM LOGIN");
                                    sharedPreferences.edit().clear().apply();
                                    sharedPreferences2.edit().clear().apply();
                                    // start service in order to remove data
                                    Intent removeDataIntent = new Intent(LoginActivity.this, WorkService.class);
                                    removeDataIntent.putExtra(WorkService.METHOD, WorkService.METHOD_REMOVE_ALL_DATA);
                                    startService(removeDataIntent);
                                    // start LoginActivity
                                    Intent startLoginActivity = new Intent(LoginActivity.this, LauncherActivity.class);
                                    startActivity(startLoginActivity);
                                    overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
                                    finish();

                               }

                            } else {

                                // user role is not "data clerk entry" == just login
                                finish();
                                Intent menuActivity = new Intent(LoginActivity.this, MenuActivity.class);
                                startActivity(menuActivity);
                                overridePendingTransition(R.anim.activity_open_enter,
                                        R.anim.activity_open_exit);

                               // showMessage("Incorrect Role");
                            }
                        }
                    }
                }else{
                    hideProgress();
                    String message = context.getString(R.string.server_error);
                    showMessage(message);
                }
            } else {
                hideProgress();
                String message = HTTPClient.getErrorMessage(LoginActivity.this, code);
                showMessage(message);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mLoginActivity = this;
        mDhis2Logo = (ImageView) findViewById(R.id.dhis2_logo);
        mLoginButton = (Button) findViewById(R.id.login_button);

        mServerUrl = (EditText) findViewById(R.id.server_url);
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);


        // OLAPI Library
        LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(5000);
        OpenLocatorRequest openLocatorRequest = new OpenLocatorRequestBuilder()
                .setLocationRequest(locationRequest)
                .setFallBackToLastLocationTime(3000)
                .build();
        requestLocationUpdates(openLocatorRequest);

        // textwatcher is responsible for watching
        // after changes in all fields
        final TextWatcher textWatcher = new TextWatcher() {

            @Override
            public void afterTextChanged(Editable edit) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                checkEditTextFields();
            }
        };

        mServerUrl.addTextChangedListener(textWatcher);
        mUsername.addTextChangedListener(textWatcher);
        mPassword.addTextChangedListener(textWatcher);

        // Call method in order to check the fields
        // and change state of login button
        checkEditTextFields();

        mLoginButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                loginUser();
            }
        });

        // Restoring state of activity from saved bundle
        if (savedInstanceState != null) {
            boolean loginInProcess = savedInstanceState.getBoolean(TAG, false);

            if (loginInProcess) {
                ViewUtils.hideAndDisableViews(mDhis2Logo, mServerUrl, mUsername, mPassword, mLoginButton);
                //ViewUtils.hideAndDisableViews(mDhis2Logo, mUsername, mPassword, mLoginButton);
                showProgress();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //showLast();
        // Registering BroadcastReceiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(TAG));
        // OLAPI Library
        LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(5000);
        OpenLocatorRequest openLocatorRequest = new OpenLocatorRequestBuilder()
                .setLocationRequest(locationRequest)
                .setFallBackToLastLocationTime(3000)
                .build();
        requestLocationUpdates(openLocatorRequest);
    }

    @Override
    public void onPause() {

        // Unregistering BroadcastReceiver in
        // onPause() in order to prevent leaks
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);

        // OLAPI Library
        LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(5000);
        OpenLocatorRequest openLocatorRequest = new OpenLocatorRequestBuilder()
                .setLocationRequest(locationRequest)
                .setFallBackToLastLocationTime(3000)
                .build();
        requestLocationUpdates(openLocatorRequest);

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Saving state of activity
        if (mProgressBar != null) {
            outState.putBoolean(TAG, mProgressBar.isShown());
        }
        super.onSaveInstanceState(outState);
    }

    // Activates *login button*,
    // if all necessary fields are full
    private void checkEditTextFields() {
        String tempUrl = mServerUrl.getText().toString();
        //Server address will be retrieved from .xml resources
        //String tempUrl = getString(R.string.default_server_url);
        String tempUsername = mUsername.getText().toString();
        String tempPassword = mPassword.getText().toString();

        if (tempUrl.equals("") || tempUsername.equals("") || tempPassword.equals("")) {
            mLoginButton.setEnabled(false);
        } else {
            mLoginButton.setEnabled(true);

        }
    }

    // loginUser() is called when user clicks *LoginButton*
    private void loginUser() {

        String tmpServer = mServerUrl.getText().toString();
        //Server address will be retrieved from .xml resources
        //String tmpServer = getString(R.string.default_server_url);

        String user = mUsername.getText().toString();
        String pass = mPassword.getText().toString();
        String pair = String.format("%s:%s", user, pass);


        // Check internet connectivity (calls NetworkUtils  class)
        if (NetworkUtils.checkConnection(LoginActivity.this)) {
            showProgress();

            String server = tmpServer + (tmpServer.endsWith("/") ? "" : "/");
            String creds = Base64.encodeToString(pair.getBytes(), Base64.NO_WRAP);

            // Preparing data to be sent to WorkService
            Intent intent = new Intent(LoginActivity.this, WorkService.class);
            intent.putExtra(WorkService.METHOD, WorkService.METHOD_LOGIN_USER);
            intent.putExtra(SERVER, server);
            intent.putExtra(USERNAME, user);
            intent.putExtra(CREDENTIALS, creds);

            // Starting WorkService
            startService(intent);
        } else {
            showMessage(getString(R.string.check_connection));
        }
    }

    private void showMessage(String message) {
        ToastManager.makeToast(this, message, Toast.LENGTH_LONG).show();
    }

    private void showProgress() {
        ViewUtils.perfomOutAnimation(this, R.anim.out_up, true,
                mDhis2Logo, mServerUrl, mUsername, mPassword, mLoginButton);
        ViewUtils.enableViews(mProgressBar);
    }

    private void hideProgress() {
        ViewUtils.perfomInAnimation(this, R.anim.in_down,
                mDhis2Logo, mServerUrl, mUsername, mPassword, mLoginButton);
        ViewUtils.hideAndDisableViews(mProgressBar);
    }

    /**
     * Location settings methods starts here
     */


    // OLAPI Library Test
    @Override
    public void onLocationPermissionGranted() {
        showToast("Location permission granted");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationPermissionDenied() {
        showToast("Location permission denied");
    }

    @Override
    public void onLocationReceived(Location location) {
        LAT = location.getLatitude();
        LNG = location.getLongitude();
        Log.i("COORDINATES", "Lat:" + location.getLatitude() + ", Lng:" + location.getLongitude());
        //showToast(location.getLatitude() + "," + location.getLongitude());
    }

    @Override
    public void onLocationProviderEnabled() {
        showToast("Location services are now ON");
    }

    @Override
    public void onLocationProviderDisabled() {
        showToast("Location services are still Off");
    }


}
