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
        InputStream is = httpExchange.getRequestBody();
        OutputStream os = httpExchange.getResponseBody();
        StringBuilder stringBuilder = new StringBuilder();

        switch(httpExchange.getRequestMethod()){
            case "PUT":
                Scanner s = new Scanner(is).useDelimiter("\\A");
                String value = (s.hasNext() ? s.next() : "");

                System.out.println("Got request PUT " + value);

                String key = urlShortener.shorten(value);

                stringBuilder.append("localhost:8001/" + key);

                String response = stringBuilder.toString();
                httpExchange.sendResponseHeaders(200, response.length());
                os.write(response.getBytes());
                os.flush();
                os.close();

                break;
            case "GET":
                System.out.println("Got request GET");

                httpExchange.sendResponseHeaders(405, 0);
                break;
            default:
                httpExchange.sendResponseHeaders(405, 0);
                return;
        }
    }
}
