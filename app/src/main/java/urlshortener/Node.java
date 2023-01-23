package urlshortener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class Node implements Stub {
    @Override
    public void join() throws RemoteException {
        System.out.println("Someone asked me to join!");
    }
}
