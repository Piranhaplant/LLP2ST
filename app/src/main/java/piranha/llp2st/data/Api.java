package piranha.llp2st.data;

import android.net.Uri;

public final class Api {

    public static final String BASE_URL = "https://m.tianyi9.com/";
    public static final String URL = BASE_URL + "API/";
    public static final String UPLOAD_URL = BASE_URL + "upload/";

    public static String getPictureUrl(String url) {
        if (url == null || url.equals("")) {
            return BASE_URL + "images/default_avatar.jpg";
        } else {
            return UPLOAD_URL + url;
        }
    }
}
