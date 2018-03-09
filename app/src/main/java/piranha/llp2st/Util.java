package piranha.llp2st;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import piranha.llp2st.data.Login;

public final class Util {

    private static InputStream downloadStream(String url) throws IOException {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection)u.openConnection();
        Login.appendConnectionParams(conn, url);
        conn.setRequestMethod("GET");

        InputStream is;
        if (conn.getResponseCode() >= 400)
            return conn.getErrorStream();
        else
            return conn.getInputStream();
    }

    public static String download(String url) throws IOException {
        InputStream is = downloadStream(url);

        String result = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
        is.close();

        //android.util.Log.i("GET", url + " : " + result);
        return result;
    }

    public static byte[] downloadBytes(String url) throws IOException {
        InputStream is = downloadStream(url);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[16384];

        while (true) {
            int nRead = is.read(data, 0, data.length);
            if (nRead == -1) break;
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    public static String post(String url, String data) throws IOException {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection)u.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", String.valueOf(data.length()));
        Login.appendConnectionParams(conn, url);
        OutputStream os = conn.getOutputStream();
        os.write(data.getBytes());

        InputStream is;
        if (conn.getResponseCode() >= 400)
            is = conn.getErrorStream();
        else
            is = conn.getInputStream();

        String result = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
        is.close();

        //android.util.Log.i("POST", url + " : " + data + " : " + result);
        return result;
    }

    public static String readFile(File f) throws IOException {
        FileInputStream s = new FileInputStream(f);
        String out = new Scanner(s, "UTF-8").useDelimiter("\\A").next();
        s.close();
        return out;
    }
}
