package urlshortener;

import java.util.Random;

public class TestUtils {
    static public String getRandomString(Random r, int len){
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'

        return r.ints(leftLimit, rightLimit+1)
            .limit(len)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }

    static public String getRandomURL(Random r){
        return "https://www." + getRandomString(r, 16) + ".com";
    }
}
