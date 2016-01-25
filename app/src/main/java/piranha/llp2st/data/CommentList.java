package piranha.llp2st.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import piranha.llp2st.Util;
import piranha.llp2st.exception.LLPException;

public class CommentList {

    public List<Comment> comments = new ArrayList<>();
    public int availableCount;
    public String songId;

    public CommentList(String songId) {
        this.songId = songId;
    }

    public void loadComments() throws IOException, JSONException, LLPException {
        String url = "https://m.tianyi9.com/API/getcomments?live_id=" + songId + "&offset=" + comments.size();
        if (Login.isLoggedIn()) {
            url += "&" + Login.getURLParams();
        }
        JSONObject j = new JSONObject(Util.download(url));
        LLPException.ThrowIfError(j);
        JSONObject content = j.getJSONObject("content");
        availableCount = content.getInt("count");

        JSONArray items = content.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            comments.add(new Comment(items.getJSONObject(i)));
        }
    }
}
