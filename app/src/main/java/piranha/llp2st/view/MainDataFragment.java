package piranha.llp2st.view;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import piranha.llp2st.Util;
import piranha.llp2st.data.Category;
import piranha.llp2st.data.Login;
import piranha.llp2st.exception.ErrorOr;
import piranha.llp2st.exception.LLPException;

public class MainDataFragment extends Fragment {

    interface DataCallbacks {
        void CategoriesLoaded(ErrorOr<List<Category>> categories);
        void RandomSongLoaded(ErrorOr<String> id);
        void LoginLoaded();
    }

    private DataCallbacks callbacks;

    public List<Category> categories;
    public SongListFragment searchFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callbacks = (DataCallbacks)context;
        new LoginTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    public void LoadCategories() {
        if (categories == null) {
            new CategoryTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (callbacks != null) {
            callbacks.CategoriesLoaded(new ErrorOr<>(categories));
        }
    }

    public void LoadRandomSong() {
        new RandomTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    class CategoryTask extends AsyncTask<Void, Void, ErrorOr<List<Category>>> {

        @Override
        protected ErrorOr<List<Category>> doInBackground(Void... voids) {
            try {
                JSONObject j = new JSONObject(Util.download("https://m.tianyi9.com/API/getcategory"));
                LLPException.ThrowIfError(j);
                List<Category> categories = new ArrayList<>();
                JSONArray items = j.getJSONObject("content").getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject o = items.getJSONObject(i);
                    Category c = new Category();
                    c.id = o.getInt("id");
                    c.name = o.getString("name");
                    categories.add(c);
                }
                return new ErrorOr<>(categories);
            } catch (Exception e) {
                e.printStackTrace();
                return ErrorOr.wrap(e);
            }
        }

        @Override
        protected void onPostExecute(ErrorOr<List<Category>> result) {
            categories = result.data;
            if (callbacks != null) {
                callbacks.CategoriesLoaded(result);
            }
        }
    }

    class LoginTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Login.checkLogin(getContext());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (callbacks != null) {
                callbacks.LoginLoaded();
            }
        }
    }

    class RandomTask extends AsyncTask<Void, Void, ErrorOr<String>> {

        @Override
        protected ErrorOr<String> doInBackground(Void... voids) {
            try {
                JSONObject j = new JSONObject(Util.download("https://m.tianyi9.com/API/getRandomLive"));
                LLPException.ThrowIfError(j);
                return new ErrorOr<>(j.getJSONObject("content").getString("live_id"));
            } catch (Exception e) {
                e.printStackTrace();
                return ErrorOr.wrap(e);
            }
        }

        @Override
        protected void onPostExecute(ErrorOr<String> result) {
            if (callbacks != null) {
                callbacks.RandomSongLoaded(result);
            }
        }
    }
}
