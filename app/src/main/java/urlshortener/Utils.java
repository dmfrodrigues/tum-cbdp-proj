package urlshortener;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class Utils {
    static public class Rand {
        static public long inRange(Random r, long min, long max){
            long n = Math.abs(r.nextLong());
            return min + (n % (max - min));
        }

        public static <T> T getRandomFromSet(Random random, Set<T> s) {
            int r = (int)Utils.Rand.inRange(random, 0, s.size());
            Iterator<T> it = s.iterator();
            while(r > 0){
                it.next();
                --r;
            }
            return it.next();
        }
    }
}
