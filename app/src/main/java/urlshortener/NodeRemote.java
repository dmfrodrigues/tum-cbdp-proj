package urlshortener;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

public interface NodeRemote extends Remote {
    public InetSocketAddress getLeaderAddress() throws RemoteException;

    /**
     * RPC. New peer asks leader to join the network.
     * 
     * @throws RemoteException
     * @throws ServerNotActiveException
     */
    public void join(InetSocketAddress newPeerAddress) throws RemoteException, ServerNotActiveException;
}
