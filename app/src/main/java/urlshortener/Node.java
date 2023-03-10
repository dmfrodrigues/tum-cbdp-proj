package urlshortener;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.sun.net.httpserver.HttpServer;

import urlshortener.db.DatabaseMongoLong;
import urlshortener.db.DatabaseOrdered;
import urlshortener.raft.Raft;
import urlshortener.server.UrlShortenerHttpHandler;
import urlshortener.urlshortener.UrlShortener;
import urlshortener.urlshortener.UrlShortenerLong;

public class Node {    
    public DatabaseOrdered<Long> db;
    public UrlShortener urlShortener;

    private Raft raft;

    private ThreadPoolExecutor serverThreadPoolExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(2);
    private HttpServer server;

    public Node() throws IOException, SQLException, AlreadyBoundException, InterruptedException, NotBoundException, ServerNotActiveException{
        String myAddress = getMyAddress();

        System.setProperty("java.rmi.server.hostname", myAddress);

        db = new DatabaseMongoLong("mongodb://localhost:27017");
        db.seed(false);
        db.init();

        raft = new Raft(myAddress, db, db);
        urlShortener = new UrlShortenerLong(db, raft);

        register();

        server();
    }

    private void register() throws AlreadyBoundException, IOException, java.rmi.AlreadyBoundException{
        register(Registry.REGISTRY_PORT);
    }

    private void register(int port) throws AlreadyBoundException, IOException, java.rmi.AlreadyBoundException {
        // Instantiating the implementation class
        raft.register();

        File file = new File("/tmp/registered");
        file.createNewFile();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try {
            raft.deregister();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        } }));

        System.err.println("Raft registered to RMI registry");
    }

    private void server() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8001), 0);
        server.createContext("/", new UrlShortenerHttpHandler(urlShortener));
        server.setExecutor(serverThreadPoolExecutor);
        server.start();

        System.err.println("Server running");
    }

    public void run() throws InterruptedException, NotBoundException, ServerNotActiveException, IOException{
        raft.run();
    }

    public static String getMyAddress() throws UnknownHostException {
        try {
            Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
            while(n.hasMoreElements()){
                NetworkInterface e = n.nextElement();
                // System.out.println("Interface: " + e.getName());
                if(e.getName().equals("lo")) continue;
                Enumeration<InetAddress> a = e.getInetAddresses();
                while(a.hasMoreElements()){
                    InetAddress addr = a.nextElement();
                    // System.out.println("  " + addr.getHostAddress() + " " + (addr instanceof Inet4Address));
                    if(addr instanceof Inet4Address){
                        return addr.getHostAddress();
                    }
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        System.err.println("Could not find my address in decent way, using the sometimes-incorrect way");
        return InetAddress.getLocalHost().getHostAddress(); // often returns "127.0.0.1"
    }
    
    public void join(String peerAddress) throws RemoteException, NotBoundException, ServerNotActiveException{
        raft.join(peerAddress);
    }
}
