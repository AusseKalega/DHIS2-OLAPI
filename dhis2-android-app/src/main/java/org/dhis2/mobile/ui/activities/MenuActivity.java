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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.aussekalega.openlocator.OpenLocatorRequest;
import com.aussekalega.openlocator.OpenLocatorRequestBuilder;
import com.google.android.gms.location.LocationRequest;

import org.dhis2.mobile.R;
import org.dhis2.mobile.WorkService;
import org.dhis2.mobile.processors.OrgUnitLocationProcessor;
import org.dhis2.mobile.processors.UserRoleProcessor;
import org.dhis2.mobile.ui.fragments.AboutUsFragment;
import org.dhis2.mobile.ui.fragments.AggregateReportFragment;
import org.dhis2.mobile.ui.fragments.MyProfileFragment;
import org.dhis2.mobile.ui.fragments.SyncLogFragment;
import org.dhis2.mobile.utils.ToastManager;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;

public class MenuActivity extends BaseActivity implements OnNavigationItemSelectedListener {
    private static final String STATE_TOOLBAR_TITLE = "state:toolbarTitle";

    // layout
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private static double LAT = 0.0;
    private static double LNG = 0.0;
    SharedPreferences sharedPreferences, sharedPreferences2;
    private String userRole = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
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

        sharedPreferences = getSharedPreferences(UserRoleProcessor.SHARED_PREFS, MODE_PRIVATE);
        userRole = sharedPreferences.getString(UserRoleProcessor.USER_ROLE, "");

        if(userRole.equalsIgnoreCase("Data entry clerk")) {

            // check time restrictions
            // checking time restrictions
            // Access is only granted between 8:00AM - 17:00PM EAT
            sharedPreferences2 = getSharedPreferences(OrgUnitLocationProcessor.SHARED_PREFS, MODE_PRIVATE);
            String starttime = sharedPreferences2.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION_STARTTIME, "");
            String stoptime = sharedPreferences2.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION_STOPTIME, "");

            DateTime dt = new DateTime();
            int hourOfDay = dt.get(DateTimeFieldType.hourOfDay());
            hourOfDay = hourOfDay + 3;
            if(starttime == null|| starttime.equalsIgnoreCase("")) {
                Log.i("TIME", "No time information");
            }else{
                int start_time = Integer.parseInt(starttime);
                int stop_time = Integer.parseInt(stoptime);

                if (hourOfDay < start_time || hourOfDay > stop_time) {
                    showMessage("System access DENIED at this time. Please try again between " +
                            start_time + ":00 and " + stop_time + ":00 (EAT). Thank you!");
                    finish();
                }
            }
