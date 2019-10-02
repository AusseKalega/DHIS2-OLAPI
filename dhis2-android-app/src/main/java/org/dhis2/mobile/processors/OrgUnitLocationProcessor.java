package org.dhis2.mobile.processors;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.URLUtil;

import com.google.gson.JsonObject;

import org.dhis2.mobile.io.json.JsonHandler;
import org.dhis2.mobile.io.json.ParsingException;
import org.dhis2.mobile.network.HTTPClient;
import org.dhis2.mobile.network.Response;
import org.dhis2.mobile.network.URLConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

public class OrgUnitLocationProcessor {
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    public static final String ORGUNIT_LOCATION = "user_location";
    public static final String ORGUNIT_LOCATION_RADIUS = "user_radius";
    public static final String ORGUNIT_LOCATION_STARTTIME = "user_starttime";
    public static final String ORGUNIT_LOCATION_STOPTIME = "user_stoptime";
    public static final String SHARED_PREFS = "sharedPrefs";
    private static String API_ORGUNIT_LOCATION = "";
    private static String API_ORGUNIT_LOCATION_RADIUS = "";
    private static String COORDS="";
    private static String RADIUS = "";
    private static String START_TIME = "";
    private static String STOP_TIME = "";

    public static boolean pullOrgUnitLocation(Context context, String server,
                                              String creds) {

        if (context == null || server == null
                || creds == null) {
            Log.i(ServerInfoProcessor.class.getName(), "Pull server info fail");
            return false;
        }

        String url = prepareUrl2(server, creds);

        Response resp = tryToGetOrgId(url, creds);

        // Checking validity of server URL
        if (!URLUtil.isValidUrl(url)) {
            return false;
        }

        // If credentials and address is correct,
        // user information will be saved to internal storage
        if (!HTTPClient.isError(resp.getCode())) {
            String orgLocationId;
            try {
                JSONObject jsonObject = new JSONObject(resp.getBody());
                JSONObject jsonForId = jsonObject.getJSONArray("organisationUnits").getJSONObject(0);
                orgLocationId = jsonForId.getString("id");
                Log.i("ORGANISATION UNIT ID", orgLocationId );
                if(orgLocationId ==null || orgLocationId.equals("")){
                    throw new ParsingException("Organisation unit id not found");
                }
            } catch (ParsingException | JSONException e) {
                e.printStackTrace();
                return false;
            }
//            if(PrefUtils.getUserRole(context) == null ||
//                    !PrefUtils.getUserRole(context).equals(displayName)) {
//                PrefUtils.initServerData(context, displayName);
//            }
            API_ORGUNIT_LOCATION = URLConstants.API_ORGUNIT_COORDINATES + orgLocationId + "/?fields=coordinates";
            API_ORGUNIT_LOCATION_RADIUS = URLConstants.API_ORGUNIT_COORDINATES + orgLocationId + "/attributeValues/attributeValue";

        }

        String url2 = prepareUrl(server, creds);
        Response resp2 = tryToPullLocation(url2, creds);

        // Checking validity of server URL
        if (!URLUtil.isValidUrl(url2)) {
            return false;
        }


        // If credentials and address is correct,
        // user information will be saved to internal storage
        if (!HTTPClient.isError(resp2.getCode())) {
            String coords;
            try {
                JsonObject jsonForm = JsonHandler.buildJsonObject(resp2);
                coords = jsonForm.get("coordinates").getAsString();
                if(coords==null || coords.equals("")){
                    throw new ParsingException("coordinates not found");
                }
            } catch (ParsingException e) {
                e.printStackTrace();
                return false;
            }

//            if(PrefUtils.getUserRole(context) == null ||
//                    !PrefUtils.getUserRole(context).equals(displayName)) {
//                PrefUtils.initServerData(context, displayName);
//            }
            COORDS = coords;

        }


        String url3 = prepareUrl3(server, creds);
        Response resp3 = tryToGetLocationRadius(url3, creds);

        // Checking validity of server URL
        if (!URLUtil.isValidUrl(url3)) {
            return false;
        }


        // If credentials and address is correct,
        // user information will be saved to internal storage
        if (!HTTPClient.isError(resp3.getCode())) {
            String radius ="", starttime = "", stoptime = "", result, lastString;
            try {
                JSONObject jsonObject = new JSONObject(resp3.getBody());
                // JSONObject jsonForRadius = jsonObject.getJSONArray("attributeValues").getJSONObject(0);
                JSONObject json1 = jsonObject.getJSONArray("attributeValues").getJSONObject(0);
                JSONObject json2 = jsonObject.getJSONArray("attributeValues").getJSONObject(1);
                JSONObject json3 = jsonObject.getJSONArray("attributeValues").getJSONObject(2);

                //Log.i("TESTING FROM ORU", String.valueOf(jsonForRadius));

                result = json1.getString("value");
                lastString = result.substring(result.length()-1);

                if(lastString.equalsIgnoreCase("r")){
                    radius = result.substring(0, result.length()-1);
                }else if (lastString.equalsIgnoreCase("s")){
                    starttime = result.substring(0, result.length()-1);
                } else {
                    stoptime = result.substring(0, result.length()-1);
                }

                result = json2.getString("value");
                lastString = result.substring(result.length()-1);

                if(lastString.equalsIgnoreCase("r")){
                    radius = result.substring(0, result.length()-1);
                }else if (lastString.equalsIgnoreCase("s")){
                    starttime = result.substring(0, result.length()-1);
                } else {
                    stoptime = result.substring(0, result.length()-1);
                }

                result = json3.getString("value");
                lastString = result.substring(result.length()-1);

                if(lastString.equalsIgnoreCase("r")){
                    radius = result.substring(0, result.length()-1);
                }else if (lastString.equalsIgnoreCase("s")){
                    starttime = result.substring(0, result.length()-1);
                } else {
                    stoptime = result.substring(0, result.length()-1);
                }

                if(radius==null || radius.equals("")){
                    throw new ParsingException("radius value not found");
                }
                if(starttime==null || starttime.equals("")){
                    throw new ParsingException("start time value not found");
                }
                if(stoptime==null || stoptime.equals("")){
                    throw new ParsingException("radius value not found");
                }
            } catch (ParsingException | JSONException e ) {
                e.printStackTrace();
                return false;
            }

//            if(PrefUtils.getUserRole(context) == null ||
//                    !PrefUtils.getUserRole(context).equals(displayName)) {
//                PrefUtils.initServerData(context, displayName);
//            }
            RADIUS = radius;
            START_TIME = starttime;
            STOP_TIME = stoptime;
        }

        saveUserRole(context, COORDS, RADIUS, START_TIME, STOP_TIME);
        Log.i("COORDS:", COORDS);
        Log.i("RADIUS", RADIUS);
        Log.i("START_TIME", START_TIME);
        Log.i("STOP_TIME", STOP_TIME);
        return true;
    }

