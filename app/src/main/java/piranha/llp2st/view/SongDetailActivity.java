package piranha.llp2st.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import mbanje.kurt.fabbutton.FabButton;
import piranha.llp2st.R;
import piranha.llp2st.data.Api;
import piranha.llp2st.data.Comment;
import piranha.llp2st.data.CommentList;
import piranha.llp2st.data.Downloads;
import piranha.llp2st.data.Song;
import piranha.llp2st.exception.ErrorOr;
import piranha.llp2st.exception.LLPException;

public class SongDetailActivity extends BaseActivity implements SongDetailDataFragment.DataCallbacks, Downloads.StatusChangedListener {

    public static final String EXTRA_ID = "live_id";
    public static final String RANDOM_ID = "random";

    private SongDetailDataFragment dataFragment;
    private static final String FRAGMENT_DATA = "data";

    private String id;
    private boolean failed = false;

    private Song song;
    private ErrorOr<CommentList> comments;
    private int selectedComment;

    private FabButton downloadButton;
    private FabButton playButton;

    private RecyclerView commentList;
    private TextView commentMore;
    private View commentProgress;
    private View commentNone;

    private CollapsingToolbarLayout collapsingToolbar;
    private TextView songInfo;
    private TextView authorInfo;
    private View userCard;
    private ImageView backdrop;
    private ImageView authorPicture;

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
        playButton = (FabButton)findViewById(R.id.detail_play);
        collapsingToolbar = (CollapsingToolbarLayout)findViewById(R.id.collapsing_toolbar);
        songInfo = (TextView)findViewById(R.id.detail_songInfo);
        authorInfo = (TextView)findViewById(R.id.detail_authorInfo);
        userCard = findViewById(R.id.detail_user_card);
        backdrop = (ImageView)findViewById(R.id.backdrop);
        authorPicture = (ImageView)findViewById(R.id.detail_authorPicture);
        commentList = (RecyclerView)findViewById(R.id.comment_list);
        commentMore = (TextView)findViewById(R.id.comments_more);
        commentProgress = findViewById(R.id.comment_progress);
        commentNone = findViewById(R.id.comment_none);

        commentList.setNestedScrollingEnabled(false);
        commentList.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(commentList);
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

