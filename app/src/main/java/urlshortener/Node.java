package urlshortener;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

import urlshortener.raft.Raft;

public class Node {
    private Raft raft;

    public Node(Raft raft){
        this.raft = raft;
    }
    
    public void join(String peerAddress) throws RemoteException, NotBoundException, ServerNotActiveException{
        raft.join(peerAddress);
    }
}
