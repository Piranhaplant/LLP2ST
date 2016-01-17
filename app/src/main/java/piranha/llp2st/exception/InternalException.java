package piranha.llp2st.exception;

public class InternalException extends Exception {

    public InternalException() { }

    @Override
    public String getMessage() {
        return "Internal error";
    }
}
