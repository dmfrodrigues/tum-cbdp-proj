package urlshortener.raft;

public class InvalidStateError extends IllegalStateException {
    public InvalidStateError(String arg0) {
        super(arg0);
    }
}
