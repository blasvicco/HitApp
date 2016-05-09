package blassoft.hitappme;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationHandler implements LocationListener {
    public Location currentBestLocation;
    private final LocationManager locationManager;
    public Address address;

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, this.currentBestLocation)) {
            this.currentBestLocation = location;
            this.locationManager.removeUpdates(this);
            Geocoder geocoder = new Geocoder(StartAppActivity.OCore.OContext, Locale.ENGLISH);
            //Place your latitude and longitude
            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(StartAppActivity.OCore.locationHandler.currentBestLocation.getLatitude(), StartAppActivity.OCore.locationHandler.currentBestLocation.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
                StartAppActivity.OCore.logException(e.toString());
            }

            if (addresses != null) {
                StartAppActivity.OCore.locationHandler.address = addresses.get(0);
                try {
                    StartAppActivity.OCore.callback = new CallbackInterface() {
                        public void onPostExecute() {
                            String status;
                            try {
                                status = StartAppActivity.OCore.response.getString("status");
                                if (status.equals("err")) {
                                    Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", "");
                                } else {
                                    StartAppActivity.OCore.loadSetLocation = false;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    List<NameValuePair> params = new ArrayList<>();
                    StartAppActivity.OCore.setLocation(params);
                } catch (Exception e) {
                    e.printStackTrace();
                    StartAppActivity.OCore.logException(e.toString());
                }
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public LocationHandler(LocationManager systemService) {
        locationManager = systemService;
    }

    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        int waitTime = 1000 * 60 * 2;
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > waitTime;
        boolean isSignificantlyOlder = timeDelta < -waitTime;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = this.isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public void locate() {
        this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 500, this);
        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30000, 500, this);
    }
}
