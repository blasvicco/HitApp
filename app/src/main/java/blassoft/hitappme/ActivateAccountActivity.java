package blassoft.hitappme;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ActivateAccountActivity extends ActionBarActivity {
    static private String cellCodeVerification = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activate_account);
        StartAppActivity.OCore.OContext = this;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.drawable.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activate_account, menu);

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
                openSettings();
                return true;
            case R.id.action_activate_account:
                //openActiveAccount();
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
        finish();
        Intent intent = new Intent(this, ScoreActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        finish();
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    public void getNewCode(View view) {
        try {
            ActivateAccountActivity.cellCodeVerification = "";
            final String alphabet = "1234567890QWERTYUIOPASDFGHJKLZXCVBNM";
            final int N = alphabet.length();
            Random r = new Random();
            for (int i = 0; i < 4; i++) {
                ActivateAccountActivity.cellCodeVerification += alphabet.charAt(r.nextInt(N));
            }

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("activate_code", ActivateAccountActivity.cellCodeVerification));

            StartAppActivity.OCore.callback = new CallbackInterface() {
                public void onPostExecute() {
                    String status;
                    String msg;
                    try {
                        status = StartAppActivity.OCore.response.getString("status");
                        msg = StartAppActivity.OCore.response.getString("msg");

                        if (status.equals("err")) {
                            Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);
                        }

                        TextView msgTextView = (TextView) findViewById(R.id.msg_text_view_aa);
                        msgTextView.setText(status + ": " + msg);
                        msgTextView.postDelayed(new Runnable() {
                            public void run() {
                                TextView msgTextView = (TextView) findViewById(R.id.msg_text_view_aa);
                                //msgTextView.setVisibility(View.INVISIBLE);
                                msgTextView.setText("");
                            }
                        }, 3000);

                        SmsManager smsManager = SmsManager.getDefault();
                        String sms_msg = getString(R.string.use_next_code);
                        PendingIntent sentPI = PendingIntent.getBroadcast(ActivateAccountActivity.this, 0, new Intent("SMS_SENT"), PendingIntent.FLAG_UPDATE_CURRENT);
                        smsManager.sendTextMessage(StartAppActivity.OCore.cell, null, sms_msg + ": " + ActivateAccountActivity.cellCodeVerification, sentPI, null);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            };

            StartAppActivity.OCore.setNewActivationCode(params);
        } catch (Exception e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }
    }

    public void activate(View view) {
        try {
            TextView activationCode = (TextView) findViewById(R.id.activation_code);

            TextView msgTextView = (TextView) findViewById(R.id.msg_text_view_aa);
            msgTextView.setText(getString(R.string.wait) + "...");

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("activate_code", activationCode.getText().toString().toUpperCase()));

            StartAppActivity.OCore.callback = new CallbackInterface() {
                public void onPostExecute() {
                    String status;
                    String msg;
                    try {
                        status = StartAppActivity.OCore.response.getString("status");
                        msg = StartAppActivity.OCore.response.getString("msg");

                        if (status.equals("err")) {
                            Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);
                            TextView msgTextView = (TextView) findViewById(R.id.msg_text_view_aa);
                            msgTextView.setText(status + ": " + msg);
                            msgTextView.postDelayed(new Runnable() {
                                public void run() {
                                    TextView msgTextView = (TextView) findViewById(R.id.msg_text_view_aa);
                                    //msgTextView.setVisibility(View.INVISIBLE);
                                    msgTextView.setText("");
                                }
                            }, 3000);
                        } else {
                            finish();
                            StartAppActivity.OCore.userStatus = "active";
                            Intent intent = new Intent(ActivateAccountActivity.this, SearchActivity.class);
                            startActivity(intent);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            };

            StartAppActivity.OCore.activateUser(params);
        } catch (Exception e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }
    }

    @Override
    public void onBackPressed() {
        openSearch();
    }
}