    private static void saveUserRole(Context context, String coords, String radius,
                                     String starttime, String stoptime) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ORGUNIT_LOCATION, coords);
        editor.putString(ORGUNIT_LOCATION_RADIUS, radius);
        editor.putString(ORGUNIT_LOCATION_STARTTIME, starttime);
        editor.putString(ORGUNIT_LOCATION_STOPTIME, stoptime);

        editor.apply();
    }

    private static String prepareUrl(String initialUrl, String creds) {
        if (initialUrl.contains(HTTPS) || initialUrl.contains(HTTP)) {
            return initialUrl;
        }

        // try to use https
        Response response = tryToPullLocation(HTTPS + initialUrl, creds);
        if (response.getCode() != HttpURLConnection.HTTP_MOVED_PERM) {
            return HTTPS + initialUrl;
        } else {
            return HTTP + initialUrl;
        }
    }

    private static String prepareUrl2(String initialUrl, String creds) {
        if (initialUrl.contains(HTTPS) || initialUrl.contains(HTTP)) {
            return initialUrl;
        }

        // try to use https
        Response response = tryToGetOrgId(HTTPS + initialUrl, creds);
        if (response.getCode() != HttpURLConnection.HTTP_MOVED_PERM) {
            return HTTPS + initialUrl;
        } else {
            return HTTP + initialUrl;
        }
    }

    private static String prepareUrl3(String initialUrl, String creds) {
        if (initialUrl.contains(HTTPS) || initialUrl.contains(HTTP)) {
            return initialUrl;
        }

        // try to use https
        Response response = tryToGetLocationRadius(HTTPS + initialUrl, creds);
        if (response.getCode() != HttpURLConnection.HTTP_MOVED_PERM) {
            return HTTPS + initialUrl;
        } else {
            return HTTP + initialUrl;
        }
    }

    private static Response tryToPullLocation(String server, String creds) {
        String url = server + API_ORGUNIT_LOCATION;
        return HTTPClient.get(url, creds);
    }

    private static Response tryToGetOrgId(String server, String creds){
        String url = server + URLConstants.API_USER_ORGUNIT_ID;
        return HTTPClient.get(url, creds);
    }

    private static Response tryToGetLocationRadius(String server, String creds){
        String url = server + API_ORGUNIT_LOCATION_RADIUS;
        return HTTPClient.get(url, creds);
    }

}
