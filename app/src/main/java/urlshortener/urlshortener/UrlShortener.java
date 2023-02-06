package urlshortener.urlshortener;

import java.io.IOException;
import java.rmi.NotBoundException;

import urlshortener.raft.Raft;

public abstract class UrlShortener<T> {
    abstract public T shortenURL(String url);

    PersistentStateMachine<T> sm;
    Raft raft;

    public UrlShortener(PersistentStateMachine<T> sm, Raft raft){
        this.sm = sm;
        this.raft = raft;
    }

    public T shorten(String url){
        T id = shortenURL(url);

        try {
            if(!raft.appendEntryRPC(new LogEntryContentPut(id.toString(), url)))
                return null;
        } catch (InterruptedException | NotBoundException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return id;
    }

    public String enlongate(T id) {
        return sm.getKeyValue(id);
    }
}
