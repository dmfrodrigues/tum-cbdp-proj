package urlshortener;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class App {
    static Node node;

    public static void main(String args[]) {
        try {
            register();

            if(args.length >= 2 && args[0].equals("-j")){
                String[] peerAddressParts = args[1].split(":");
                String peerHost = peerAddressParts[0];
                int peerPort = Integer.parseInt(peerAddressParts[1]);
                InetSocketAddress peerAddress = new InetSocketAddress(peerHost, peerPort);
                node.joinNetwork(peerAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        System.err.println("Server ready");
    }

    private static void register() throws AlreadyBoundException, IOException{
        register(Registry.REGISTRY_PORT);
    }

    private static void register(int port) throws AlreadyBoundException, IOException {
        // Instantiating the implementation class
        InetSocketAddress myAddress = new InetSocketAddress(InetAddress.getLocalHost(), port);
        node = new Node(myAddress);

        // Exporting the object of implementation class
        // (here we are exporting the remote object to the stub)
        NodeRemote stub = (NodeRemote) UnicastRemoteObject.exportObject(node, 0);

        // Binding the remote object (stub) in the registry
        Registry registry = LocateRegistry.getRegistry();

        registry.bind("node", stub);

        File file = new File("/tmp/registered");
        file.createNewFile();
    }
}
