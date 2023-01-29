package urlshortener.raft;

import java.io.Serializable;

public interface LogEntryContent extends Serializable  {
    public void apply();
}
