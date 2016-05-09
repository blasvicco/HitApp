package blassoft.hitappme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SettingActivity extends ActionBarActivity {
    private boolean ignoreTextChange = false;
    private sentReceiver sendSms;
    private deliverReceiver deliverSms;
    private boolean receiverRegistered = false;

    class deliverReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            String result = "";
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    result = "SMS delivered";
                    break;
                case Activity.RESULT_CANCELED:
                    result = "SMS not delivered";
                    break;
            }
            Log.d("MSG " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", result);
        }

    }

    class sentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            String result = "";
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    result = "Transmission successful";
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    result = "Transmission failed";
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    result = "Radio off";
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    result = "No PDU defined";
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    result = "No service";
                    break;
            }
            Log.d("MSG " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", result);
        }
    }

    private void loadSetting() {
        try {
            StartAppActivity.OCore.callback = new CallbackInterface() {
                public void onPostExecute() {
                    String status;
                    String msg;
                    try {
                        status = StartAppActivity.OCore.response.getString("status");
                        msg = StartAppActivity.OCore.response.getString("msg");
                        StartAppActivity.OCore.user = StartAppActivity.OCore.response.getJSONObject("data").getJSONObject("user");
                        StartAppActivity.OCore.categories = StartAppActivity.OCore.response.getJSONObject("data").getJSONArray("categories");

                        if (status.equals("err")) {
                            Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);
                        } else {
                            fillFields();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            };
            StartAppActivity.OCore.getSettings();
        } catch (Exception e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        StartAppActivity.OCore.OContext = this;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.drawable.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        if (StartAppActivity.OCore.userStatus.equals("need_activation")) {
            EditText editCell = (EditText) findViewById(R.id.edit_cell);
            editCell.setEnabled(true);
        }

        try {
            fillFields();
            if (StartAppActivity.OCore.user != null) {
                TextView fixOrMobil = (TextView) findViewById(R.id.fix_or_mobil);
                fixOrMobil.setText(getString(R.string.location_mobil));
                TextView location = (TextView) findViewById(R.id.location);
                String fullAddress = "";
                if (StartAppActivity.OCore.user.getBoolean("fix_location")) {
                    fixOrMobil.setText(getString(R.string.location_fix));
                    fullAddress = StartAppActivity.OCore.location;
                } else {
                    if (StartAppActivity.OCore.locationHandler.address != null) {
                        fullAddress = StartAppActivity.OCore.getFullAddressFromLocationHandler();
                    }
                }
                location.setText(getString(R.string.location)+": "+fullAddress);
            } else {
                this.loadSetting();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }

        //categories
        final ArrayAdapter<String> adapterC1 = new ArrayAdapter<>(this, R.layout.item_list);
        AutoCompleteTextView textViewC1 = (AutoCompleteTextView) findViewById(R.id.edit_category_1);
        adapterC1.setNotifyOnChange(true);
        textViewC1.setAdapter(adapterC1);
        textViewC1.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((s.length() >= 3) && !ignoreTextChange) {
                    adapterC1.clear();
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("search", s.toString()));
                    try {
                        StartAppActivity.OCore.callback = new CallbackInterface() {
                            public void onPostExecute() {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(), R.layout.item_list);
                                adapter.setNotifyOnChange(true);
                                AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.edit_category_1);
                                textView.setAdapter(adapter);
                                String status;
                                String msg;
                                JSONArray objs;
                                try {
                                    status = StartAppActivity.OCore.response.getString("status");
                                    msg = StartAppActivity.OCore.response.getString("msg");
                                    objs = StartAppActivity.OCore.response.getJSONObject("data").getJSONArray("categories");

                                    if (status.equals("err")) {
                                        Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);
                                    } else if (objs != null) {
                                        for (int i = 0; i < objs.length(); i++) {
                                            JSONObject obj = objs.getJSONObject(i);
                                            adapter.add(obj.getString("name").trim());
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    StartAppActivity.OCore.logException(e.toString());
                                }
                            }
                        };
                        StartAppActivity.OCore.getCategories(params);
                    } catch (Exception e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void afterTextChanged(Editable s) { }
        });

        final ArrayAdapter<String> adapterC2 = new ArrayAdapter<>(this, R.layout.item_list);
        AutoCompleteTextView textViewC2 = (AutoCompleteTextView) findViewById(R.id.edit_category_2);
        adapterC2.setNotifyOnChange(true);
        textViewC2.setAdapter(adapterC2);
        textViewC2.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((s.length() >= 3) && !ignoreTextChange) {
                    adapterC2.clear();
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("search", s.toString()));
                    try {
                        StartAppActivity.OCore.callback = new CallbackInterface() {
                            public void onPostExecute() {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(), R.layout.item_list);
                                adapter.setNotifyOnChange(true);
                                //attach the adapter to textview
                                AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.edit_category_2);
                                textView.setAdapter(adapter);
                                String status;
                                String msg;
                                JSONArray objs;
                                try {
                                    status = StartAppActivity.OCore.response.getString("status");
                                    msg = StartAppActivity.OCore.response.getString("msg");
                                    objs = StartAppActivity.OCore.response.getJSONObject("data").getJSONArray("categories");

                                    if (status.equals("err")) {
                                        Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);
                                    } else if (objs != null) {
                                        for (int i = 0; i < objs.length(); i++) {
                                            JSONObject obj = objs.getJSONObject(i);
                                            adapter.add(obj.getString("name").trim());
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    StartAppActivity.OCore.logException(e.toString());
                                }
                            }
                        };
                        StartAppActivity.OCore.getCategories(params);
                    } catch (Exception e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void afterTextChanged(Editable s) { }
        });

        final ArrayAdapter<String> adapterC3 = new ArrayAdapter<>(this, R.layout.item_list);
        AutoCompleteTextView textViewC3 = (AutoCompleteTextView) findViewById(R.id.edit_category_3);
        adapterC3.setNotifyOnChange(true);
        textViewC3.setAdapter(adapterC3);
        textViewC3.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((s.length() >= 3) && !ignoreTextChange) {
                    adapterC3.clear();
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("search", s.toString()));
                    try {
                        StartAppActivity.OCore.callback = new CallbackInterface() {
                            public void onPostExecute() {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(), R.layout.item_list);
                                adapter.setNotifyOnChange(true);
                                //attach the adapter to textview
                                AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.edit_category_3);
                                textView.setAdapter(adapter);
                                String status;
                                String msg;
                                JSONArray objs;
                                try {
                                    status = StartAppActivity.OCore.response.getString("status");
                                    msg = StartAppActivity.OCore.response.getString("msg");
                                    objs = StartAppActivity.OCore.response.getJSONObject("data").getJSONArray("categories");

                                    if (status.equals("err")) {
                                        Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);
                                    } else if (objs != null) {
                                        for (int i = 0; i < objs.length(); i++) {
                                            JSONObject obj = objs.getJSONObject(i);
                                            adapter.add(obj.getString("name").trim());
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    StartAppActivity.OCore.logException(e.toString());
                                }
                            }
                        };

                        StartAppActivity.OCore.getCategories(params);
                    } catch (Exception e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void afterTextChanged(Editable s) { }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_setting, menu);

        menu.findItem(R.id.action_score).setVisible(false);
        if (StartAppActivity.OCore.userStatus.equals("active")) {
            menu.findItem(R.id.action_score).setVisible(true);
        }

        menu.findItem(R.id.action_activate_account).setVisible(false);
        if (StartAppActivity.OCore.userStatus.equals("need_activation") && !StartAppActivity.OCore.cell.isEmpty()) {
            menu.findItem(R.id.action_activate_account).setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                openSearch();
                return true;
            case R.id.action_score:
                openScore();
                return true;
            case R.id.action_settings:
                return true;
            case R.id.action_activate_account:
                openActiveAccount();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSearch() {
        finish();
        Intent intent = new Intent(this, SearchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EXIT", true);
        startActivity(intent);
    }

    private void openScore() {
        Intent intent = new Intent(this, ScoreActivity.class);
        startActivity(intent);
    }

    private void openActiveAccount() {
        Intent intent = new Intent(this, ActivateAccountActivity.class);
        startActivity(intent);
    }

    private void saveSettings() {
        String cellCodeVerification;
        try {
            List<NameValuePair> params = new ArrayList<>();
            EditText editCell = (EditText) findViewById(R.id.edit_cell);
            EditText editAltNumber = (EditText) findViewById(R.id.edit_alt_number);
            EditText editName = (EditText) findViewById(R.id.edit_name);
            AutoCompleteTextView cat1 = (AutoCompleteTextView) findViewById(R.id.edit_category_1);
            AutoCompleteTextView cat2 = (AutoCompleteTextView) findViewById(R.id.edit_category_2);
            AutoCompleteTextView cat3 = (AutoCompleteTextView) findViewById(R.id.edit_category_3);
            CheckBox editDiscoverable = (CheckBox) findViewById(R.id.edit_discoverable);

            if (editCell.isEnabled()) {
                if (editCell.getText().toString().isEmpty()) return;
                if (!StartAppActivity.OCore.cell.equals(editCell.getText().toString())) {
                    cellCodeVerification = "";
                    final String alphabet = "1234567890QWERTYUIOPASDFGHJKLZXCVBNM";
                    final int N = alphabet.length();
                    Random r = new Random();
                    for (int i = 0; i < 4; i++) {
                        cellCodeVerification += alphabet.charAt(r.nextInt(N));
                    }

                    SmsManager smsManager = SmsManager.getDefault();
                    String sms_msg = getString(R.string.use_next_code);

                    String SENT = "SMS_SENT";
                    String DELIVERED = "SMS_DELIVERED";

                    /*Create Pending Intents*/
                    Intent sentIntent = new Intent(SENT);
                    PendingIntent sentPI = PendingIntent.getBroadcast(getApplicationContext(), 0, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    Intent deliveryIntent = new Intent(DELIVERED);
                    PendingIntent deliverPI = PendingIntent.getBroadcast(getApplicationContext(), 0, deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    if (!receiverRegistered) {
                        registerReceiver(sendSms, new IntentFilter(SENT));
                        registerReceiver(deliverSms, new IntentFilter(DELIVERED));
                        receiverRegistered = true;
                    }

                    TelephonyManager tm;
                    tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
                    if (tm.getLine1Number().isEmpty()) {
                        if (!editCell.getText().toString().isEmpty()) {
                            params.add(new BasicNameValuePair("cell", editCell.getText().toString()));
                            smsManager.sendTextMessage(editCell.getText().toString(), null, sms_msg + ": " + cellCodeVerification, sentPI, deliverPI);
                            StartAppActivity.OCore.cell = editCell.getText().toString();
                        }
                    } else {
                        params.add(new BasicNameValuePair("cell", tm.getLine1Number()));
                        smsManager.sendTextMessage(tm.getLine1Number(), null, sms_msg + ": " + cellCodeVerification, sentPI, deliverPI);
                        StartAppActivity.OCore.cell = tm.getLine1Number();
                    }

                    StartAppActivity.OCore.userStatus = "need_activation";
                    params.add(new BasicNameValuePair("activate_code", cellCodeVerification));
                }
            }

            StartAppActivity.OCore.user = new JSONObject();
            StartAppActivity.OCore.user.put("name", editName.getText().toString());
            StartAppActivity.OCore.user.put("cell", StartAppActivity.OCore.cell);
            StartAppActivity.OCore.user.put("alt_number", editAltNumber.getText().toString());
            StartAppActivity.OCore.user.put("available", (editDiscoverable.isChecked() ? "true" : "false"));
            StartAppActivity.OCore.user.put("fix_location", StartAppActivity.OCore.fixLocation);
            StartAppActivity.OCore.categories = new JSONArray();
            StartAppActivity.OCore.categories.put(0, cat1.getText());
            StartAppActivity.OCore.categories.put(1, cat2.getText());
            StartAppActivity.OCore.categories.put(2, cat3.getText());

            params.add(new BasicNameValuePair("name", editName.getText().toString()));
            params.add(new BasicNameValuePair("categories", cat1.getText() + "|" + cat2.getText() + "|" + cat3.getText()));
            params.add(new BasicNameValuePair("available", (editDiscoverable.isChecked() ? "true" : "false")));

            if (!StartAppActivity.OCore.user.getBoolean("fix_location")) {
                if (StartAppActivity.OCore.locationHandler.currentBestLocation != null) {
                    String lat = String.valueOf(StartAppActivity.OCore.locationHandler.currentBestLocation.getLatitude());
                    String lon = String.valueOf(StartAppActivity.OCore.locationHandler.currentBestLocation.getLongitude());
                    params.add(new BasicNameValuePair("latitude", lat));
                    params.add(new BasicNameValuePair("longitude", lon));
                    if (StartAppActivity.OCore.locationHandler.address != null) {
                        params.add(new BasicNameValuePair("area", StartAppActivity.OCore.locationHandler.address.getAdminArea()));
                        params.add(new BasicNameValuePair("country", StartAppActivity.OCore.locationHandler.address.getCountryCode()));
                        params.add(new BasicNameValuePair("locality", StartAppActivity.OCore.locationHandler.address.getLocality()));
                        params.add(new BasicNameValuePair("sub_area", StartAppActivity.OCore.locationHandler.address.getSubAdminArea()));
                        params.add(new BasicNameValuePair("zipcode", StartAppActivity.OCore.locationHandler.address.getPostalCode()));
                        params.add(new BasicNameValuePair("thoroughfare", StartAppActivity.OCore.locationHandler.address.getThoroughfare()));
                    }
                }
            }

            StartAppActivity.OCore.callback = new CallbackInterface() {
                public void onPostExecute() {
                    String status;
                    String msg;
                    try {
                        status = StartAppActivity.OCore.response.getString("status");
                        msg = StartAppActivity.OCore.response.getString("msg");
                        Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            };

            StartAppActivity.OCore.setSettings(params);
        } catch (Exception e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }
    }

    public void setLocation(View view) {
        final CharSequence[] items = {getString(R.string.location_mobil), getString(R.string.location_fix)};
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose))
                .setItems(items, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (StartAppActivity.OCore.locationHandler.currentBestLocation != null) {
                            List<NameValuePair> params = new ArrayList<>();
                            TextView fixOrMobil = (TextView) findViewById(R.id.fix_or_mobil);
                            try {
                                switch (which) {
                                    default:
                                    case 0:
                                        params.add(new BasicNameValuePair("fix_location", "false"));
                                        StartAppActivity.OCore.fixLocation = false;
                                        StartAppActivity.OCore.user.put("fix_location", false);
                                        fixOrMobil.setText(getString(R.string.location_mobil));
                                        break;
                                    case 1:
                                        params.add(new BasicNameValuePair("fix_location", "true"));
                                        StartAppActivity.OCore.fixLocation = true;
                                        StartAppActivity.OCore.user.put("fix_location", true);
                                        fixOrMobil.setText(getString(R.string.location_fix));
                                        break;
                                }

                                StartAppActivity.OCore.callback = new CallbackInterface() {
                                    public void onPostExecute() {
                                        String fullAddress = StartAppActivity.OCore.getFullAddressFromLocationHandler();
                                        StartAppActivity.OCore.location = fullAddress;
                                        TextView location = (TextView) findViewById(R.id.location);
                                        location.setText(getString(R.string.location)+": "+fullAddress);
                                    }
                                };
                                StartAppActivity.OCore.setLocation(params);
                            } catch (Exception e) {
                                e.printStackTrace();
                                StartAppActivity.OCore.logException(e.toString());
                            }
                        }
                    }

                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onPause() {
        saveSettings();
        super.onPause();

        if (receiverRegistered) {
            try {
                unregisterReceiver(sendSms);
                unregisterReceiver(deliverSms);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                receiverRegistered = false;
                Intent intent = new Intent(StartAppActivity.OCore.OContext, ActivateAccountActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onBackPressed() {
        openSearch();
    }

    private void fillFields() throws JSONException {
        if (StartAppActivity.OCore.user != null) {
            EditText editCell = (EditText) findViewById(R.id.edit_cell);
            if (StartAppActivity.OCore.cell.isEmpty()) {
                editCell.setText(StartAppActivity.OCore.user.getString("cell").trim());
            } else {
                editCell.setText(StartAppActivity.OCore.cell.trim());
            }

            EditText editName = (EditText) findViewById(R.id.edit_name);
            if (!StartAppActivity.OCore.user.getString("name").trim().equals("null") && !StartAppActivity.OCore.user.getString("name").trim().isEmpty()) {
                editName.setText(StartAppActivity.OCore.user.getString("name").trim());
            }

            EditText editAltNumber = (EditText) findViewById(R.id.edit_alt_number);
            if (!StartAppActivity.OCore.user.getString("alt_number").trim().equals("null") && !StartAppActivity.OCore.user.getString("alt_number").trim().isEmpty()) {
                editAltNumber.setText(StartAppActivity.OCore.user.getString("alt_number").trim());
            }

            CheckBox editDiscoverable = (CheckBox) findViewById(R.id.edit_discoverable);
            editDiscoverable.setChecked(StartAppActivity.OCore.user.getBoolean("available"));

            TextView fixOrMobil = (TextView) findViewById(R.id.fix_or_mobil);
            fixOrMobil.setText(getString(R.string.location_mobil));
            TextView location = (TextView) findViewById(R.id.location);
            String fullAddress;
            if (StartAppActivity.OCore.fixLocation) {
                fixOrMobil.setText(getString(R.string.location_fix));
                fullAddress = StartAppActivity.OCore.getFullAddressFromUser();
            } else {
                fullAddress = StartAppActivity.OCore.getFullAddressFromLocationHandler();
            }
            StartAppActivity.OCore.location = !fullAddress.isEmpty() ? fullAddress : StartAppActivity.OCore.location;
            location.setText(getString(R.string.location) + ": " + StartAppActivity.OCore.location);

        }

        if (StartAppActivity.OCore.categories != null) {
            ignoreTextChange = true;
            for (int i = 0; i < StartAppActivity.OCore.categories.length(); i++) {
                if (i == 0) {
                    AutoCompleteTextView cat1 = (AutoCompleteTextView) findViewById(R.id.edit_category_1);
                    cat1.setText(StartAppActivity.OCore.categories.getString(i).trim());
                } else if (i == 1) {
                    AutoCompleteTextView cat2 = (AutoCompleteTextView) findViewById(R.id.edit_category_2);
                    cat2.setText(StartAppActivity.OCore.categories.getString(i).trim());
                } else if (i == 2) {
                    AutoCompleteTextView cat3 = (AutoCompleteTextView) findViewById(R.id.edit_category_3);
                    cat3.setText(StartAppActivity.OCore.categories.getString(i).trim());
                }
            }
            ignoreTextChange = false;
        }
    }
}
