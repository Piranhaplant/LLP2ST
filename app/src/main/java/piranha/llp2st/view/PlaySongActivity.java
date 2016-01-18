package piranha.llp2st.view;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.MenuItem;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import piranha.llp2st.R;
import piranha.llp2st.data.Downloads;
import piranha.llp2st.data.Login;
import piranha.llp2st.data.Song;
import piranha.llp2st.data.SongInfo;
import piranha.llp2st.exception.ErrorOr;

public abstract class PlaySongActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

    private static final int NOTIFICATION_ID = 1;
    private static final String EXTRA_AUDIO_SESSION = "audioSession";
    private static final String EXTRA_ACTION = "action";
    private static final String ACTION_STOP = "stop";

    static MediaPlayer player;
    static PlaySongActivity curActivity;
    static PlaySongActivity audioFocusActivity;
    static boolean loadingSong = false;

    abstract void setStopVisible(boolean visible);

    public void refreshStopButton() {
        if (player != null) {
            setStopVisible(player.isPlaying());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        curActivity = this;
        refreshStopButton();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_stop:
                Stop();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void Play(String id) {
        if (loadingSong)
            return;
        loadingSong = true;
        if (player == null) {
            player = new MediaPlayer();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    curActivity.setStopVisible(false);
                }
            });
        } else {
            player.reset();
        }
        new PlayTask().execute(id);
    }

    public static void Stop() {
        if (player != null) {
            player.stop();
            curActivity.setStopVisible(false);
            AudioManager audioManager = (AudioManager)curActivity.getSystemService(Context.AUDIO_SERVICE);
            audioManager.abandonAudioFocus(audioFocusActivity);
            audioFocusActivity = null;
            //final NotificationManager n = (NotificationManager)curActivity.getSystemService(Context.NOTIFICATION_SERVICE);
            //n.cancel(NOTIFICATION_ID);
        }
    }

    public static boolean isPlaying() {
        if (player == null) {
            return false;
        } else {
            return player.isPlaying();
        }
    }

    public void onAudioFocusChange(int focusChange) {
        if (player == null) return;

        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (!player.isPlaying())
                player.start();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            if (player.isPlaying())
                player.stop();
        } else {
            if (player.isPlaying())
                player.pause();
        }
    }

    static class PlayTask extends AsyncTask<String, Void, ErrorOr<Song>> {

        @Override
        protected ErrorOr<Song> doInBackground(String... ids) {
            try {
                Song s;
                if (Downloads.getStatus(ids[0]) == Downloads.Status.Done) {
                    s = SongInfo.get(ids[0], false);
                    player.setDataSource(Downloads.getLocalAudioFileName(ids[0]));
                } else {
                    s = SongInfo.get(ids[0], true);
                    player.setDataSource(Song.UploadPath + s.audioUrl);
                }
                player.prepare();

                if (audioFocusActivity == null) {
                    AudioManager audioManager = (AudioManager) curActivity.getSystemService(Context.AUDIO_SERVICE);
                    int result = audioManager.requestAudioFocus(curActivity, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

                    if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        return new ErrorOr<>(new Exception("Couldn't get audio focus."));
                    }
                    audioFocusActivity = curActivity;
                }

                player.start();
                return new ErrorOr<>(s);
            } catch (Exception e) {
                e.printStackTrace();
                return ErrorOr.wrap(e);
            }
        }

        protected void onPostExecute(ErrorOr<Song> result) {
            curActivity.setStopVisible(!result.isError());
            if (result.isError()) {
                Toast.makeText(curActivity, "Error playing song: " + result.error.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                //CreateNotification(result.data);
            }
            loadingSong = false;
        }
    }

    // TODO: Can't seem to get the action buttons to work...

    private static void CreateNotification(Song s) {
        final android.support.v4.app.NotificationCompat.Builder noti = new NotificationCompat.Builder(curActivity)
                .setSmallIcon(R.drawable.ic_play_arrow_white)
                .setContentTitle(s.name)
                .setContentText("LLP2ST")
                .setStyle(new NotificationCompat.MediaStyle());

        Intent stopIntent = new Intent(curActivity, NotificationAction.class);
        stopIntent.putExtra(EXTRA_ACTION, ACTION_STOP);
        stopIntent.putExtra(EXTRA_AUDIO_SESSION, player.getAudioSessionId());
        PendingIntent stopPendIntent = PendingIntent.getActivity(curActivity, 0, stopIntent, 0);

        noti.addAction(R.drawable.ic_stop_black, "Stop", stopPendIntent);
        noti.setDeleteIntent(stopPendIntent);

        noti.addAction(R.drawable.ic_pause_black, "Pause", stopPendIntent);

        final NotificationManager n = (NotificationManager)curActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        n.notify(NOTIFICATION_ID, noti.build());

        Glide.with(curActivity)
                .load(Song.UploadPath + s.pictureUrl)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        noti.setLargeIcon(resource);
                        n.notify(NOTIFICATION_ID, noti.build());
                    }
                });
    }

    static class NotificationAction extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle extras = getIntent().getExtras();

            MediaPlayer player = new MediaPlayer();
            player.setAudioSessionId(extras.getInt(EXTRA_AUDIO_SESSION));

            String action = extras.getString(EXTRA_ACTION);
            if (action.equals(ACTION_STOP)) {
                player.stop();
            }
            finish();
        }
    }
}
