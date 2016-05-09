package blassoft.hitappme;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends ActionBarActivity {
    public final static String SEARCH_VALUE = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        StartAppActivity.OCore.OContext = this;

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.drawable.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_list);
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.search);
        adapter.setNotifyOnChange(true);
        textView.setAdapter(adapter);
        textView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    search(null);
                }
                return false;
            }
        });

        textView.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((s.length() >= 3)) {
                    adapter.clear();
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("search", s.toString()));
                    try {
                        StartAppActivity.OCore.callback = new CallbackInterface() {
                            public void onPostExecute() {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(), R.layout.item_list);
                                adapter.setNotifyOnChange(true);
                                //attach the adapter to textview
                                AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.search);
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
                                        TextView msgTextView = (TextView) findViewById(R.id.msg_text_view);
                                        msgTextView.setText(status + ": " + msg);
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

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            public void afterTextChanged(Editable s) {

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);

        menu.findItem(R.id.action_score).setVisible(false);
        if ("active".equals(StartAppActivity.OCore.userStatus)) {
            menu.findItem(R.id.action_score).setVisible(true);
        }

        menu.findItem(R.id.action_activate_account).setVisible(false);
        if ("need_activation".equals(StartAppActivity.OCore.userStatus) && !StartAppActivity.OCore.cell.isEmpty()) {
            menu.findItem(R.id.action_activate_account).setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                //openSearch();
                return true;
            case R.id.action_score:
                openScore();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_activate_account:
                openActiveAccount();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void openScore() {
        Intent intent = new Intent(this, ScoreActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    private void openActiveAccount() {
        Intent intent = new Intent(this, ActivateAccountActivity.class);
        startActivity(intent);
    }

    public void search(View view) {
        Intent intent = new Intent(this, ResultListActivity.class);
        EditText editText = (EditText) findViewById(R.id.search);
        String search = editText.getText().toString();
        if (search.isEmpty()) return;
        intent.putExtra(SEARCH_VALUE, search);
        startActivity(intent);
    }

    public void getRecentHits(View view) {
        Intent intent = new Intent(this, ResultListActivity.class);
        intent.putExtra(SEARCH_VALUE, getString(R.string.recent_hits));
        startActivity(intent);
    }

    public void getAround(View view) {
        Intent intent = new Intent(this, ResultListActivity.class);
        intent.putExtra(SEARCH_VALUE, getString(R.string.hits_around));
        startActivity(intent);
    }

    public void getTopHits(View view) {
        Intent intent = new Intent(this, ResultListActivity.class);
        intent.putExtra(SEARCH_VALUE, getString(R.string.top_hits));
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
