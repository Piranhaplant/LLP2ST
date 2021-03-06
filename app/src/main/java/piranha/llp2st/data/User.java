package piranha.llp2st.data;

import org.json.JSONObject;

public class User {
    public int id;
    public String name = "";
    public String avatar = "";
    public int comments;
    public int posts;
    public int clicks;
    public String location = "";
    public String lastLogin = "";

    public User() { }

    public User(JSONObject json) {
        if (json == null) return;

        id = json.optInt("id");
        name = json.optString("username");
        if (json.isNull("avatar_path")) {
            avatar = null;
        } else {
            avatar = json.optString("avatar_path").trim();
        }
        comments = json.optInt("total_comment");
        posts = json.optInt("post_count");
        clicks = json.optInt("total_click");
        location = json.optString("location");
        lastLogin = json.optString("lastlogin");
    }
}
