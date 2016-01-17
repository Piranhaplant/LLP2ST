package piranha.llp2st.exception;

import org.json.JSONObject;

public class LLPException extends Exception {

    public static final int ERRCODE_NEEDLOGIN = 2;

    public String message;
    public int errcode;

    public LLPException(JSONObject json) {
        message = json.optString("message");
        errcode = json.optInt("errcode");
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static void ThrowIfError(JSONObject json) throws LLPException {
        if (json.has("succeed") && !json.optBoolean("succeed")) {
            throw new LLPException(json);
        }
    }
}
