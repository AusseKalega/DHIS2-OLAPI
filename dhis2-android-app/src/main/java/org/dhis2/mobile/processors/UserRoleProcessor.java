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

public class UserRoleProcessor {
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    public static final String USER_ROLE = "user_role";
    public static final String SHARED_PREFS = "sharedPrefs";
    private static String API_USER_ROLE = "";

    public static boolean pullUserRole(Context context, String server,
                                         String creds) {

        if (context == null || server == null
                || creds == null) {
            Log.i(ServerInfoProcessor.class.getName(), "Pull server info fail");
            return false;
        }

        String url = prepareUrl2(server, creds);

        Response resp = tryToGetUserRoleId(url, creds);

        // Checking validity of server URL
        if (!URLUtil.isValidUrl(url)) {
            return false;
        }

        // If credentials and address is correct,
        // user information will be saved to internal storage
        if (!HTTPClient.isError(resp.getCode())) {
            String userRoleId;
            try {
                JSONObject jsonObject = new JSONObject(resp.getBody());
                JSONObject jsonForId = jsonObject.getJSONObject("userCredentials").getJSONArray("userRoles").getJSONObject(0);
                userRoleId = jsonForId.getString("id");
                Log.i("USER_ROLE_ID", userRoleId );
                if(userRoleId ==null || userRoleId .equals("")){
                    throw new ParsingException("User role id not found");
                }
            } catch (ParsingException | JSONException e) {
                e.printStackTrace();
                return false;
            }
//            if(PrefUtils.getUserRole(context) == null ||
//                    !PrefUtils.getUserRole(context).equals(displayName)) {
//                PrefUtils.initServerData(context, displayName);
//            }
            API_USER_ROLE = URLConstants.API_USER_ROLE_NAME + userRoleId + "/?fields=displayName";
        }
        String url2 = prepareUrl(server, creds);
        Response resp2 = tryToPullUserRole(url, creds);

        // If credentials and address is correct,
        // user information will be saved to internal storage
        if (!HTTPClient.isError(resp2.getCode())) {
            String displayName;
            try {
                JsonObject jsonForm = JsonHandler.buildJsonObject(resp2);
                displayName = jsonForm.get("displayName").getAsString();
                if(displayName==null || displayName.equals("")){
                    throw new ParsingException("User role not found");
                }
            } catch (ParsingException e) {
                e.printStackTrace();
                return false;
            }
//            if(PrefUtils.getUserRole(context) == null ||
//                    !PrefUtils.getUserRole(context).equals(displayName)) {
//                PrefUtils.initServerData(context, displayName);
//            }
            savaUserRole(context, displayName);
        }
        return true;
    }

    private static void savaUserRole(Context context, String displayName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USER_ROLE, displayName);
        editor.apply();
    }

    private static String prepareUrl(String initialUrl, String creds) {
        if (initialUrl.contains(HTTPS) || initialUrl.contains(HTTP)) {
            return initialUrl;
        }

        // try to use https
        Response response = tryToPullUserRole(HTTPS + initialUrl, creds);
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
        Response response = tryToGetUserRoleId(HTTPS + initialUrl, creds);
        if (response.getCode() != HttpURLConnection.HTTP_MOVED_PERM) {
            return HTTPS + initialUrl;
        } else {
            return HTTP + initialUrl;
        }
    }

    private static Response tryToPullUserRole(String server, String creds) {
        String url = server + API_USER_ROLE;
        return HTTPClient.get(url, creds);
    }

    private static Response tryToGetUserRoleId(String server, String creds){
        String url = server + URLConstants.API_USER_ROLE_ID;
        return HTTPClient.get(url, creds);
    }
}
