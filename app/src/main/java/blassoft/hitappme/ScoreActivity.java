package blassoft.hitappme;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import blassoft.hitappme.util.MyScrollView;

public class ScoreActivity extends ActionBarActivity {
    private static JSONArray scores;
    private int page = 1;
    private final int step = 5;
    private ProgressDialog loadingDialog;

    private static void populateList(JSONArray data, Context context) {
        Activity activity = (Activity) context;
        try {
            TableLayout tableLayout = (TableLayout) activity.findViewById(R.id.table_score);
            for (int i = tableLayout.getChildCount(); i < data.length(); i++) {
                JSONObject score = data.getJSONObject(i);
                TextView userToScore;
                TextView catToScore;
                LayoutInflater inflater = activity.getLayoutInflater();
                TableRow row = (TableRow) inflater.inflate(R.layout.score_row, tableLayout, false);
                int remainder = tableLayout.getChildCount() % 2;
                if (remainder == 0)
                    row.setBackgroundColor(Color.parseColor("#E7E7E7"));

                tableLayout.addView(row);
                row.setId(tableLayout.getChildCount());
                RelativeLayout content = (RelativeLayout) row.getChildAt(0);
                userToScore = (TextView) content.getChildAt(0);
                catToScore = (TextView) content.getChildAt(1);
                userToScore.setText(score.getString("name").trim());
                catToScore.setText(score.getString("category").trim());
                row.setVisibility(View.VISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int getIndex(View view) {
        int index = -1;
        TableRow tableRow = (TableRow) view.getParent().getParent();
        TableLayout tableLayout = (TableLayout) findViewById(R.id.table_score);
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow tableRowByI = (TableRow) tableLayout.getChildAt(i);
            if (tableRowByI == tableRow) {
                index = i;
                break;
            }
        }
        return index;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);
        StartAppActivity.OCore.OContext = this;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.drawable.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        try {
            StartAppActivity.OCore.callback = new CallbackInterface() {
                public void onPostExecute() {
                    String status;
                    String msg;
                    ScoreActivity.scores = null;
                    try {
                        status = StartAppActivity.OCore.response.getString("status");
                        msg = StartAppActivity.OCore.response.getString("msg");
                        ScoreActivity.scores = StartAppActivity.OCore.response.getJSONObject("data").getJSONArray("scores");

                        if (status.equals("err")) {
                            Log.d(status + " " + this.getClass().getName() + this.getClass().getEnclosingMethod().getName() + ": ", msg);
                        } else {
                            TextView msgTextView = (TextView) findViewById(R.id.msg_text_view_sc);
                            msgTextView.setText("");

                            if (ScoreActivity.scores.length() <= 0) {
                                msgTextView.setText(getString(R.string.no_pending_score) + "...");
                                msgTextView.setTextSize(18);
                                //msgTextView.setTypeface(null, Typeface.BOLD);
                                msgTextView.setTextColor(Color.parseColor("#073763"));
                                msgTextView.postDelayed(new Runnable() {
                                    public void run() {
                                        openSearch();
                                    }
                                }, 2000);
                            } else {
                                msgTextView.setText(getString(R.string.score_the_contact));
                                msgTextView.setTextSize(18);
                                msgTextView.setTypeface(null, Typeface.BOLD);
                                msgTextView.setTextColor(Color.parseColor("#073763"));
                                ScoreActivity.populateList(ScoreActivity.scores, StartAppActivity.OCore.OContext);
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            };

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("page", Integer.toString(page)));
            params.add(new BasicNameValuePair("step", Integer.toString(step)));
            StartAppActivity.OCore.getPendingScore(params);
        } catch (Exception e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }

        MyScrollView scrollView = (MyScrollView) findViewById(R.id.score_scroll);
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
        inflater.inflate(R.menu.menu_score, menu);

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
                //openScore();
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

    public void saveScore(View view) {
        int index = this.getIndex(view);
        if (index == -1) return;

        TableRow tableRow = (TableRow) view.getParent().getParent();
        RelativeLayout content = (RelativeLayout) tableRow.getChildAt(0);
        RatingBar score = (RatingBar) content.getChildAt(2);

        loadingDialog = ProgressDialog.show(this, "", getString(R.string.loading) + "...", true);
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("score_id", String.valueOf(ScoreActivity.scores.getJSONObject(index).getString("id"))));
            params.add(new BasicNameValuePair("score", String.valueOf(score.getRating())));
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

                        loadingDialog.dismiss();
                        finish();
                        startActivity(getIntent());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            };
            StartAppActivity.OCore.setScore(params);
        } catch (Exception e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }
    }

    public void deleteScore(View view) {
        int index = this.getIndex(view);
        if (index == -1) return;

        loadingDialog = ProgressDialog.show(this, "", getString(R.string.loading) + "...", true);
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("score_id", String.valueOf(ScoreActivity.scores.getJSONObject(index).getString("id"))));
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

                        loadingDialog.dismiss();
                        finish();
                        startActivity(getIntent());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                }
            };
            StartAppActivity.OCore.deleteScore(params);
        } catch (Exception e) {
            e.printStackTrace();
            StartAppActivity.OCore.logException(e.toString());
        }
    }

    public void nextPage(View view) {
        if ((ScoreActivity.scores.length() % step) == 0) {
            if (loadingDialog != null && loadingDialog.isShowing()) { return; }
            loadingDialog = ProgressDialog.show(this, "", getString(R.string.loading) + "...", true);
            StartAppActivity.OCore.callback = new CallbackInterface() {
                public void onPostExecute() {
                    page += 1;
                    try {
                        JSONArray tmp = StartAppActivity.OCore.response.getJSONObject("data").getJSONArray("scores");
                        if (tmp != null) {
                            if (tmp.length() < step) {
                                MyScrollView scrollView = (MyScrollView) findViewById(R.id.score_scroll);
                                scrollView.setOnBottomReachedListener(null);
                                TextView loadNext = (TextView) findViewById(R.id.load_next);
                                loadNext.setVisibility(View.INVISIBLE);
                            }

                            for (int i = 0; i < tmp.length(); i++) {
                                ScoreActivity.scores.put(tmp.getJSONObject(i));
                            }
                        } else {
                            MyScrollView scrollView = (MyScrollView) findViewById(R.id.score_scroll);
                            scrollView.setOnBottomReachedListener(null);
                            TextView loadNext = (TextView) findViewById(R.id.load_next);
                            loadNext.setVisibility(View.INVISIBLE);
                        }
                        ScoreActivity.populateList(ScoreActivity.scores, StartAppActivity.OCore.OContext);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        StartAppActivity.OCore.logException(e.toString());
                    }
                    loadingDialog.dismiss();
                }
            };

            try {
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("page", Integer.toString(page + 1)));
                params.add(new BasicNameValuePair("step", Integer.toString(step)));
                StartAppActivity.OCore.getPendingScore(params);
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
