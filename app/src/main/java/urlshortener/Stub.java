package urlshortener;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Stub extends Remote {
    void join() throws RemoteException;
}
