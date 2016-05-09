package blassoft.hitappme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;

public class StartAppActivity extends Activity {
    public final static Core OCore = new Core();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_app);
        OCore.OContext = this;
        if (initialize()) {
            class AsyncTaskHandler extends AsyncTask<Object, Integer, Object> {
                @Override
                protected Object doInBackground(Object[] params) {
                    retrieveSimInformation();
                    try {
                        OCore.getKey();
                        OCore.login();
                    } catch (Exception e) {
                        e.printStackTrace();
                        OCore.logException(e.toString());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Object obj) {
                    //check first login
                    if (OCore.userStatus != null) {
                        if (OCore.userStatus.equals("need_activation") && OCore.loadFirstLogin) {
                            Intent intent = new Intent(OCore.OContext, SettingActivity.class);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(OCore.OContext, SearchActivity.class);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        new AlertDialog.Builder(StartAppActivity.OCore.OContext)
                                .setTitle("Error")
                                .setMessage(R.string.err_cannot_connect_to_server)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }
            }
            AsyncTaskHandler startApp = new AsyncTaskHandler();
            startApp.execute();
        }
    }

    private boolean initialize() {
        ConnectivityManager connMgr = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
        OCore.networkInfo = connMgr.getActiveNetworkInfo();
        LocationManager lManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        if (!lManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            buildAlertMessageNoGps();
        } else {
            OCore.locationHandler = new LocationHandler(lManager);
            OCore.locationHandler.locate();
            return true;
        }
        return false;
    }

    private void retrieveSimInformation() {
        try {
            TelephonyManager tm;
            tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
            OCore.imei = tm.getDeviceId();
            OCore.simSerialNumber = tm.getSimSerialNumber();
            OCore.id = OCore.simSerialNumber;
            OCore.cell = tm.getLine1Number();
        } catch (Exception e) {
            e.printStackTrace();
            OCore.logException(e.toString());
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.geo_is_not_on))
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
