package urlshortener;

import java.io.File;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class App extends Node {
    public static void main(String args[]) {
        try {
            register();

            if(args.length >= 2 && args[0].equals("-j")){
                String peerHost = args[1];
                System.out.println("Joining " + peerHost);
                Registry registry = LocateRegistry.getRegistry(peerHost);
                Stub stub = (Stub)registry.lookup("node");
                stub.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        System.err.println("Server ready");
    }

    private static void register() throws AlreadyBoundException, IOException {
        // Instantiating the implementation class
        Node obj = new Node();

        // Exporting the object of implementation class
        // (here we are exporting the remote object to the stub)
        Stub stub = (Stub) UnicastRemoteObject.exportObject(obj, 0);

        // Binding the remote object (stub) in the registry
        Registry registry = LocateRegistry.getRegistry();

        registry.bind("node", stub);

        File file = new File("/tmp/registered");
        file.createNewFile();
    }
}
