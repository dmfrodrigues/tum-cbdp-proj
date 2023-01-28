package urlshortener;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.sun.net.httpserver.HttpServer;

import urlshortener.raft.Raft;
import urlshortener.raft.RaftRemote;
import urlshortener.server.UrlShortenerHttpHandler;
import urlshortener.urlshortener.Database;
import urlshortener.urlshortener.UrlShortener;
import urlshortener.urlshortener.UrlShortenerHash;

public class App {
    static Database db;
    static UrlShortener urlShortener;

    static Raft raft;
    static Node node;

    static ThreadPoolExecutor serverThreadPoolExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(2);
    static HttpServer server;

    public static void main(String args[]) {
        try {
            createUrlShortener();

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

    private static void createUrlShortener() throws SQLException {
        String POSTGRES_PASSWORD = System.getenv("POSTGRES_PASSWORD");
        db = new Database("jdbc:postgresql://localhost:5432/postgres", "postgres", POSTGRES_PASSWORD);

        System.out.println("Database connected");

        db.seed();

        urlShortener = new UrlShortenerHash(db);

        // System.out.println("URL shortener created");
    }

    private static void register() throws AlreadyBoundException, IOException{
        register(Registry.REGISTRY_PORT);
    }

    private static void register(int port) throws AlreadyBoundException, IOException {
        // Instantiating the implementation class
        String myAddress = InetAddress.getLocalHost().getHostAddress();
        raft = new Raft(myAddress);
        node = new Node(raft);

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
