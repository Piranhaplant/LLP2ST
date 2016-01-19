package piranha.llp2st.view;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import piranha.llp2st.R;
import piranha.llp2st.data.Category;
import piranha.llp2st.data.Login;
import piranha.llp2st.data.SongListSource;
import piranha.llp2st.exception.ErrorOr;

public class MainActivity extends BaseActivity implements MainDataFragment.DataCallbacks {

    private MainDataFragment dataFragment;
    private static final String FRAGMENT_DATA = "data";

    private ViewPager viewPager;
    private Adapter viewPagerAdapter;
    private TabLayout tabLayout;
    private ProgressBar progress;
    private TextView errorText;
    private MenuItem stopButton;
    private MenuItem loginButton;
    private MenuItem logoutButton;
    private boolean loadingRandom = false;
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progress = (ProgressBar)findViewById(R.id.categoryProgress);

        errorText = (TextView)findViewById(R.id.main_error);
        errorText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadCategories();
            }
        });

        viewPager = (ViewPager)findViewById(R.id.viewpager);

        FragmentManager fm = getSupportFragmentManager();
        dataFragment = (MainDataFragment)fm.findFragmentByTag(FRAGMENT_DATA);
        if (dataFragment == null) {
            dataFragment = new MainDataFragment();
            fm.beginTransaction().add(dataFragment, FRAGMENT_DATA).commit();
        }

        loadCategories();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            if (!loaded) return;

            String query = intent.getStringExtra(SearchManager.QUERY);

            SongListSource source = SongListSource.getSearchSongListSource(query);
            if (dataFragment.searchFragment != null) {
                dataFragment.searchFragment.setSongSource(source);
            } else {
                dataFragment.searchFragment = createSongListFragment(source);

                viewPagerAdapter.addFragment(dataFragment.searchFragment);
                tabLayout.setupWithViewPager(viewPager);
            }
            viewPager.setCurrentItem(viewPagerAdapter.getCount() - 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Associate searchable configuration with the SearchView
            SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView)menu.findItem(R.id.action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        stopButton = menu.findItem(R.id.action_stop);
        loginButton = menu.findItem(R.id.action_login);
        logoutButton = menu.findItem(R.id.action_logout);
        refreshStopButton();
        refreshLogin();
        return true;
    }

    @Override
    void setStopVisible(boolean visible) {
        if (stopButton != null) {
            stopButton.setVisible(visible);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLogin();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    onSearchRequested();
                }
                return true;
            case R.id.action_random:
                if (loadingRandom) return true;
                loadingRandom = true;
                dataFragment.LoadRandomSong();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_downloads:
                startActivity(new Intent(this, DownloadsActivity.class));
                return true;
            case R.id.action_stop:
                BaseActivity.Stop();
                return true;
            case R.id.action_login:
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            case R.id.action_logout:
                Login.logout(this);
                refreshLogin();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshLogin() {
        if (loginButton != null) {
            loginButton.setVisible(!Login.isLoggedIn());
            logoutButton.setVisible(Login.isLoggedIn());
            if (Login.isLoggedIn()) {
                logoutButton.setTitle("Logout " + Login.username);
            }
        }
    }

    private void loadCategories() {
        progress.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        dataFragment.LoadCategories();
    }

    @Override
    public void CategoriesLoaded(ErrorOr<List<Category>> categories) {
        progress.setVisibility(View.GONE);
        if (categories.isError()) {
            errorText.setText("Error loading LLPractice data: " + categories.error.getMessage() + "\nTap to retry.");
            errorText.setVisibility(View.VISIBLE);
            return;
        }
        loaded = true;

        viewPagerAdapter = new Adapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(createSongListFragment(SongListSource.getNewSongListSource()));
        viewPagerAdapter.addFragment(createSongListFragment(SongListSource.getFeaturedSongListSource()));
        for (Category c : categories.data) {
            viewPagerAdapter.addFragment(createSongListFragment(SongListSource.getCategorySongListSource(c.name, c.id)));
        }
        if (dataFragment.searchFragment != null) {
            viewPagerAdapter.addFragment(dataFragment.searchFragment);
        }
        viewPager.setAdapter(viewPagerAdapter);

        tabLayout = (TabLayout)findViewById(R.id.tabs);
        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void RandomSongLoaded(ErrorOr<String> id) {
        loadingRandom = false;
        if (id.isError()) {
            Toast.makeText(this, "Error getting random song: " + id.error.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        Context context = MainActivity.this;
        Intent intent = new Intent(context, SongDetailActivity.class);
        intent.putExtra(SongDetailActivity.EXTRA_ID, id.data);

        context.startActivity(intent);
    }

    @Override
    public void LoginLoaded() {
        refreshLogin();
    }

    private static SongListFragment createSongListFragment(SongListSource source) {
        SongListFragment fragment = new SongListFragment();
        fragment.setSongSource(source);
        return fragment;
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<SongListFragment> fragments = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(SongListFragment fragment) {
            fragments.add(fragment);
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragments.get(position).getSongSource().getTitle();
        }
    }
}
