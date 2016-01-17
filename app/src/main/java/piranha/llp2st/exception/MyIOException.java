package piranha.llp2st.exception;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MyIOException extends Exception {

    private IOException ex;

    public MyIOException(IOException ex) {
        this.ex = ex;
    }

    @Override
    public String getMessage() {
        if (ex instanceof FileNotFoundException)
            return "Couldn't read/write external storage.";
        else
            return "Couldn't connect to LLPractice."/* + ex.getMessage()*/;
    }
}
