package piranha.llp2st.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import piranha.llp2st.R;
import piranha.llp2st.data.SongListSource;
import piranha.llp2st.data.User;
import piranha.llp2st.exception.ErrorOr;

public class UserActivity extends BaseActivity implements UserDataFragment.DataCallbacks {

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_USER_NAME = "user_name";
    private static final String FRAGMENT_LIST = "list";
    private static final String FRAGMENT_DATA = "data";

    private UserDataFragment dataFragment;
    private MenuItem stopButton;
    private ErrorOr<User> user;

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
        if (userName != null) {
            getSupportActionBar().setTitle(userName);
        } else {
            getSupportActionBar().setTitle(R.string.loading);
        }

        FragmentManager fm = getSupportFragmentManager();
        SongListFragment frag = (SongListFragment)fm.findFragmentByTag(FRAGMENT_LIST);

        if (frag == null) {
            frag = new SongListFragment();
            frag.setSongSource(SongListSource.getUserSongListSource(userId));
            fm.beginTransaction().replace(R.id.fragment_content, frag, FRAGMENT_LIST).commit();
        }

        dataFragment = (UserDataFragment)fm.findFragmentByTag(FRAGMENT_DATA);

        if (dataFragment == null) {
            dataFragment = new UserDataFragment();
            fm.beginTransaction().add(dataFragment, FRAGMENT_DATA).commit();
        }

        user = dataFragment.GetUser();
        if (user == null) {
            dataFragment.LoadUser(userId);
        } else {
            ShowUser();
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
                BaseActivity.Stop();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void UserLoaded(ErrorOr<User> user) {
        this.user = user;
        ShowUser();
    }

    private void ShowUser() {
        if (user == null) return;

        if (user.isError()) {

            return;
        }

        User u = user.data;
        getSupportActionBar().setTitle(u.name);
    }
}
