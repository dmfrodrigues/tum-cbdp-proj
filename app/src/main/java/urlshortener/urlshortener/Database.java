package urlshortener.urlshortener;

public interface Database {
    public boolean seed();

    public boolean put(String key, String value);

    public String get(String key);
}
