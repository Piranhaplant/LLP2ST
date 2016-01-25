package piranha.llp2st.data;

import org.json.JSONObject;

public class Comment {

    public String content;
    public String date;
    public int id;

    public String userName;
    public String userPictureUrl;
    public int userId;

    public Comment() { }

    public Comment(JSONObject json) {
        content = json.optString("content");
        date = json.optString("comment_date");
        date = date.replace("T", " ");
        id = json.optInt("id");

        JSONObject user = json.optJSONObject("user");
        if (user != null) {
            userName = user.optString("username");
            userPictureUrl = user.optString("avatar_path");
            userId = user.optInt("id");
        }
    }
}
