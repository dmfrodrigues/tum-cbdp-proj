package urlshortener.rmi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

public class MyRMISocketFactory extends RMISocketFactory {
    int timeoutMillis;

    public MyRMISocketFactory(int timeoutMillis){
        this.timeoutMillis = timeoutMillis;
    }

    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(timeoutMillis);
        socket.setSoLinger(false, 0);
        socket.connect(new InetSocketAddress(host, port), timeoutMillis);
        return socket;
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }
}
