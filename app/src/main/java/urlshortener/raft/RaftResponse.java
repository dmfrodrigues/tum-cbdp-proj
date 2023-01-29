package urlshortener.raft;

import java.io.Serializable;

public class RaftResponse<T> implements Serializable {
    private int term;
    private T t;

    public RaftResponse(int term, T t){
        this.term = term;
        this.t = t;
    }

    public int term(){ return term; }
    public T get(){ return t; }
}
