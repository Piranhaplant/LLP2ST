package piranha.llp2st.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import piranha.llp2st.Util;
import piranha.llp2st.exception.LLPException;

public class ApiSongListSource extends SongListSource {

    private List<Song> songs = new ArrayList<>();
    private String title;
    private String apiUrl;
    private int availableCount;

    public ApiSongListSource(String apiUrl, String title) {
        this.apiUrl = apiUrl;
        this.title = title;
    }

    @Override
    public List<Song> getSongs() {
        if (songs.size() == 0) {
            loadSongs();
        }
        return songs;
    }

    @Override
    public boolean loadMore() {
        if (songs.size() >= availableCount) {
            return false;
        }
        loadSongs();
        return true;
    }

    @Override
    public String getTitle() {
        return title;
    }

    private void loadSongs() {
        String url = apiUrl + "&offset=" + songs.size();
        if (Login.isLoggedIn()) {
            url += "&" + Login.getURLParams();
        }
        try {
            JSONObject j = new JSONObject(Util.download(url));
            LLPException.ThrowIfError(j);
            JSONObject content = j.getJSONObject("content");
            availableCount = content.getInt("count");

            JSONArray items = content.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                songs.add(SongInfo.set(items.getJSONObject(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Probably a network error. Stop trying to load any more songs
            availableCount = songs.size();
        }
    }
}
