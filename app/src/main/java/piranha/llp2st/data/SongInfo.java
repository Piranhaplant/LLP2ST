package piranha.llp2st.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import piranha.llp2st.Util;
import piranha.llp2st.exception.LLPException;

public final class SongInfo {

    private static Map<String, Song> cache = new HashMap<>();

    public static Song get(String id, boolean requireComplete) throws IOException, JSONException, LLPException {
        // Get cached info if available
        Song s;
        synchronized (cache) {
            s = cache.get(id);
        }
        if (s != null && (s.complete || !requireComplete)) {
            return s;
        }
        // Otherwise, download the info
        String url = Api.URL + "getlive?live_id=" + id;
        url = Login.appendURLParams(url);
        String text = Util.download(url);
        if (text.equals("Not found")) {
            throw new LLPException("Live not found", -1);
        }
        JSONObject j = new JSONObject(text);
        LLPException.ThrowIfError(j);
        j = j.getJSONObject("content");
        return set(j, s);
    }

    public static Song set(JSONObject j) {
        Song s = null;
        if (j.has(Song.KeyId)) {
            String id = j.optString(Song.KeyId);
            synchronized (cache) {
                s = cache.get(id);
            }
        }
        return set(j, s);
    }

    public static Song set(JSONObject j, Song s) {
        if (s == null) {
            s = new Song(j);
            synchronized (cache) {
                cache.put(s.id, s);
            }
        } else if (!s.complete) {
            s.loadJson(j);
        }
        return s;
    }
}
