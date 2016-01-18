package piranha.llp2st.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import piranha.llp2st.R;
import piranha.llp2st.data.SongListSource;

public class UserActivity extends PlaySongActivity {

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_USER_NAME = "user_name";
    private static final String FRAGMENT_DATA = "data";

    private MenuItem stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        Intent intent = getIntent();
        int userId = intent.getIntExtra(EXTRA_USER_ID, 0);
        String userName = intent.getStringExtra(EXTRA_USER_NAME);

        Toolbar toolbar = (Toolbar)findViewById(R.id.fragment_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(userName);

        FragmentManager fm = getSupportFragmentManager();
        SongListFragment frag = (SongListFragment)fm.findFragmentByTag(FRAGMENT_DATA);

        if (frag == null) {
            frag = new SongListFragment();
            frag.setSongSource(SongListSource.getUserSongListSource(userId));
            fm.beginTransaction().replace(R.id.fragment_content, frag, FRAGMENT_DATA).commit();
        }
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
    void setStopVisible(boolean visible) {
        if (stopButton != null) {
            stopButton.setVisible(visible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_stop:
                PlaySongActivity.Stop();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
