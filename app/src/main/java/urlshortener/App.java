package urlshortener;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class App extends Node {
    public static void main(String args[]) {
        try {
            register();
            if(args[0] == "-j"){
                String peerHost = args[1];
                Registry registry = LocateRegistry.getRegistry(peerHost);
                Stub stub = (Stub)registry.lookup("node");
                stub.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void register() throws RemoteException, AlreadyBoundException {
        // Instantiating the implementation class
        Node obj = new Node();

        // Exporting the object of implementation class
        // (here we are exporting the remote object to the stub)
        Stub stub = (Stub) UnicastRemoteObject.exportObject(obj, 0);

        // Binding the remote object (stub) in the registry
        Registry registry = LocateRegistry.getRegistry();

        registry.bind("node", stub);
        System.err.println("Server ready");
    }
}
