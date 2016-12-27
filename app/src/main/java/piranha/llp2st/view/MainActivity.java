package piranha.llp2st.view;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.UrlQuerySanitizer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import piranha.llp2st.R;
import piranha.llp2st.data.Category;
import piranha.llp2st.data.Downloads;
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
                Intent intent = new Intent(this, SongDetailActivity.class);
                intent.putExtra(SongDetailActivity.EXTRA_ID, SongDetailActivity.RANDOM_ID);
                startActivity(intent);
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
            case R.id.action_redownload:
                redownload();
                return true;
            case R.id.action_url:
                showUrlDialog();
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

    private void redownload() {
        final List<String> ids = Downloads.getAllDownloads();
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(false);
        dialog.setMessage("Redownloading data files");
        dialog.setMax(ids.size());
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String id : ids) {
                    try {
                        Downloads.redownload(id, MainActivity.this);
                    } catch (Exception e) {
                        android.util.Log.i("Failed", id);
                        e.printStackTrace();
                        final ErrorOr<Boolean> err = ErrorOr.wrap(e);
                    }
                    dialog.incrementProgressBy(1);
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.hide();
                    }
                });
            }
        }).start();
    }

    private void showUrlDialog() {
        final EditText urlText = new EditText(this);
        urlText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        new AlertDialog.Builder(this)
            .setTitle("Enter URL or Live ID")
            .setView(urlText)
            .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int button) {
                    openUrlOrId(urlText.getText().toString());
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int button) {
                }
            })
            .show();
    }

    private void openUrlOrId(String urlOrId) {
        String songId = null;
        Integer userId = null;
        if (urlOrId.matches("^[0-9A-Za-z]{16}$")) {
            songId = urlOrId;
        } else {
            UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(urlOrId);
            songId = sanitizer.getValue("live_id");
            if (songId != null && !songId.matches("^[0-9A-Za-z]{16}$")) {
                songId = null;
            }
            if (songId == null) {
                try {
                    userId = Integer.parseInt(sanitizer.getValue("uid"));
                    if (userId < 0) {
                        userId = null;
                    }
                } catch (Exception e) { }
            }
        }
        if (songId != null) {
            Intent intent = new Intent(this, SongDetailActivity.class);
            intent.putExtra(SongDetailActivity.EXTRA_ID, songId);
            startActivity(intent);
        } else if (userId != null) {
            Intent intent = new Intent(this, UserActivity.class);
            intent.putExtra(UserActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Invalid URL/ID entered.", Toast.LENGTH_LONG).show();
        }
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