        if (id.equals(RANDOM_ID)) {
            dataFragment.LoadRandomSong();
        } else {
            LoadId();
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
        if (failed && !id.equals(RANDOM_ID)) {
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // For some reason this method is called for the comment context menu too
        // This fixes these items being shown on that menu too
        if (menu.size() == 0) {
            getMenuInflater().inflate(R.menu.menu_songinfo, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

        switch (item.getItemId()) {
            case R.id.menu_copy_title:
                clipboard.setPrimaryClip(ClipData.newPlainText("Title", song.name));
                return true;
            case R.id.menu_copy_artist:
                clipboard.setPrimaryClip(ClipData.newPlainText("Artist", song.artist));
                return true;
            case R.id.menu_copy_description:
                clipboard.setPrimaryClip(ClipData.newPlainText("Description", song.description));
                return true;
            case R.id.menu_copy_url:
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", Api.BASE_URL + "#/getlive?live_id=" + song.id));
                return true;
            case R.id.menu_copy_comment:
                clipboard.setPrimaryClip(ClipData.newPlainText("Comment", comments.data.comments.get(selectedComment).content));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void InfoLoaded(final ErrorOr<Song> esong) {
        findViewById(R.id.detail_area).setVisibility(View.VISIBLE);
        if (esong.isError()) {
            ShowError(esong.error);
            return;
        }

        findViewById(R.id.comment_area).setVisibility(View.VISIBLE);
        failed = false;
        downloadButton.setVisibility(View.VISIBLE);
        playButton.setVisibility(View.VISIBLE);

        song = esong.data;
        collapsingToolbar.setTitle(song.name);

        Glide.with(SongDetailActivity.this).load(Api.UPLOAD_URL + song.pictureUrl).centerCrop().into(backdrop);

        songInfo.setText(Html.fromHtml(
            "<b>Title:</b> " + htmlEscape(song.name) + "<br/>" +
            "<b>Artist:</b> " + htmlEscape(song.artist) + "<br/>" +
            "<b>Level:</b> " + song.difficulty + "<br/>" +
            "<b>Played:</b> " + song.clickCount + "<br/>" +
            "<b>Description:</b> " + htmlEscape(song.description)));
        registerForContextMenu(songInfo);

        authorInfo.setText(Html.fromHtml(
            "<b>Author:</b> " + htmlEscape(song.user.name) + "<br/>" +
            "<b>Post count:</b> " + song.user.posts));

        Glide.with(SongDetailActivity.this).load(Api.getPictureUrl(song.user.avatar)).centerCrop().into(authorPicture);

        userCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                Intent intent = new Intent(context, UserActivity.class);
                intent.putExtra(UserActivity.EXTRA_USER_ID, song.user.id);
                intent.putExtra(UserActivity.EXTRA_USER_NAME, song.user.name);
                context.startActivity(intent);
            }
        });
    }

    private String htmlEscape(String s) {
        return s.replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public void CommentsLoaded(ErrorOr<CommentList> comments) {
        this.comments = comments;
        ShowComments();
    }

    @Override
    public void RandomSongLoaded(ErrorOr<String> id) {
        if (id.isError()) {
            findViewById(R.id.detail_area).setVisibility(View.VISIBLE);
            ShowError(id.error);
            return;
        }
        this.id = id.data;
        LoadId();
    }

    private void LoadId() {
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
        dataFragment.LoadInfo(id);
        comments = dataFragment.GetComments();
        if (comments == null) {
            dataFragment.LoadComments(id);
        } else {
            ShowComments();
        }
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

        commentList.setAdapter(new CommentRecyclerViewAdapter(c.comments));
    }

    private void LoadMoreComments() {
        dataFragment.LoadMoreComments();
        commentProgress.setVisibility(View.VISIBLE);
        commentMore.setVisibility(View.GONE);
    }

    private void ShowError(Exception error) {
        failed = true;
        collapsingToolbar.setTitle(getResources().getString(R.string.error_detail_title));
        songInfo.setText(R.string.error_detail);
        if (error instanceof LLPException) {
            LLPException llperror = (LLPException)error;
            if (llperror.errcode == LLPException.ERRCODE_NEEDLOGIN) {
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
        authorInfo.setText(error.getMessage());
    }

    public class CommentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_COMMENT = 0;

        private List<Comment> comments;

        public class CommentViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
            public final View v;
            public final ImageView picture;
            public final TextView header;
            public final TextView commentText;
            public Comment comment;

            public CommentViewHolder(View view) {
                super(view);
                v = view;
                picture = (ImageView)view.findViewById(R.id.comment_picture);
                header = (TextView)view.findViewById(R.id.comment_header);
                commentText = (TextView)view.findViewById(R.id.comment_text);
                v.setOnCreateContextMenuListener(this);
            }

            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                getMenuInflater().inflate(R.menu.menu_comment, contextMenu);
            }
        }

        public CommentRecyclerViewAdapter(List<Comment> items) {
            comments = items;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder h, final int position) {
            final CommentViewHolder holder = (CommentViewHolder)h;
            final Comment c = comments.get(position);
            holder.comment = c;
            if (c == null) return;

            holder.commentText.setText(c.content);
            holder.header.setText(c.user.name + " at " + c.date);
            Glide.with(holder.picture.getContext()).load(Api.getPictureUrl(c.user.avatar)).centerCrop().into(holder.picture);
            holder.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, UserActivity.class);
                    intent.putExtra(UserActivity.EXTRA_USER_ID, c.user.id);
                    intent.putExtra(UserActivity.EXTRA_USER_NAME, c.user.name);
                    context.startActivity(intent);
                }
            });
            holder.v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    selectedComment = holder.getLayoutPosition();
                    return false;
                }
            });
        }

        @Override
        public int getItemViewType(int position) {
            return VIEW_COMMENT;
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }
    }
}
