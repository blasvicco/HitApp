package blassoft.hitappme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import blassoft.hitappme.util.MyScrollView;

public class ResultListActivity extends ActionBarActivity {
    private static JSONArray users_by_d;
    private static JSONArray users_by_s;
    private int page = 1;
    private final int step = 5;
    private ProgressDialog loadingDialog;

    private static void populateList(JSONArray data, Context context) {
        Activity activity = (Activity) context;
        if (data != null) {
            TableLayout tableLayout = (TableLayout) activity.findViewById(R.id.table_result);
            if (tableLayout == null) return;
            for (int i = tableLayout.getChildCount(); i < ResultListActivity.users_by_d.length(); i++) {
                try {
                    JSONObject usr = data.getJSONObject(i);
                    TextView resultUsr;
                    TextView resultCat;
                    TextView resultData;
                    ImageView img;
                    ImageView nav;

                    LayoutInflater inflater = activity.getLayoutInflater();
                    TableRow tableRow = (TableRow) inflater.inflate(R.layout.result_row, tableLayout, false);

                    int remainder = tableLayout.getChildCount() % 2;
                    if (remainder == 0)
                        tableRow.setBackgroundColor(Color.parseColor("#E7E7E7"));

                    tableLayout.addView(tableRow);
                    tableRow.setId(tableLayout.getChildCount());
                    RelativeLayout content = (RelativeLayout) tableRow.getChildAt(0);
                    resultUsr = (TextView) content.getChildAt(0);
                    resultCat = (TextView) content.getChildAt(1);
                    resultData = (TextView) content.getChildAt(2);
                    nav = (ImageView) content.getChildAt(3);
                    img = (ImageView) content.getChildAt(4);

                    resultUsr.setText(usr.getString("name"));
                    resultCat.setText(usr.getString("category").trim());
                    DecimalFormat precision = new DecimalFormat("0.0");
                    String scoreStrVl = usr.getString("score").trim().equals("null") ? "2.5" : usr.getString("score").trim();
                    double scoreVl = Double.parseDouble(scoreStrVl);
                    double distance = Double.parseDouble(usr.getString("delta").trim());
                    String score = context.getString(R.string.excellent);
                    if (scoreVl <= 4) score = context.getString(R.string.very_good);
                    if (scoreVl <= 3) score = context.getString(R.string.good);
                    if (scoreVl <= 2) score = context.getString(R.string.regular);
                    if (scoreVl <= 1) score = context.getString(R.string.bad);
                    resultData.setText(precision.format(distance) + " Km" + " (" + score + ")");
                    img.setVisibility(View.VISIBLE);
                    nav.setVisibility(View.VISIBLE);
                    tableRow.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                    StartAppActivity.OCore.logException(e.toString());
                }
            }
        }
    }

    private int getIndex(View view) {
        int index = -1;
        TableRow tableRow;
        if (view instanceof ImageView) {
            tableRow = (TableRow) view.getParent().getParent();
        } else if (view instanceof TableRow) {
            tableRow = (TableRow) view;
        } else {
            return -1;
        }

        TableLayout tableLayout = (TableLayout) findViewById(R.id.table_result);
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow tableRowByI = (TableRow) tableLayout.getChildAt(i);
            if (tableRowByI == tableRow) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void setToScore(JSONObject usr) {
        try {
            StartAppActivity.OCore.callback = new CallbackInterface() {
                public void onPostExecute() {
                    String status = null;
                    String msg = null;
                    try {
                        status = StartAppActivity.OCore.response.getString("status");
                        msg = StartAppActivity.OCore.response.getString("msg");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }

                    if ((status != null) && status.equals("err")) {
                        Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);
                    }

                }
            };

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("provider_id", usr.getString("id").trim()));
            params.add(new BasicNameValuePair("category", usr.getString("category").trim()));
            StartAppActivity.OCore.setToScore(params);
        } catch (Exception e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_list);
        StartAppActivity.OCore.OContext = this;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.drawable.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        Intent intent = getIntent();
        final String search = intent.getStringExtra(SearchActivity.SEARCH_VALUE);

        TextView msgTextView = (TextView) findViewById(R.id.msg_text_view_rv_0);
        msgTextView.setText(getString(R.string.wait) + "...");
        msgTextView.setVisibility(View.VISIBLE);

