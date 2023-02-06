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
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.sun.net.httpserver.HttpServer;

import urlshortener.raft.Raft;
import urlshortener.server.UrlShortenerHttpHandler;
import urlshortener.urlshortener.Database;
import urlshortener.urlshortener.DatabasePostgres;
import urlshortener.urlshortener.UrlShortener;
import urlshortener.urlshortener.UrlShortenerHash;

public class App {
    static Database db;
    static UrlShortener urlShortener;

    static Raft raft;
    static Node node;

    static ThreadPoolExecutor serverThreadPoolExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(2);
    static HttpServer server;

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

    public static void main(String args[]) {
        try {
            String myAddress = getMyAddress();
            String POSTGRES_PASSWORD = System.getenv("POSTGRES_PASSWORD");

            DatabasePostgres databasePostgres = new DatabasePostgres("jdbc:postgresql://localhost:5432/postgres", "postgres", POSTGRES_PASSWORD);
            db = databasePostgres;
            db.seed(false);
            databasePostgres.loadLog();

            raft = new Raft(myAddress, db, db);
            node = new Node(raft);
            urlShortener = new UrlShortenerHash(db, raft);

            register();

            server();

            if(args.length >= 2 && args[0].equals("-j")){
                String peerAddress = args[1];
                node.join(peerAddress);
            }

            raft.run();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private static void register() throws AlreadyBoundException, IOException{
        register(Registry.REGISTRY_PORT);
    }

    private static void register(int port) throws AlreadyBoundException, IOException {
        // Instantiating the implementation class
        raft.register();

        File file = new File("/tmp/registered");
        file.createNewFile();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try {
            raft.deregister();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        } }));

        // System.err.println("Raft registered to RMI registry");
    }

    private static void server() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8001), 0);
        server.createContext("/", new UrlShortenerHttpHandler(urlShortener));
        server.setExecutor(serverThreadPoolExecutor);
        server.start();

        System.err.println("Server running");
    }
}
