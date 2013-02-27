package edu.iastate.mis.spring;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.apache.cordova.api.PluginResult;


import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;

import java.util.Date;
//import android.widget.Toast;

/**
 * PhoneGap plugin for retrieving GPS location via native api.
 *
 * references:
 * http://developer.android.com/reference/android/location/LocationManager.html
 * https://github.com/apache/incubator-cordova-android/blob/1.4.1/framework/src/com/phonegap/api/Plugin.java
 * http://docs.phonegap.com/en/1.4.1/phonegap_geolocation_geolocation.md.html
 * @author m.augustynowicz
 * 
 */
public class NativeGeo1 extends CordovaPlugin {

    //public long maximumAge = 1000 * 60; // ms
    public long timeout    = 1000 * 30; // ms

    public static final String ACTION_GETCURRENTPOSITION="getCurrentPosition";

    protected CallbackContext callbackContext = null;

    /*
     * Argument passed to success callback is similar to PhoneGap's Position:
     * http://docs.phonegap.com/en/1.4.1/phonegap_geolocation_geolocation.md.html#Position
     *
     * @see com.phonegap.api.Plugin#execute(java.lang.String,
     * org.json.JSONArray, java.lang.String)
     */
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext)
    {

    	Log.d("HelloPlugin", "Hello, this is a native function called from PhoneGap/Cordova!");
       
    	this.callbackContext = callbackContext;
    	final LocationManager locationManager = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
        String provider = getBestProvider(locationManager);


        if (ACTION_GETCURRENTPOSITION.equals(action))
        {
            Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
           if (lastKnownLocation != null)
            {
                gotLocation(lastKnownLocation);
               return true;
        	
            }
         else
            {
                cordova.getActivity().runOnUiThread(new RunnableLocationListener(
                     callbackContext, locationManager, provider
                ));
                return true;
            }
        }
        else
        {
            callbackContext.error("Invalid Action Selected");
            return false;
        }

    }

    public void gotLocation (Location location)
    {
        try
        {
            JSONObject geoposition = new JSONObject();
            JSONObject coords = new JSONObject();
            coords.put("latitude",         location.getLatitude());
            coords.put("longitude",        location.getLongitude());
            coords.put("altitude",         location.getAltitude());
            coords.put("accuracy",         location.getAccuracy());
            coords.put("altitudeAccuracy", null);
            coords.put("heading",          null);
            coords.put("speed",            location.getSpeed());
            geoposition.put("coords",    coords);
            geoposition.put("timestamp", location.getTime());
            geoposition.put("provider",  location.getProvider());
            callbackContext.success(coords);
        }
        catch (JSONException jsonEx)
        {
        	callbackContext.error("JSON_EXCEPTION");
        }
        
    }

    protected String getBestProvider (LocationManager locationManager)
    {
        String provider = LocationManager.PASSIVE_PROVIDER;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            provider = LocationManager.GPS_PROVIDER;
        }
        else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            provider = LocationManager.NETWORK_PROVIDER;
        }
        return provider;
    }


    class RunnableLocationListener extends Thread
    {
        //protected final NativeGeo1 plugin;
        protected final LocationManager locationManager;
        protected final String provider;
        protected final CallbackContext callbackContext;

        public Boolean ended = null;

        public RunnableLocationListener (CallbackContext callbackContext, LocationManager locationManager, String provider)
        {
            this.locationManager = locationManager;
            this.provider = provider;
            this.callbackContext = callbackContext;
        }

        public void run ()
        {
            ended = false;

            final LocationListener locationListener = new LocationListener() {
                public void onLocationChanged (Location location) {
                    if ( false == ended )
                    {
                        gotLocation(location);
                        locationManager.removeUpdates(this);
                    }
                }
                public void onStatusChanged (String provider, int status, Bundle extras) {}
                public void onProviderEnabled(String provider) {}
                public void onProviderDisabled(String provider) {}
            };

            locationManager.requestLocationUpdates(
                provider, 0, 0, locationListener
            );


            Thread timeouter = new Thread() {
                public void run ()
                {
                    try
                    {
                        Thread.sleep(timeout);

                        ended = true;
                        locationManager.removeUpdates(locationListener);
                        callbackContext.error("Timeout");
                    }
                    catch (java.lang.InterruptedException ex)
                    {
                    	callbackContext.error("Interrupted Exception");
                    }
                }
            };

            timeouter.start();

        }
    }

}