package urlshortener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {

    public static Node node;

    private static Logger logger = LogManager.getLogger(App.class.getName());

    public static void main(String args[]) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
                logger.info("Shutting down...");
           }));

            node = new Node();

            if (args.length >= 2 && args[0].equals("-j")) {
                String peerAddress = args[1];
                node.join(peerAddress);
            }

            node.run();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
