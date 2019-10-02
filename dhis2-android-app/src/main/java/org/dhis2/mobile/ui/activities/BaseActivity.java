package org.dhis2.mobile.ui.activities;

import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.widget.Toast;

import com.aussekalega.openlocator.OpenLocatorAppCompatActivity;
import com.aussekalega.openlocator.OpenLocatorRequest;
import com.aussekalega.openlocator.OpenLocatorRequestBuilder;
import com.google.android.gms.location.LocationRequest;

import org.dhis2.mobile.NetworkStateReceiver;
import org.dhis2.mobile.R;
import org.dhis2.mobile.utils.CustomTypefaceSpan;
import org.dhis2.mobile.utils.TypefaceManager;

public class BaseActivity extends OpenLocatorAppCompatActivity {
    private Typeface mCustomTypeFace;
    private NetworkStateReceiver mNetworkStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCustomTypeFace = getTypeFace();
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
    public void setTitle(CharSequence sequence) {
        if (getSupportActionBar() == null) {
            return;
        }

        if (sequence != null && mCustomTypeFace != null) {
            CustomTypefaceSpan typefaceSpan = new CustomTypefaceSpan(mCustomTypeFace);
            SpannableString title = new SpannableString(sequence);
            title.setSpan(typefaceSpan, 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(title);
        } else {
            getSupportActionBar().setTitle(sequence);
        }
    }

    private Typeface getTypeFace() {
        AssetManager manager = getAssets();
        String fontName = getString(R.string.regular_font_name);
        if (manager != null && !TextUtils.isEmpty(fontName)) {
            return TypefaceManager.getTypeface(getAssets(), fontName);
        } else {
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mNetworkStateReceiver = new NetworkStateReceiver();
        registerReceiver(mNetworkStateReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mNetworkStateReceiver!=null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            unregisterReceiver(mNetworkStateReceiver);
        }
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
        //LAT = location.getLatitude();
        //LNG = location.getLongitude();
        //Log.i("COORDINATES", "Lat:" + location.getLatitude() + ", Lng:" + location.getLongitude());
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
