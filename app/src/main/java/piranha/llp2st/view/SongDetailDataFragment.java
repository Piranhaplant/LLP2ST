package piranha.llp2st.view;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import piranha.llp2st.data.CommentList;
import piranha.llp2st.data.Song;
import piranha.llp2st.data.SongInfo;
import piranha.llp2st.exception.ErrorOr;

public class SongDetailDataFragment extends Fragment {

    interface DataCallbacks {
        void InfoLoaded(ErrorOr<Song> song);
        void CommentsLoaded(ErrorOr<CommentList> comments);
    }

    private DataCallbacks callbacks;
    private CommentList comments;
    private ErrorOr<CommentList> errComments;

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

    public void LoadInfo(String id) {
        new InfoTask().execute(id);
    }

    public ErrorOr<CommentList> GetComments() {
        return errComments;
    }
    public void LoadComments(String id) {
        new CommentTask().execute(id);
    }
    public void LoadMoreComments() {
        new CommentTask().execute();
    }

    class InfoTask extends AsyncTask<String, Void, ErrorOr<Song>> {

        @Override
        protected ErrorOr<Song> doInBackground(String... strings) {
            String id = strings[0];
            try {
                return new ErrorOr<>(SongInfo.get(id, true));
            } catch (Exception e) {
                e.printStackTrace();
                return ErrorOr.wrap(e);
            }
        }

        @Override
        protected void onPostExecute(ErrorOr<Song> result) {
            if (callbacks != null) {
                callbacks.InfoLoaded(result);
            }
        }
    }

    class CommentTask extends AsyncTask<String, Void, ErrorOr<CommentList>> {

        @Override
        protected ErrorOr<CommentList> doInBackground(String... strings) {
            if (strings.length >= 1) {
                comments = new CommentList(strings[0]);
            }
            try {
                comments.loadComments();
                return new ErrorOr<>(comments);
            } catch (Exception e) {
                e.printStackTrace();
                return ErrorOr.wrap(e);
            }
        }

        @Override
        protected void onPostExecute(ErrorOr<CommentList> result) {
            errComments = result;
            if (callbacks != null) {
                callbacks.CommentsLoaded(result);
            }
        }
    }
}
