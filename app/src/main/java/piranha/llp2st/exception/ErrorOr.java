package piranha.llp2st.exception;

import java.io.IOException;

public class ErrorOr<T> {

    public Exception error;
    public T data;

    public ErrorOr(Exception error) {
        this.error = error;
    }

    public ErrorOr(T data) {
        this.data = data;
    }

    public boolean isError() {
        return error != null;
    }

    public static <T> ErrorOr<T> wrap(Exception ex) {
        if (ex instanceof LLPException) {
            return new ErrorOr<>(ex);
        } else if (ex instanceof IOException) {
            return new ErrorOr<>(new MyIOException((IOException)ex));
        } else {
            return new ErrorOr<>(new InternalException());
        }
    }
}
