package piranha.llp2st.data;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import piranha.llp2st.R;
import piranha.llp2st.Util;
import piranha.llp2st.exception.ErrorOr;
import piranha.llp2st.exception.LLPException;
import piranha.llp2st.view.BaseActivity;

public final class Downloads {

    private static final String dataFilesDirectory = "beatmaps/datafiles/";
    private static final String soundFilesDirectory = "beatmaps/soundfiles/";

    public enum Status {
        None,
        InProgress,
        Done
    }

    private static List<StatusChangedListener> listeners = new ArrayList<>();
    private static Map<String, Status> downloadStatus = new HashMap<>();

    static {
        BaseActivity.runWithPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE, new Runnable() {
            @Override
            public void run() {
                File dir = new File(Environment.getExternalStorageDirectory(), dataFilesDirectory);
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : dir.listFiles()) {
                        // Example file name: ZDnsvUoyoaOjYU78.rs
                        String name = f.getName();
                        if (name.matches("^[0-9A-Za-z]{16}\\.rs$")) {
                            setStatus(name.substring(0, 16), Status.Done);
                        }
                    }
                }
            }
        });
    }

    public interface StatusChangedListener {
        void StatusChanged(String id, Status status);
    }

    public static Status getStatus(String id) {
        Status s = downloadStatus.get(id);
        if (s == null) {
            return Status.None;
        }
        return s;
    }

    public static void download(String id, Context context) throws JSONException, LLPException, IOException {
        if (getStatus(id) != Status.None) return;
        setStatus(id, Status.InProgress);

        BaseActivity.requestPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        try {
            Song s = SongInfo.get(id, true);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            double leadIn = Double.valueOf(prefs.getString("pref_leadin", "2"));
            double timingOffset = Double.valueOf(prefs.getString("pref_offset", "0.1"));

            JSONObject map = new JSONObject(Util.download(Song.UploadPath + s.mapUrl));
            JSONObject sifTrainMap = convertToSifTrain(map, s.name, leadIn, timingOffset);

            File f = new File(Environment.getExternalStorageDirectory(), dataFilesDirectory + id + ".rs");
            f.getParentFile().mkdirs();
            OutputStreamWriter o = new OutputStreamWriter(new FileOutputStream(f));
            o.write(sifTrainMap.toString());
            o.close();

            InputStream i = new URL(Song.UploadPath + s.audioUrl).openStream();
            f = new File(Environment.getExternalStorageDirectory(), soundFilesDirectory + id + ".mp3");
            f.getParentFile().mkdirs();
            FileOutputStream fo = new FileOutputStream(f);
            byte[] buffer = new byte[1024];
            while (true) {
                int count = i.read(buffer);
                if (count == -1) {
                    break;
                }
                fo.write(buffer, 0, count);
            }
            i.close();
            fo.close();
        } catch (Exception e) {
            setStatus(id, Downloads.Status.None);
            throw e;
        }

        setStatus(id, Downloads.Status.Done);
    }

    public static void downloadAsync(final String id, final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    download(id, context);
                } catch (Exception e) {
                    e.printStackTrace();
                    ErrorOr<Boolean> err = ErrorOr.wrap(e);
                    Toast.makeText(context, "Error downloading song: " + err.error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }).start();
    }

    public static void delete(final String id) {
        BaseActivity.runWithPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, new Runnable() {
            @Override
            public void run() {
                File f = new File(Environment.getExternalStorageDirectory(), dataFilesDirectory + id + ".rs");
                f.delete();
                f = new File(Environment.getExternalStorageDirectory(), soundFilesDirectory + id + ".mp3");
                f.delete();
                setStatus(id, Status.None);
            }
        });
    }

    public static List<String> getAllDownloads() {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Status> e : downloadStatus.entrySet()) {
            if (e.getValue() != Status.None) {
                ids.add(e.getKey());
            }
        }
        return ids;
    }

    public static String getLocalSongName(String id) {
        try {
            File f = new File(Environment.getExternalStorageDirectory(), dataFilesDirectory + id + ".rs");
            JSONObject json = new JSONObject(Util.readFile(f));
            return json.getString("song_name");
        } catch (Exception ex) {
            ex.printStackTrace();
            return id;
        }
    }

    public static String getLocalAudioFileName(String id) {
        File f = new File(Environment.getExternalStorageDirectory(), soundFilesDirectory + id + ".mp3");
        return f.getAbsolutePath();
    }

    public static void showDeletePrompt(final String id, Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setMessage(R.string.dialog_delete_message);
        dialog.setPositiveButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Downloads.delete(id);
            }
        });
        dialog.setNegativeButton(R.string.dialog_cancel, null);
        dialog.create().show();
    }

    public static void addListener(StatusChangedListener listener) {
        listeners.add(listener);
    }
    public static void removeListener(StatusChangedListener listener) {
        listeners.remove(listener);
    }

    private static void setStatus(String id, Status s) {
        downloadStatus.put(id, s);
        for (StatusChangedListener listener : listeners) {
            listener.StatusChanged(id, s);
        }
    }

    private static JSONObject convertToSifTrain(JSONObject map, String name, double leadIn, double timeOffset) throws JSONException {
        JSONObject s = new JSONObject();
        s.put("song_name", name);
        s.put("difficulty", 4);
        s.put("lead_in", leadIn);

        JSONArray notes = new JSONArray();
        JSONArray lanes = map.getJSONArray("lane");
        for (int il = 0; il < lanes.length(); il++) {
            JSONArray lane = lanes.getJSONArray(il);
            for (int in = 0; in < lane.length(); in++) {
                JSONObject note = lane.getJSONObject(in);
                double time = note.getDouble("starttime") / 1000d;
                int effect = 1;
                double effectValue = 2d;
                if (note.getBoolean("longnote")) {
                    effect = 4;
                    effectValue = note.getDouble("endtime") / 1000d - time;
                }
                if (note.getBoolean("parallel")) {
                    effect += 16;
                }

                JSONObject newNote = new JSONObject();
                newNote.put("timing_sec", time + timeOffset);
                newNote.put("effect", effect);
                newNote.put("effect_value", effectValue);
                newNote.put("position", 9 - note.getInt("lane"));
                notes.put(newNote);
            }
        }

        s.put("song_info", new JSONArray().put(new JSONObject().put("notes", notes)));
        return s;
    }
}
