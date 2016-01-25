package piranha.llp2st.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import mbanje.kurt.fabbutton.FabButton;
import piranha.llp2st.R;
import piranha.llp2st.Util;
import piranha.llp2st.data.Comment;
import piranha.llp2st.data.CommentList;
import piranha.llp2st.data.Downloads;
import piranha.llp2st.data.Song;
import piranha.llp2st.exception.ErrorOr;
import piranha.llp2st.exception.LLPException;

public class SongDetailActivity extends BaseActivity implements SongDetailDataFragment.DataCallbacks, Downloads.StatusChangedListener {

    public static final String EXTRA_ID = "live_id";

    private SongDetailDataFragment dataFragment;
    private static final String FRAGMENT_DATA = "data";

    private String id;
    private boolean failed = false;
    private ErrorOr<CommentList> comments;
    private int commentsShown = 0;

    private FabButton downloadButton;
    private FabButton playButton;

    private LinearLayout commentList;
    private TextView commentMore;
    private View commentProgress;
    private View commentNone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        id = intent.getStringExtra(EXTRA_ID);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        downloadButton = (FabButton)findViewById(R.id.detail_download);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Downloads.getStatus(id) == Downloads.Status.Done) {
                    Downloads.showDeletePrompt(id, view.getContext());
                } else {
                    Downloads.downloadAsync(id, view.getContext());
                }
            }
        });

        playButton = (FabButton)findViewById(R.id.detail_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BaseActivity.isPlaying()) {
                    BaseActivity.Stop();
                } else {
                    BaseActivity.Play(id);
                }
            }
        });
        commentList = (LinearLayout)findViewById(R.id.comment_list);
        commentMore = (TextView)findViewById(R.id.comments_more);
        commentProgress = findViewById(R.id.comment_progress);
        commentNone = findViewById(R.id.comment_none);
        commentMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoadMoreComments();
            }
        });

        Downloads.addListener(this);
        updateStatus();

        FragmentManager fm = getSupportFragmentManager();
        dataFragment = (SongDetailDataFragment)fm.findFragmentByTag(FRAGMENT_DATA);

        if (dataFragment == null) {
            dataFragment = new SongDetailDataFragment();
            fm.beginTransaction().add(dataFragment, FRAGMENT_DATA).commit();
        }

        dataFragment.LoadInfo(id);
        comments = dataFragment.GetComments();
        if (comments == null) {
            dataFragment.LoadComments(id);
        } else {
            ShowComments();
        }
    }

    @Override
    public void StatusChanged(String id, Downloads.Status status) {
        if (id.equals(this.id)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateStatus();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (failed) {
            comments = null;
            dataFragment.LoadInfo(id);
        }
    }

    @Override
    void setStopVisible(boolean visible) {
        if (visible) {
            playButton.setIcon(R.drawable.ic_stop_white, R.drawable.ic_done_white_pad);
        } else {
            playButton.setIcon(R.drawable.ic_play_arrow_white, R.drawable.ic_done_white_pad);
        }
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

    private void updateStatus() {
        Downloads.Status status = Downloads.getStatus(id);
        switch (status) {
            case None:
                downloadButton.onProgressCompleted();
                downloadButton.resetIcon();
                break;
            case InProgress:
                downloadButton.showProgress(true);
                break;
            case Done:
                downloadButton.onProgressCompleted();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Downloads.removeListener(this);
    }

    @Override
    public void InfoLoaded(final ErrorOr<Song> esong) {
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout)findViewById(R.id.collapsing_toolbar);
        TextView songInfo = (TextView)findViewById(R.id.detail_songInfo);
        TextView authorInfo = (TextView)findViewById(R.id.detail_authorInfo);
        View userCard = findViewById(R.id.detail_user_card);
        final ImageView imageView = (ImageView)findViewById(R.id.backdrop);
        final ImageView authorPicture = (ImageView)findViewById(R.id.detail_authorPicture);

        findViewById(R.id.detail_area).setVisibility(View.VISIBLE);
        if (esong.isError()) {
            failed = true;
            collapsingToolbar.setTitle(getResources().getString(R.string.error_detail_title));
            songInfo.setText(R.string.error_detail);
            if (esong.error instanceof LLPException) {
                LLPException error = (LLPException)esong.error;
                if (error.errcode == LLPException.ERRCODE_NEEDLOGIN) {
                    authorInfo.setText(R.string.error_detail_signin);
                    authorPicture.setBackgroundResource(R.drawable.ic_lock_black);
                    userCard.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(SongDetailActivity.this, LoginActivity.class));
                        }
                    });
                    return;
                }
            }
            authorInfo.setText(esong.error.getMessage());
            return;
        }

        findViewById(R.id.comment_area).setVisibility(View.VISIBLE);
        failed = false;
        downloadButton.setVisibility(View.VISIBLE);
        playButton.setVisibility(View.VISIBLE);

        final Song song = esong.data;
        collapsingToolbar.setTitle(song.name);

        Glide.with(SongDetailActivity.this).load(Song.UploadPath + song.pictureUrl).centerCrop().into(imageView);

        songInfo.setText(Html.fromHtml(
            "<b>Title:</b> " + song.name + "<br/>" +
            "<b>Artist:</b> " + song.artist + "<br/>" +
            "<b>Level:</b> " + song.difficulty + "<br/>" +
            "<b>Played:</b> " + song.clickCount + "<br/>" +
            "<b>Description:</b> " + song.description));

        authorInfo.setText(Html.fromHtml(
                "<b>Author:</b> " + song.uploaderName + "<br/>" +
                        "<b>Post count:</b> " + song.uploaderPostCount));

        Glide.with(SongDetailActivity.this).load(Util.getPictureUrl(song.uploaderPictureUrl)).centerCrop().into(authorPicture);

        userCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                Intent intent = new Intent(context, UserActivity.class);
                intent.putExtra(UserActivity.EXTRA_USER_ID, song.uploaderId);
                intent.putExtra(UserActivity.EXTRA_USER_NAME, song.uploaderName);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public void CommentsLoaded(ErrorOr<CommentList> comments) {
        this.comments = comments;
        ShowComments();
    }

    private void ShowComments() {
        if (comments == null) return;

        commentProgress.setVisibility(View.GONE);

        if (comments.isError()) {
            commentMore.setVisibility(View.VISIBLE);
            commentMore.setText("Error loading comments: " + comments.error.getMessage());
            return;
        }

        CommentList c = comments.data;
        commentMore.setText(R.string.detail_loadmore);
        commentMore.setVisibility(c.comments.size() < c.availableCount ? View.VISIBLE : View.GONE);
        commentNone.setVisibility(c.comments.size() == 0 ? View.VISIBLE : View.GONE);

        for (int i = commentsShown; i < c.comments.size(); i++) {
            CreateCommentView(c.comments.get(i));
        }

        commentsShown = c.comments.size();
    }

    private void CreateCommentView(final Comment comment) {
        View v = View.inflate(this, R.layout.comment, null);
        TextView content = (TextView)v.findViewById(R.id.comment_text);
        TextView header = (TextView)v.findViewById(R.id.comment_header);
        ImageView picture = (ImageView)v.findViewById(R.id.comment_picture);

        content.setText(comment.content);
        header.setText(comment.userName + " at " + comment.date);
        Glide.with(this).load(Util.getPictureUrl(comment.userPictureUrl)).centerCrop().into(picture);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                Intent intent = new Intent(context, UserActivity.class);
                intent.putExtra(UserActivity.EXTRA_USER_ID, comment.userId);
                intent.putExtra(UserActivity.EXTRA_USER_NAME, comment.userName);
                context.startActivity(intent);
            }
        });

        commentList.addView(v);
    }

    private void LoadMoreComments() {
        dataFragment.LoadMoreComments();
        commentProgress.setVisibility(View.VISIBLE);
        commentMore.setVisibility(View.GONE);
    }
}
