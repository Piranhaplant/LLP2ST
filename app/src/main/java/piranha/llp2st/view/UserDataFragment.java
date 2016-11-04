package piranha.llp2st.view;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.json.JSONObject;

import piranha.llp2st.Util;
import piranha.llp2st.data.Login;
import piranha.llp2st.data.User;
import piranha.llp2st.exception.ErrorOr;
import piranha.llp2st.exception.LLPException;

public class UserDataFragment extends Fragment {

    interface DataCallbacks {
        void UserLoaded(ErrorOr<User> user);
    }

    private DataCallbacks callbacks;
    private ErrorOr<User> errUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callbacks = (DataCallbacks)context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    public void LoadUser(int id) {
        new UserTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
    }

    public ErrorOr<User> GetUser() {
        return errUser;
    }

    class UserTask extends AsyncTask<Integer, Void, ErrorOr<User>> {

        @Override
        protected ErrorOr<User> doInBackground(Integer... integers) {
            Integer id = integers[0];
            try {
                String url = "https://m.tianyi9.com/API/user_info?uid=" + Integer.toString(id);
                if (Login.isLoggedIn()) {
                    url += "&" + Login.getURLParams();
                }
                JSONObject j = new JSONObject(Util.download(url));
                LLPException.ThrowIfError(j);
                JSONObject content = j.getJSONObject("content");
                return new ErrorOr<>(new User(content));
            } catch (Exception e) {
                e.printStackTrace();
                return ErrorOr.wrap(e);
            }
        }

        @Override
        protected void onPostExecute(ErrorOr<User> result) {
            errUser = result;
            if (callbacks != null) {
                callbacks.UserLoaded(result);
            }
        }
    }

}
