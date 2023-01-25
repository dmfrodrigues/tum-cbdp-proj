package urlshortener;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import urlshortener.raft.Raft;
import urlshortener.raft.RaftRemote;

public class App {
    static Raft raft;
    static Node node;

    public static void main(String args[]) {
        try {
            register();

            if(args.length >= 2 && args[0].equals("-j")){
                String peerAddress = args[1];
                node.join(peerAddress);
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
        String myAddress = InetAddress.getLocalHost().getHostAddress();
        raft = new Raft(myAddress);
        node = new Node(raft);

        // Exporting the object of implementation class
        // (here we are exporting the remote object to the stub)
        RaftRemote stub = (RaftRemote) UnicastRemoteObject.exportObject(raft, 0);

        // Binding the remote object (stub) in the registry
        Registry registry = LocateRegistry.getRegistry();

        registry.bind("raft", stub);

        File file = new File("/tmp/registered");
        file.createNewFile();
    }
}
