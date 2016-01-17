package piranha.llp2st.data;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import piranha.llp2st.Util;
import piranha.llp2st.exception.LLPException;
import piranha.llp2st.view.MainActivity;

public final class Login {

    public static String cookie;
    public static String uid;
    public static String username;
    public static String token;

    public static void login(String user, String password) throws JSONException, IOException, LLPException {
        JSONObject params = new JSONObject();
        params.put("username", user);
        params.put("password", hashPassword(password));
        JSONObject json = new JSONObject(Util.post("https://m.tianyi9.com/API/login", params.toString()));
        LLPException.ThrowIfError(json);

        JSONObject content = json.getJSONObject("content");
        cookie = content.getString("logincookie");
        uid = String.valueOf(content.getInt("uid"));
        username = content.getString("username");
        token = content.getString("token");
        saveLoginInfo();
    }

    public static void logout() {
        cookie = null;
        uid = null;
        username = null;
        token = null;
        saveLoginInfo();
    }

    public static boolean isLoggedIn() {
        return cookie != null;
    }

    public static String getURLParams() {
        return "logincookie=" + cookie
            + "&timestamp=" + String.valueOf(new Date().getTime());
    }

    public static String getXSign(String params) {
        try {
            return sha256(params + token);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void checkLogin() {
        loadLoginInfo();
        if (!isLoggedIn())
            return;
        try {
            String result = Util.download("https://m.tianyi9.com/API/checkLogin?" + getURLParams());
            JSONObject json = new JSONObject(result);
            if (!json.getBoolean("succeed"))
                logout();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String hashPassword(String password) {
        try {
            return sha256("as_8^yg8*R" + password);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String sha256(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(s.getBytes("UTF-8"));
        byte[] digest = md.digest();
        return String.format("%064x", new java.math.BigInteger(1, digest));
    }

    private static void loadLoginInfo() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.ctx);
        cookie = prefs.getString("login_cookie", null);
        uid = prefs.getString("login_uid", null);
        username = prefs.getString("login_username", null);
        token = prefs.getString("login_token", null);
    }

    private static void saveLoginInfo() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.ctx);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("login_cookie", cookie);
        edit.putString("login_uid", uid);
        edit.putString("login_username", username);
        edit.putString("login_token", token);
        edit.commit();
    }
}
