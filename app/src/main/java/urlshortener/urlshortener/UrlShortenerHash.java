package urlshortener.urlshortener;

import java.util.Base64;

public class UrlShortenerHash implements UrlShortener {
    public UrlShortenerHash(){
    }

    public String shorten(String url){
        Integer hash = url.hashCode();
        String hashString = hash.toString();
        return new String(Base64.getEncoder().encode(hashString.getBytes()));
    }
}
