package piranha.llp2st.view;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import piranha.llp2st.R;
import piranha.llp2st.data.Login;
import piranha.llp2st.exception.ErrorOr;

public class LoginFragment extends Fragment {

    private EditText usernameText;
    private EditText passwordText;
    private View progressBar;
    private View loginButton;
    private TextView errorText;

    private String username;
    private String password;
    private boolean loggingIn = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        usernameText = (EditText)v.findViewById(R.id.login_username);
        passwordText = (EditText)v.findViewById(R.id.login_password);
        progressBar = v.findViewById(R.id.login_progress);
        loginButton = v.findViewById(R.id.login_button);
        errorText = (TextView)v.findViewById(R.id.login_error);

        if (loggingIn) {
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
        }

        if (getActivity() != null) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!loggingIn) {
                    loggingIn = true;
                    new LoginTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });
        return v;
    }

    class LoginTask extends AsyncTask<Void, Void, ErrorOr<Boolean>> {

        @Override
        protected void onPreExecute() {
            username = usernameText.getText().toString();
            password = passwordText.getText().toString();

            progressBar.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            errorText.setVisibility(View.GONE);
        }

        @Override
        protected ErrorOr<Boolean> doInBackground(Void... voids) {
            try {
                Login.login(username, password, getContext());
                return new ErrorOr<>(true);
            } catch (Exception e) {
                e.printStackTrace();
                return ErrorOr.wrap(e);
            }
        }

        protected void onPostExecute(ErrorOr<Boolean> result) {
            progressBar.setVisibility(View.GONE);
            if (result.isError()) {
                loginButton.setVisibility(View.VISIBLE);
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Error: " + result.error.getMessage());
            } else {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.finish();
                }
            }
            loggingIn = false;
        }
    }
}
