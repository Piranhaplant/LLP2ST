package piranha.llp2st.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DownloadsSongListSource extends SongListSource {

    private static final int SONG_LOAD_SIZE = 15;

    private List<String> ids;
    private List<Song> songs = new ArrayList<>();

    @Override
    public List<Song> getSongs() {
        if (ids == null) {
            ids = Downloads.getAllDownloads();
        }
        if (songs.size() == 0) {
            loadSongs();
        }
        return songs;
    }

    @Override
    public boolean loadMore() {
        if (songs.size() >= ids.size()) {
            return false;
        }
        loadSongs();
        return true;
    }

    @Override
    public String getTitle() {
        // This will never be used since this source isn't displayed in a tab
        return "";
    }

    private void loadSongs() {
        int start = songs.size();
        int count = Math.min(SONG_LOAD_SIZE, ids.size() - start);
        final CountDownLatch latch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            final int index = start + i;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SongInfo.get(ids.get(index), false);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // All song info has been downloaded now, so we can put them into the list
        for (int i = 0; i < count; i++) {
            String id = ids.get(start + i);
            Song s;
            try {
                s = SongInfo.get(id, false);
            } catch (Exception ex) {
                // Failed to download the song info. The user probably doesn't have an internet connection, but it's
                // possible that the song was removed from the site or something like that, so still show something.
                s = new Song();
                s.id = id;
                // Song name is stored in the json file, so get that
                s.name = Downloads.getLocalSongName(id);
            }
            songs.add(s);
        }
    }
}
