package piranha.llp2st.view;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import piranha.llp2st.R;
import piranha.llp2st.data.DownloadsSongListSource;

public class DownloadsActivity extends PlaySongActivity {

    private MenuItem stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        Toolbar toolbar = (Toolbar)findViewById(R.id.fragment_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SongListFragment fragment = new SongListFragment();
        fragment.setSongSource(new DownloadsSongListSource());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_content, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions_songlist, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        stopButton = menu.findItem(R.id.action_stop);
        refreshStopButton();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_stop:
                PlaySongActivity.Stop();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    void setStopVisible(boolean visible) {
        if (stopButton != null) {
            stopButton.setVisible(visible);
        }
    }
}