//            sharedPreferences3 = getSharedPreferences(OrgUnitLocationProcessor.SHARED_PREFS, MODE_PRIVATE);
//            String coordinates = sharedPreferences3.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION, "");
//            String rad = sharedPreferences3.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION_RADIUS, "");
//            double requiredRadius = Double.parseDouble(rad);
//
//            String[] coord = coordinates.split("[,]");
//            Double lat = Double.parseDouble(coord[1].substring(0, coord[1].length() - 1));
//            Double lng = Double.parseDouble(coord[0].substring(1));
//            if (LAT != 0 && LNG != 0) {
//                // calculate radius
//                Location locRecorded = new Location("LocationA");
//                locRecorded.setLatitude(lat);
//                locRecorded.setLongitude(lng);
//
//                Location myLocation = new Location("LocationB");
//                myLocation.setLatitude(LAT);
//                myLocation.setLongitude(LNG);
//
//                double radius = myLocation.distanceTo(locRecorded);
//
//                Log.i("RADIUS FROM MENU", String.valueOf(radius));
//                if (radius > requiredRadius) {
//                    // logout and clear shared preferences
//                    showMessage("Location fence exceeded. Please contact administrator");
//                    logOut();
//                }
//            } else {
//                showMessage("Location is null, please check network connection and try again");
//                logOut();
//            }
        }

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        if (navigationView != null) {
            navigationView.inflateMenu(R.menu.menu_drawer);
            navigationView.setNavigationItemSelectedListener(this);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_menu);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleNavigationDrawer();
                }
            });
        }

        if (savedInstanceState == null) {
            onNavigationItemSelected(navigationView.getMenu()
                    .findItem(R.id.drawer_item_aggregate_report));
        } else if (savedInstanceState.containsKey(STATE_TOOLBAR_TITLE) && toolbar != null) {
            setTitle(savedInstanceState.getString(STATE_TOOLBAR_TITLE));
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.drawer_item_aggregate_report: {
                attachFragment(new AggregateReportFragment());
                break;
            }
            case R.id.drawer_item_profile: {
                attachFragment(new MyProfileFragment());
                break;
            }
            case R.id.drawer_item_about: {
                attachFragment(new AboutUsFragment());

                break;
            }
            case R.id.drawer_item_sync_logs: {
                attachFragment(new SyncLogFragment());
                break;
            }
            case R.id.drawer_item_logout: {
                new AlertDialog.Builder(this)
                        .setTitle(getApplicationContext().getString(R.string.log_out))
                        .setMessage(R.string.dialog_content_logout_confirmation)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                logOut();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).create().show();
                break;
            }
        }

        setTitle(item.getTitle());
        navigationView.setCheckedItem(R.id.drawer_item_aggregate_report);
        drawerLayout.closeDrawers();

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (toolbar != null) {
            outState.putString(STATE_TOOLBAR_TITLE, toolbar.getTitle().toString());
        }

        super.onSaveInstanceState(outState);
        outState.clear();
    }

    protected void attachFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    protected void toggleNavigationDrawer() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            drawerLayout.openDrawer(navigationView);
        }
    }

    private void logOut() {
        // start service in order to remove data
        Intent removeDataIntent = new Intent(MenuActivity.this, WorkService.class);
        removeDataIntent.putExtra(WorkService.METHOD, WorkService.METHOD_REMOVE_ALL_DATA);
        startService(removeDataIntent);
        if(userRole.equalsIgnoreCase("Data entry clerk")) {
            sharedPreferences.edit().clear().apply();
            sharedPreferences2.edit().clear().apply();
            // sharedPreferences3.edit().clear().apply();
        }

        // start LoginActivity
        Intent startLoginActivity = new Intent(MenuActivity.this, LoginActivity.class);
        startActivity(startLoginActivity);
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
        finish();
    }

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
        Log.i("COORD MenuActivity", "Lat:" + location.getLatitude() + ", Lng:" + location.getLongitude());
        //showToast(location.getLatitude() + "," + location.getLongitude());
        SharedPreferences sharedPreferences = getSharedPreferences(UserRoleProcessor.SHARED_PREFS, MODE_PRIVATE);
        String userRole = sharedPreferences.getString(UserRoleProcessor.USER_ROLE, "");

        if(userRole.equalsIgnoreCase("Data entry clerk")) {

            SharedPreferences sharedPreferences2 = getSharedPreferences(OrgUnitLocationProcessor.SHARED_PREFS, MODE_PRIVATE);
            String coordinates = sharedPreferences2.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION, "");
            String rad = sharedPreferences2.getString(OrgUnitLocationProcessor.ORGUNIT_LOCATION_RADIUS, "");
            double requiredRadius = Double.parseDouble(rad);

            String[] coord = coordinates.split("[,]");
            Double lat = Double.parseDouble(coord[1].substring(0, coord[1].length() - 1));
            Double lng = Double.parseDouble(coord[0].substring(1));
            if (LAT != 0 && LNG != 0) {
                // calculate radius
                Location locRecorded = new Location("LocationA");
                locRecorded.setLatitude(lat);
                locRecorded.setLongitude(lng);

                Location myLocation = new Location("LocationB");
                myLocation.setLatitude(LAT);
                myLocation.setLongitude(LNG);

                double radius = myLocation.distanceTo(locRecorded);

                Log.i("RADIUS FROM MENU", String.valueOf(radius));
                if (radius > requiredRadius) {
                    // logout and clear shared preferences
                    showMessage("Location fence exceeded. Please contact administrator");
                    logOut();
                }
            } else {
                showMessage("Location is null, please check network connection");
                logOut();
            }
        }

    }

    @Override
    public void onLocationProviderEnabled() {
        showToast("Location services are now ON");
    }

    @Override
    public void onLocationProviderDisabled() {
        showToast("Location services are still Off");
    }

    private void showMessage(String message) {
        ToastManager.makeToast(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
}