        StartAppActivity.OCore.callback = new CallbackInterface() {
            public void onPostExecute() {
                TextView msgTextView;
                String status;
                String msg;
                ResultListActivity.users_by_d = null;
                ResultListActivity.users_by_s = null;
                try {
                    status = StartAppActivity.OCore.response.getString("status");
                    msg = StartAppActivity.OCore.response.getString("msg");
                    ResultListActivity.users_by_d = StartAppActivity.OCore.response.getJSONObject("data").getJSONArray("closest");
                    ResultListActivity.users_by_s = StartAppActivity.OCore.response.getJSONObject("data").getJSONArray("bestScored");

                    msgTextView = (TextView) findViewById(R.id.msg_text_view_rv_0);
                    msgTextView.setVisibility(View.INVISIBLE);

                    if (status.equals("err")) {
                        Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);

                        msgTextView = (TextView) findViewById(R.id.msg_text_view_rv_1);
                        msgTextView.setVisibility(View.INVISIBLE);

                        msgTextView = (TextView) findViewById(R.id.sort_distance);
                        msgTextView.setVisibility(View.INVISIBLE);

                        msgTextView = (TextView) findViewById(R.id.sort_score);
                        msgTextView.setVisibility(View.INVISIBLE);

                        msgTextView = (TextView) findViewById(R.id.msg_text_view_rv_2);
                        msgTextView.setTypeface(null, Typeface.NORMAL);
                        msgTextView.setText(getString(R.string.no_results) + " " + search + "...");
                        msgTextView.setVisibility(View.VISIBLE);
                        msgTextView.postDelayed(new Runnable() {
                            public void run() {
                                openSearch();
                            }
                        }, 2000);

                    } else {
                        msgTextView = (TextView) findViewById(R.id.msg_text_view_rv_0);
                        msgTextView.setVisibility(View.INVISIBLE);

                        msgTextView = (TextView) findViewById(R.id.msg_text_view_rv_1);
                        if (search.equals(getString(R.string.recent_hits))) {
                            msgTextView.setText("");
                            msgTextView.setVisibility(View.VISIBLE);
                        } else if (search.equals(getString(R.string.hits_around))) {
                            msgTextView.setText("");
                            msgTextView.setVisibility(View.VISIBLE);
                        } else if (search.equals(getString(R.string.top_hits))) {
                            msgTextView.setText("");
                            msgTextView.setVisibility(View.VISIBLE);
                        } else {
                            //String countHit = String.valueOf(ResultListActivity.users_by_d.length());
                            //msgTextView.setText(countHit);
                            msgTextView.setText(getString(R.string.searching_for) + ":");
                            msgTextView.setVisibility(View.VISIBLE);
                        }

                        msgTextView = (TextView) findViewById(R.id.msg_text_view_rv_2);
                        msgTextView.setTypeface(null, Typeface.BOLD);
                        //msgTextView.setText(getString(R.string.hits));
                        msgTextView.setText(search);
                        msgTextView.setVisibility(View.VISIBLE);

                        //msgTextView = (TextView) findViewById(R.id.sort_distance);
                        //msgTextView.setVisibility(View.VISIBLE);

                        //msgTextView = (TextView) findViewById(R.id.sort_score);
                        //msgTextView.setVisibility(View.VISIBLE);
                        ResultListActivity.populateList(ResultListActivity.users_by_d, StartAppActivity.OCore.OContext);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    StartAppActivity.OCore.logException(e.toString());
                }

            }
        };

        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("search", search));
            params.add(new BasicNameValuePair("page", Integer.toString(page)));
            params.add(new BasicNameValuePair("step", Integer.toString(step)));
            if (search.equals(getString(R.string.recent_hits))) {
                StartAppActivity.OCore.getRecentHits(params);
            } else if (search.equals(getString(R.string.hits_around))) {
                StartAppActivity.OCore.getAround(params);
            } else if (search.equals(getString(R.string.top_hits))) {
                StartAppActivity.OCore.getTopHits(params);
            } else {
                StartAppActivity.OCore.search(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }

        MyScrollView scrollView = (MyScrollView) findViewById(R.id.result_scroll);
        scrollView.setOnBottomReachedListener(new MyScrollView.OnBottomReachedListener() {
            public void onBottomReached() {
                nextPage(null);
            }
        });

        TextView dots = (TextView) findViewById(R.id.load_next);
        dots.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        nextPage(null);
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_result_list, menu);

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
                openSearch();
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

    private void openActiveAccount() {
        finish();
        Intent intent = new Intent(this, ActivateAccountActivity.class);
        startActivity(intent);
    }

    public void sort(View view) {
        switch (view.getId()) {
            case R.id.sort_distance:
                ResultListActivity.populateList(ResultListActivity.users_by_d, StartAppActivity.OCore.OContext);
                break;
            case R.id.sort_score:
                ResultListActivity.populateList(ResultListActivity.users_by_s, StartAppActivity.OCore.OContext);
                break;
            default:
                break;
        }
    }

    public void intentDial(View view) {
        int index = this.getIndex(view);
        if (index == -1) return;
        try {
            JSONObject usr = ResultListActivity.users_by_d.getJSONObject(index);
            if (!usr.getString("cell").trim().isEmpty()) {
                this.setToScore(usr);
                String uri = "tel:" + usr.getString("cell").trim();
                if (!usr.getString("alt_number").isEmpty()) {
                    uri = "tel:" + usr.getString("alt_number").trim();
                }
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse(uri));
                startActivity(intent);
            } else {
                Log.d("MSG " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", "Cell number empty");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }
    }

    public void showMap(View view) {
        int index = this.getIndex(view);
        if (index == -1) return;
        try {
            JSONObject usr = ResultListActivity.users_by_d.getJSONObject(index);
            if (!usr.getString("cell").trim().isEmpty()) {
                this.setToScore(usr);
                String uriString = "google.navigation:q=" + usr.getString("latitude") + "," + usr.getString("longitude");
                Uri uri = Uri.parse(uriString);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

            } else {
                Log.d("MSG " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", "Cell number empty");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }
    }

    public void displayAction(final View view) {
        final CharSequence[] items = {getString(R.string.call), getString(R.string.direction)};
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose))
                .setItems(items, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            default:
                            case 0:
                                intentDial(view);
                                break;
                            case 1:
                                showMap(view);
                                break;
                        }
                    }

                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void nextPage(View view) {
        if ((ResultListActivity.users_by_d.length() % step) == 0) {
            if (loadingDialog != null && loadingDialog.isShowing()) { return; }
            loadingDialog = ProgressDialog.show(this, "", getString(R.string.loading) + "...", true);
            Intent intent = getIntent();
            final String search = intent.getStringExtra(SearchActivity.SEARCH_VALUE);
            StartAppActivity.OCore.callback = new CallbackInterface() {
                public void onPostExecute() {
                    page += 1;
                    try {
                        JSONArray tmp = StartAppActivity.OCore.response.getJSONObject("data").getJSONArray("closest");
                        if (tmp != null) {
                            if (tmp.length() < step) {
                                MyScrollView scrollView = (MyScrollView) findViewById(R.id.result_scroll);
                                scrollView.setOnBottomReachedListener(null);
                                TextView loadNext = (TextView) findViewById(R.id.load_next);
                                loadNext.setVisibility(View.INVISIBLE);
                            }

                            for (int i = 0; i < tmp.length(); i++) {
                                ResultListActivity.users_by_d.put(tmp.getJSONObject(i));
                            }
                        } else {
                            MyScrollView scrollView = (MyScrollView) findViewById(R.id.result_scroll);
                            scrollView.setOnBottomReachedListener(null);
                            TextView loadNext = (TextView) findViewById(R.id.load_next);
                            loadNext.setVisibility(View.INVISIBLE);
                        }
                        ResultListActivity.populateList(ResultListActivity.users_by_d, StartAppActivity.OCore.OContext);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                    loadingDialog.dismiss();
                }
            };

            try {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("search", search));
                params.add(new BasicNameValuePair("page", Integer.toString(page + 1)));
                params.add(new BasicNameValuePair("step", Integer.toString(step)));
                if (search.equals(getString(R.string.recent_hits))) {
                    StartAppActivity.OCore.getRecentHits(params);
                } else if (search.equals(getString(R.string.hits_around))) {
                    StartAppActivity.OCore.getAround(params);
                } else if (search.equals(getString(R.string.top_hits))) {
                    StartAppActivity.OCore.getTopHits(params);
                } else {
                    StartAppActivity.OCore.search(params);
                }
            } catch (Exception e) {
                e.printStackTrace();
                StartAppActivity.OCore.logException(e.toString());
            }
        }
    }

    @Override
    public void onBackPressed() {
        openSearch();
    }

}
