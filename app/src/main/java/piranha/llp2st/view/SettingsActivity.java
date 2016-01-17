package piranha.llp2st.view;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import piranha.llp2st.R;

public class SettingsActivity extends PlaySongActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        Toolbar toolbar = (Toolbar)findViewById(R.id.fragment_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction()
            .replace(R.id.fragment_content, new SettingsFragment())
            .commit();
    }

    @Override
    void setStopVisible(boolean visible) {
        // Nothing since we don't need a stop button on the settings screen
    }
}
