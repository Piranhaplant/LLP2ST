package piranha.llp2st.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public abstract class SongListSource {

    public abstract List<Song> getSongs();
    /**
     * Requests that more songs be loaded from the source
     * @return true if there are more songs to load, otherwise false
     */
    public abstract boolean loadMore();
    public abstract String getTitle();
    public abstract SongListSource clone();

    public static SongListSource getNewSongListSource() {
        return new ApiSongListSource("https://m.tianyi9.com/API/getlivelist?type=public", "New");
    }

    public static SongListSource getFeaturedSongListSource() {
        return new ApiSongListSource("https://m.tianyi9.com/API/getlivelist?type=featured", "Featured");
    }

    public static SongListSource getCategorySongListSource(String category, int id) {
        return new ApiSongListSource("https://m.tianyi9.com/API/getlivelist?type=category&category=" + id, category);
    }

    public static SongListSource getSearchSongListSource(String search) {
        try {
            return new ApiSongListSource("https://m.tianyi9.com/API/search?keyword=" + URLEncoder.encode(search, "UTF-8"), "Search");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SongListSource getUserSongListSource(int uid) {
        return new ApiSongListSource("https://m.tianyi9.com/API/getlivelist?type=user_public&uid=" + Integer.toString(uid), "");
    }
}
