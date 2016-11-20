package piranha.llp2st.data;

import org.json.JSONObject;

public class Song {

    public static final String KeyId = "live_id";

    /**
     * Sets whether all of the song data is available. Song info loaded from a live
     * list won't contain everything needed, so the full live info will need to be
     * loaded in order to download the song.
     */
    public boolean complete;

    public String id;
    public String name;
    public String artist;
    public int difficulty;
    public int clickCount;
    public String description;
    public boolean memberOnly;

    public String pictureUrl;
    public String mapUrl;
    public String audioUrl;
    // Will only have name, avatar, id, and posts
    public User user;

    public Song() { }
    public Song(JSONObject j) {
        loadJson(j);
    }

    public void loadJson(JSONObject j) {
        complete = j.has("map_path");

        id = j.optString(KeyId);
        name = j.optString("live_name");
        artist = j.optString("artist");
        difficulty = j.optInt("level");
        clickCount = j.optInt("click_count");
        description = j.optString("live_info");
        memberOnly = j.optBoolean("memberonly");

        pictureUrl = j.optString("cover_path");
        mapUrl = j.optString("map_path");
        audioUrl = j.optString("bgm_path");

        user = new User(j.optJSONObject("upload_user"));
    }
}
