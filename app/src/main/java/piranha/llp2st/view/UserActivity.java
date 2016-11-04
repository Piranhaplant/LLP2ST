package piranha.llp2st.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import piranha.llp2st.R;
import piranha.llp2st.data.SongListSource;
import piranha.llp2st.data.User;
import piranha.llp2st.exception.ErrorOr;

public class UserActivity extends BaseActivity implements UserDataFragment.DataCallbacks, SongListFragment.Callbacks {

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_USER_NAME = "user_name";
    private static final String FRAGMENT_LIST = "list";
    private static final String FRAGMENT_DATA = "data";

    private UserDataFragment dataFragment;
    private SongListFragment songFragment;
    private MenuItem stopButton;
    private int userId;
    private ErrorOr<User> user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        Intent intent = getIntent();
        userId = intent.getIntExtra(EXTRA_USER_ID, 0);
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
        songFragment = (SongListFragment)fm.findFragmentByTag(FRAGMENT_LIST);

        if (songFragment == null) {
            songFragment = new SongListFragment();
            songFragment.setSongSource(SongListSource.getUserSongListSource(userId));
            songFragment.setTopView(R.layout.list_user_item);
            fm.beginTransaction().replace(R.id.fragment_content, songFragment, FRAGMENT_LIST).commit();
        }

        dataFragment = (UserDataFragment)fm.findFragmentByTag(FRAGMENT_DATA);

        if (dataFragment == null) {
            dataFragment = new UserDataFragment();
            fm.beginTransaction().add(dataFragment, FRAGMENT_DATA).commit();
        }

        user = dataFragment.GetUser();
        if (user == null) {
            dataFragment.LoadUser(userId);
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
        songFragment.refreshDone();
    }

    @Override
    public void TopViewLoaded() {
        ShowUser();
    }

    @Override
    public boolean Refresh() {
        dataFragment.LoadUser(userId);
        return true;
    }

    private void ShowUser() {
        if (user == null) return;

        View userView = songFragment.getTopView();
        if (userView == null) return;

        if (user.isError()) {

            return;
        }

        User u = user.data;
        getSupportActionBar().setTitle(u.name);
        Glide.with(this).load(SongDetailActivity.getPictureUrl(u.avatar)).centerCrop().into((ImageView) userView.findViewById(R.id.avatar));
        ((TextView)userView.findViewById(R.id.user_posts)).setText(Integer.toString(u.posts));
        ((TextView)userView.findViewById(R.id.user_clicks)).setText(Integer.toString(u.clicks));
        ((TextView)userView.findViewById(R.id.user_comments)).setText(Integer.toString(u.comments));
    }
}
