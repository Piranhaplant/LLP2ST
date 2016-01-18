package piranha.llp2st.view;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import piranha.llp2st.R;
import piranha.llp2st.data.DownloadsSongListSource;

public class LoginActivity extends PlaySongActivity {

    private static final String FRAGMENT_DATA = "data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        Toolbar toolbar = (Toolbar)findViewById(R.id.fragment_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
        LoginFragment frag = (LoginFragment)fm.findFragmentByTag(FRAGMENT_DATA);

        if (frag == null) {
            frag = new LoginFragment();
            fm.beginTransaction().replace(R.id.fragment_content, frag, FRAGMENT_DATA).commit();
        }
    }

    @Override
    void setStopVisible(boolean visible) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
