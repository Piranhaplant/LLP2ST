package piranha.llp2st.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import piranha.llp2st.R;
import piranha.llp2st.Util;
import piranha.llp2st.data.Downloads;
import piranha.llp2st.data.Song;
import piranha.llp2st.data.SongListSource;
import piranha.llp2st.exception.ErrorOr;

public class SongListFragment extends Fragment {

    private SongListSource source;
    private boolean loadingMore = false;

    private RecyclerView rv;
    private View progressBar;
    private View noResults;
    private LinearLayoutManager rvLayoutManager;

    public void setSongSource(SongListSource source) {
        this.source = source;
        if (rv != null) {
            loadingMore = false;
            progressBar.setVisibility(View.VISIBLE);
            noResults.setVisibility(View.GONE);
            rv.setVisibility(View.GONE);
            new SongListTask().execute();
        }
    }
    public SongListSource getSongSource() {
        return source;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_song_list, container, false);
        progressBar = v.findViewById(R.id.songListProgress);
        noResults = v.findViewById(R.id.noResults);

        rv = (RecyclerView)v.findViewById(R.id.recyclerview);
        rvLayoutManager = new LinearLayoutManager(rv.getContext());
        rv.setLayoutManager(rvLayoutManager);
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!loadingMore && rvLayoutManager.getChildCount() + rvLayoutManager.findFirstVisibleItemPosition() >= rvLayoutManager.getItemCount() - 2) {
                    loadingMore = true;
                    ((SongRecyclerViewAdapter)rv.getAdapter()).showLoadingItem();
                    new LoadMoreTask().execute();
                }
            }
        });
        rv.setItemAnimator(null);

        new SongListTask().execute();

        return v;
    }

    class SongListTask extends AsyncTask<Void, Void, List<Song>> {

        @Override
        protected List<Song> doInBackground(Void... voids) {
            return source.getSongs();
        }

        protected void onPostExecute(List<Song> result) {
            rv.setAdapter(new SongRecyclerViewAdapter(result));
            progressBar.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            if (result.size() == 0) {
                noResults.setVisibility(View.VISIBLE);
            }
        }
    }

    class LoadMoreTask extends AsyncTask<Void, Void, List<Song>> {

        @Override
        protected List<Song> doInBackground(Void... voids) {
            if (source.loadMore()) {
                return source.getSongs();
            }
            return null;
        }

        protected void onPostExecute(List<Song> result) {
            SongRecyclerViewAdapter oldAdapter = (SongRecyclerViewAdapter)rv.getAdapter();
            if (result != null) {
                Parcelable state = rvLayoutManager.onSaveInstanceState();

                SongRecyclerViewAdapter newAdapter = new SongRecyclerViewAdapter(result);
                if (oldAdapter != null)
                    newAdapter.expandedPosition = oldAdapter.expandedPosition;

                rv.setAdapter(newAdapter);
                rvLayoutManager.onRestoreInstanceState(state);
                loadingMore = false;
            } else {
                if (oldAdapter != null)
                    oldAdapter.hideLoadingItem();
            }
        }
    }

    public class SongRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_ITEM = 0;
        private static final int VIEW_PROG = 1;

        private List<Song> mValues;
        public int expandedPosition = -1;
        public boolean loadingItemVisible;

        public class ViewHolder extends RecyclerView.ViewHolder implements Downloads.StatusChangedListener {
            public final View mView;
            public final ImageView mImageView;
            public final TextView mTitleText;
            public final TextView mDifficultyText;
            public final ImageView mDownloadImage;
            public final ProgressBar mDownloadProgress;
            public final View mDownloadClickArea;
            public final View mExpandArea;
            public final View mViewDetails;
            public final View mPreviewSong;
            public final View mMemberOnly;
            public Song curSong;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mImageView = (ImageView)view.findViewById(R.id.avatar);
                mTitleText = (TextView)view.findViewById(R.id.songTitle);
                mDifficultyText = (TextView)view.findViewById(R.id.songDifficulty);
                mDownloadImage = (ImageView)view.findViewById(R.id.downloadButton);
                mDownloadProgress = (ProgressBar)view.findViewById(R.id.download_progress);
                mDownloadClickArea = view.findViewById(R.id.download_clickarea);
                mExpandArea = view.findViewById(R.id.expandArea);
                mViewDetails = view.findViewById(R.id.viewDetails);
                mPreviewSong = view.findViewById(R.id.previewSong);
                mMemberOnly = view.findViewById(R.id.memberOnly);
            }

            @Override
            public String toString() {
                return super.toString() + " " + mTitleText.getText();
            }

            @Override
            public void StatusChanged(String id, Downloads.Status status) {
                if (id.equals(curSong.id)) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            updateStatus(ViewHolder.this);
                        }
                    });
                }
            }
        }

        public class ProgViewHolder extends RecyclerView.ViewHolder {
            public ProgViewHolder(View view) {
                super(view);
            }
        }

        public void showLoadingItem() {
            loadingItemVisible = true;
            notifyItemInserted(mValues.size());
        }

        public void hideLoadingItem() {
            loadingItemVisible = false;
            notifyItemRemoved(mValues.size());
        }

        public SongRecyclerViewAdapter(List<Song> items) {
            mValues = items;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_ITEM) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
                final ViewHolder vh = new ViewHolder(view);
                return vh;
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_prog_item, parent, false);
                final ProgViewHolder vh = new ProgViewHolder(view);
                return vh;
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder h, final int position) {
            if (h instanceof ProgViewHolder)
                return;

            ViewHolder holder = (ViewHolder)h;
            final Song s = mValues.get(position);
            holder.curSong = s;
            if (s == null) return;

            updateStatus(holder);

            holder.mTitleText.setText(s.name);
            holder.mDifficultyText.setText("Level: " + s.difficulty);
            holder.mMemberOnly.setVisibility(s.memberOnly ? View.VISIBLE : View.INVISIBLE);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int prevPosition = expandedPosition;
                    if (position != expandedPosition) {
                        expandedPosition = position;
                    } else {
                        expandedPosition = -1;
                    }
                    if (prevPosition >= 0) {
                        notifyItemChanged(prevPosition);
                    }
                    notifyItemChanged(expandedPosition);
                }
            });

            holder.mDownloadClickArea.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Downloads.getStatus(s.id) == Downloads.Status.Done) {
                        Downloads.showDeletePrompt(s.id, getContext());
                    } else {
                        Downloads.downloadAsync(s.id, getContext());
                    }
                }
            });

            holder.mViewDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, SongDetailActivity.class);
                    intent.putExtra(SongDetailActivity.EXTRA_ID, s.id);

                    context.startActivity(intent);
                }
            });

            holder.mPreviewSong.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PlaySongActivity.Play(s.id);
                }
            });

            Glide.with(holder.mImageView.getContext())
                .load(Song.UploadPath + s.pictureUrl)
                .centerCrop()
                .into(holder.mImageView);

            if (position == expandedPosition) {
                holder.mView.setBackgroundResource(R.color.listBgSelected);
                holder.mExpandArea.setVisibility(View.VISIBLE);
            } else {
                holder.mView.setBackgroundResource(R.color.listBgNormal);
                holder.mExpandArea.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position >= mValues.size()) {
                return VIEW_PROG;
            } else {
                return VIEW_ITEM;
            }
        }

        private void updateStatus(final ViewHolder holder) {
            Downloads.Status status = Downloads.getStatus(holder.curSong.id);
            switch (status) {
                case None:
                    holder.mDownloadImage.setVisibility(View.VISIBLE);
                    holder.mDownloadImage.setBackgroundResource(R.drawable.ic_download_black);
                    holder.mDownloadProgress.setVisibility(View.GONE);
                    break;
                case InProgress:
                    holder.mDownloadImage.setVisibility(View.GONE);
                    holder.mDownloadProgress.setVisibility(View.VISIBLE);
                    break;
                case Done:
                    holder.mDownloadImage.setVisibility(View.VISIBLE);
                    holder.mDownloadImage.setBackgroundResource(R.drawable.ic_done_black);
                    holder.mDownloadProgress.setVisibility(View.GONE);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size() + (loadingItemVisible ? 1 : 0);
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof ViewHolder) {
                Downloads.addListener((ViewHolder)holder);
            }
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            if (holder instanceof ViewHolder) {
                Downloads.removeListener((ViewHolder)holder);
            }
        }
    }
}
