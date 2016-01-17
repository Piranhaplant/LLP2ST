package piranha.llp2st.view;


import android.os.Bundle;
import android.preference.PreferenceFragment;

import piranha.llp2st.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
