package urlshortener.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import urlshortener.urlshortener.UrlShortener;

public class UrlShortenerHttpHandler implements HttpHandler {
    private UrlShortener urlShortener;

    public UrlShortenerHttpHandler(UrlShortener urlShortener){
        this.urlShortener = urlShortener;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        OutputStream os = httpExchange.getResponseBody();

        try {
            switch(httpExchange.getRequestMethod()){
                case "PUT": {
                    InputStream is = httpExchange.getRequestBody();
                    Scanner s = new Scanner(is);
                    s.useDelimiter("\\A");
                    String value = (s.hasNext() ? s.next() : "");
                    s.close();

                    String key = urlShortener.shorten(value);

                    httpExchange.sendResponseHeaders(200, key.length());
                    os.write(key.getBytes());
                    os.flush();
                    os.close();

                    break;
                }
                case "GET": {
                    String[] uriParts = httpExchange.getRequestURI().toString().split("/");
                    String key = uriParts[uriParts.length-1];

                    String url = urlShortener.enlongate(key);

                    if(url == null){
                        httpExchange.sendResponseHeaders(404, 0);
                        break;
                    }

                    httpExchange.getResponseHeaders().set("Location", url);

                    httpExchange.sendResponseHeaders(301, 0);

                    break;
                }
                default:
                    httpExchange.sendResponseHeaders(405, 0);
                    break;
            }

            os.flush();
            os.close();
        } catch(Exception e){
            e.printStackTrace();
            httpExchange.sendResponseHeaders(500, 0);
            os.flush();
            os.close();
        }
    }
}
