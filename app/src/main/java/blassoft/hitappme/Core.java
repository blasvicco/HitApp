package blassoft.hitappme;

import android.content.Context;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import blassoft.hitappme.Util;

//define callback interface
interface CallbackInterface {
    void onPostExecute();
}

class MyTaskHandler extends AsyncTask<Core, Void, Core> {

    @Override
    protected void onPostExecute(Core obj) {
        if (obj.callback != null) {
            obj.callback.onPostExecute();
        }
        obj.urlToRequest = "";
        obj.method = "";
        obj.params.clear();
    }

    @Override
    protected Core doInBackground(Core... obj) {
        // params comes from the execute() call: params[0] is the url.
        try {
            obj[0].response = obj[0].request(obj[0].urlToRequest, obj[0].method, obj[0].params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj[0];
    }
}

public class Core {
    static {
        //for localhost testing only
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                            return hostname.equals(Util.hostName);
                    }
                });
    }

    public String id;
    private String key;
    private String hashId;
    public String cell;
    public String imei;
    public String simSerialNumber;
    public String userStatus;

    public boolean fixLocation = false;
    public final boolean loadFirstLogin = true;
    public boolean loadSetLocation = true;
    public String location = "";

    private String status;
    private String msg;
    public JSONObject response;

    public JSONObject user;
    public JSONArray categories;

    public String urlToRequest;
    public CallbackInterface callback;
    public String method;
    public List<NameValuePair> params;

    public Context OContext;

    public NetworkInfo networkInfo;

    public LocationHandler locationHandler;

    static private MyTaskHandler taskRunning;
    static private Util OUtil;

    private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (NameValuePair pair : params) {
            if (first)
                first = false;
            else
                result.append("&");

            if (pair.getValue() != null) {
                result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            }
        }
        return result.toString();
    }

    private void setHeader(List<NameValuePair> paramsInput) throws UnsupportedEncodingException {
        String sid = String.valueOf(System.currentTimeMillis() / 1000L);
        byte[] data;
        data = (hashId + sid).getBytes("UTF-8");
        paramsInput.add(new BasicNameValuePair("hashid", OUtil.encode(data)));

        data = sid.getBytes("UTF-8");
        paramsInput.add(new BasicNameValuePair("sid", OUtil.encode(data)));
    }

    public Core() {
        OUtil = new Util();
        taskRunning = new MyTaskHandler();
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    public JSONObject request(String myUrl, String method, List<NameValuePair> params) throws IOException, JSONException {
        InputStream is = null;
        response = new JSONObject();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            URL url = new URL(myUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            //HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            Certificate ca = null;
            InputStream caInput = new BufferedInputStream(OContext.getResources().openRawResource(R.raw.ssl));
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                ca = cf.generateCertificate(caInput);
                //System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } catch (CertificateException e) {
                e.printStackTrace();
                return new JSONObject().put("status", "err").put("msg", "No connection available.").put("data", new Object());
            } finally {
                caInput.close();
            }

            // Create a KeyStore containing our trusted CAs
            KeyStore keyStore;
            try {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setCertificateEntry("ca", ca);
            } catch (KeyStoreException e) {
                e.printStackTrace();
                return new JSONObject().put("status", "err").put("msg", "No connection available.").put("data", new Object());
            } catch (CertificateException e) {
                e.printStackTrace();
                return new JSONObject().put("status", "err").put("msg", "No connection available.").put("data", new Object());
            }

            // Create a TrustManager that trusts the CAs in our KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            try {
                tmf.init(keyStore);
            } catch (KeyStoreException e) {
                e.printStackTrace();
                return new JSONObject().put("status", "err").put("msg", "No connection available.").put("data", new Object());
            }

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            try {
                context.init(null, tmf.getTrustManagers(), null);
            } catch (KeyManagementException e) {
                e.printStackTrace();
                return new JSONObject().put("status", "err").put("msg", "No connection available.").put("data", new Object());
            }

            //conn.setDefaultSSLSocketFactory(context.getSocketFactory());
            conn.setSSLSocketFactory(context.getSocketFactory());
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod(method);
            conn.setDoInput(true);
            conn.setDoOutput(false);
            if (method.equals("POST")) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery(params));
                writer.flush();
                writer.close();
                os.close();
            }

            // Starts the query
            conn.connect();
            int status = conn.getResponseCode();
            is = (status >= HttpStatus.SC_BAD_REQUEST) ? conn.getErrorStream() : conn.getInputStream();
            //int response = conn.getResponseCode();
            //Log.d(mContext.DEBUG_TAG, "The response is: " + response);
            //is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is);
            response = new JSONObject(contentAsString);
            if ((response.getString("status").equals("err")) && (response.getString("msg").equals("Session expired. Request new key."))) {
                //clear the hashid to request a new one
                hashId = "";
            }

        } catch (IOException e) {
            e.printStackTrace();
            return new JSONObject().put("status", "err").put("msg", "No connection available.").put("data", new Object());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new JSONObject().put("status", "err").put("msg", "No connection available.").put("data", new Object());
        } finally {
            if (is != null) is.close();
        }

        return response;
    }

    private String readIt(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return new String(total);
    }

    public void getKey() throws IOException, JSONException {
        params = new ArrayList<>();
        request(Util.baseAPIUrl + "getKey", "GET", params);
        if (response.has("status")) {
            status = response.getString("status");
            msg = response.getString("msg");
            if (!status.equals("err")) {
                key = response.getJSONObject("data").getString("tk");
            } else {
                Log.d(status, msg);
            }
        }
    }

    public void login() throws IOException, JSONException {
        params = new ArrayList<>();
        String sid = String.valueOf(System.currentTimeMillis() / 1000L);

        //encode
        hashId = key + sid + id;
        byte[] data;

        data = hashId.getBytes("UTF-8");
        params.add(new BasicNameValuePair("hashid", OUtil.encode(data)));

        data = sid.getBytes("UTF-8");
        params.add(new BasicNameValuePair("sid", OUtil.encode(data)));

        request(Util.baseAPIUrl + "setHashid", "POST", params);

        status = response.getString("status");
        msg = response.getString("msg");

        if (!status.equals("err")) {
            userStatus = response.getJSONObject("data").getString("user_status");
            fixLocation = response.getJSONObject("data").getBoolean("fix_location");

            if (cell.isEmpty()) cell = response.getJSONObject("data").getString("cell");
            hashId = OUtil.decode(response.getJSONObject("data").getString("hashid"));
            sid = response.getJSONObject("data").getString("sid");
            hashId = hashId.replace(sid, "");
        } else if (msg.equals("User does not exist")) {
            //signIn
            params.add(new BasicNameValuePair("id", id));
            params.add(new BasicNameValuePair("cell", cell));
            params.add(new BasicNameValuePair("name", null));
            if (locationHandler.currentBestLocation != null) {
                String lat = String.valueOf(locationHandler.currentBestLocation.getLatitude());
                String lon = String.valueOf(locationHandler.currentBestLocation.getLongitude());
                params.add(new BasicNameValuePair("latitude", lat));
                params.add(new BasicNameValuePair("longitude", lon));
                if (locationHandler.address != null) {
                    params.add(new BasicNameValuePair("area", locationHandler.address.getAdminArea()));
                    params.add(new BasicNameValuePair("country", locationHandler.address.getCountryCode()));
                    params.add(new BasicNameValuePair("locality", locationHandler.address.getLocality()));
                    params.add(new BasicNameValuePair("sub_area", locationHandler.address.getSubAdminArea()));
                    params.add(new BasicNameValuePair("zipcode", locationHandler.address.getPostalCode()));
                }
            }

            request(Util.baseAPIUrl + "signIn", "POST", params);
            status = response.getString("status");
            msg = response.getString("msg");

            if (!status.equals("err")) {
                login();
            } else {
                Log.d(status, msg);
            }
        }
    }

    private boolean checkConnection() throws IOException, JSONException {
        if (networkInfo != null && networkInfo.isConnected()) {
            if (hashId == null) {
                getKey();
            }
            return true;
        } else {
            return false;
        }
    }

    public void setLocation(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", R.string.no_connection);
            return;
        }

        setHeader(paramsInput);

        if (locationHandler.currentBestLocation == null) return;

        String lat = String.valueOf(locationHandler.currentBestLocation.getLatitude());
        String lon = String.valueOf(locationHandler.currentBestLocation.getLongitude());
        paramsInput.add(new BasicNameValuePair("latitude", lat));
        paramsInput.add(new BasicNameValuePair("longitude", lon));

        if (locationHandler.address != null) {
            paramsInput.add(new BasicNameValuePair("area", locationHandler.address.getAdminArea()));
            paramsInput.add(new BasicNameValuePair("country", locationHandler.address.getCountryCode()));
            paramsInput.add(new BasicNameValuePair("locality", locationHandler.address.getLocality()));
            paramsInput.add(new BasicNameValuePair("sub_area", locationHandler.address.getSubAdminArea()));
            paramsInput.add(new BasicNameValuePair("zipcode", locationHandler.address.getPostalCode()));
            paramsInput.add(new BasicNameValuePair("thoroughfare", locationHandler.address.getAddressLine(0)));
        }

        urlToRequest = Util.baseAPIUrl + "setLocation";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    void getCategories(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", R.string.no_connection);
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "getCategories";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void search(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", R.string.no_connection);
            return;
        }

        setHeader(paramsInput);

        if (locationHandler.currentBestLocation != null) {
            paramsInput.add(new BasicNameValuePair("latitude", String.valueOf(locationHandler.currentBestLocation.getLatitude())));
            paramsInput.add(new BasicNameValuePair("longitude", String.valueOf(locationHandler.currentBestLocation.getLongitude())));
        }

        urlToRequest = Util.baseAPIUrl + "search";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void getRecentHits(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", R.string.no_connection);
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "getRecentHits";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void getAround(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", R.string.no_connection);
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "getAround";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void getTopHits(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", R.string.no_connection);
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "getTopHits";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void setToScore(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", R.string.no_connection);
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "setToScore";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void getPendingScore(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", "No connection detected.");
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "getPendingScore";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void setScore(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", "No connection detected.");
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "setScore";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void deleteScore(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", "No connection detected.");
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "ignoreScore";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void getSettings() throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", R.string.no_connection);
            return;
        }

        params = new ArrayList<>();
        setHeader(params);

        urlToRequest = Util.baseAPIUrl + "getSettings";
        method = "POST";
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void setSettings(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", R.string.no_connection);
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "setSettings";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void setNewActivationCode(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", "No connection detected.");
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "setNewActivationCode";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void activateUser(List<NameValuePair> paramsInput) throws IOException, JSONException {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        if (!checkConnection()) {
            response = new JSONObject().put("status", "err").put("msg", "No connection detected.");
            return;
        }

        setHeader(paramsInput);

        urlToRequest = Util.baseAPIUrl + "activateUser";
        method = "POST";
        params = paramsInput;
        taskRunning = new MyTaskHandler();
        taskRunning.execute(this, null, this);
    }

    public void logException(String err) {
        if (taskRunning.getStatus() == AsyncTask.Status.RUNNING) {
            taskRunning.cancel(true);
        }

        try {
            if (!checkConnection()) {
                response = new JSONObject().put("status", "err").put("msg", "No connection detected.");
                return;
            }

            List<NameValuePair> paramsInput = new ArrayList<>();
            paramsInput.add(new BasicNameValuePair("user_id", id));
            paramsInput.add(new BasicNameValuePair("log", err));

            setHeader(paramsInput);

            urlToRequest = Util.baseAPIUrl + "logException";
            method = "POST";
            params = paramsInput;
            callback = null;
            taskRunning = new MyTaskHandler();
            taskRunning.execute(this, null, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFullAddressFromUser() {
        String fullAddress = "";
        try {
            fullAddress = this.user.getString("country");
            if (!this.user.getString("area").equals("null") && !this.user.getString("area").isEmpty())
                fullAddress += " - " +this.user.getString("area");

            if (!this.user.getString("locality").equals("null") && !this.user.getString("locality").isEmpty())
                fullAddress += " - " +this.user.getString("locality");

            if (!this.user.getString("sub_area").equals("null") && !this.user.getString("sub_area").isEmpty())
                fullAddress += " - " +this.user.getString("sub_area");

            if (!this.user.getString("zipcode").equals("null") && !this.user.getString("zipcode").isEmpty())
                fullAddress += " - " +this.user.getString("zipcode");

            if (!this.user.getString("thoroughfare").equals("null") && !this.user.getString("thoroughfare").isEmpty())
                fullAddress += " ~ " +this.user.getString("thoroughfare");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return fullAddress;
    }

    public String getFullAddressFromLocationHandler() {
        String fullAddress = "";
        if (StartAppActivity.OCore.locationHandler.address != null) {
            fullAddress = StartAppActivity.OCore.locationHandler.address.getCountryCode();
            if (StartAppActivity.OCore.locationHandler.address.getAdminArea() != null && !StartAppActivity.OCore.locationHandler.address.getAdminArea().isEmpty())
                fullAddress += " - " +StartAppActivity.OCore.locationHandler.address.getAdminArea();

            if (StartAppActivity.OCore.locationHandler.address.getLocality() != null && !StartAppActivity.OCore.locationHandler.address.getLocality().isEmpty())
                fullAddress += " - " +StartAppActivity.OCore.locationHandler.address.getLocality();

            if (StartAppActivity.OCore.locationHandler.address.getSubAdminArea() != null && !StartAppActivity.OCore.locationHandler.address.getSubAdminArea().isEmpty())
                fullAddress += " - " +StartAppActivity.OCore.locationHandler.address.getSubAdminArea();

            if (StartAppActivity.OCore.locationHandler.address.getPostalCode() != null && !StartAppActivity.OCore.locationHandler.address.getPostalCode().isEmpty())
                fullAddress += " - " +StartAppActivity.OCore.locationHandler.address.getPostalCode();

            if (StartAppActivity.OCore.locationHandler.address.getThoroughfare() != null && !StartAppActivity.OCore.locationHandler.address.getThoroughfare().isEmpty())
                fullAddress += " ~ " +StartAppActivity.OCore.locationHandler.address.getAddressLine(0);
        }
        return fullAddress;
    }
}
