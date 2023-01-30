package urlshortener.urlshortener;

public interface Database {
    abstract public boolean seed() throws Exception;

    abstract public void put(String id, String url) throws Exception;

    abstract public String get(String id) throws Exception;
}
