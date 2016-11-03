package piranha.llp2st.data;

import org.json.JSONObject;

public class Comment {

    public String content;
    public String date;
    public int id;
    // Will only have name, avatar, and id
    public User user;

    public Comment() { }

    public Comment(JSONObject json) {
        content = json.optString("content");
        date = json.optString("comment_date");
        date = date.replace("T", " ");
        id = json.optInt("id");
        user = new User(json.optJSONObject("user"));
    }
}
