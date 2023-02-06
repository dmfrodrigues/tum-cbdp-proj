package urlshortener;

public class App {

    public static Node node;

    public static void main(String args[]) {
        try {
            node = new Node();

            if(args.length >= 2 && args[0].equals("-j")){
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
