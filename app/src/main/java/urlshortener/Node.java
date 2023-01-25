package urlshortener;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.List;

public class Node implements NodeRemote {
    public enum State {
        PEER,
        CANDIDATE,
        LEADER
    }

    InetSocketAddress myAddress;
    InetSocketAddress leaderAddress;
    State state;

    List<InetSocketAddress> members = new ArrayList<>();

    public Node(InetSocketAddress myAddress){
        this.myAddress = myAddress;
        this.leaderAddress = myAddress;
        this.state = State.LEADER;
        members.add(myAddress);

        System.out.println("Created node with address " + myAddress);
    }

    /**
     * Make current node join a network, by using peer already in the network.
     * 
     * @param peerHost
     * @throws RemoteException
     * @throws NotBoundException
     * @throws ServerNotActiveException
     */
    public void joinNetwork(InetSocketAddress peerAddress) throws RemoteException, NotBoundException, ServerNotActiveException {
        Registry peerRegistry = LocateRegistry.getRegistry(peerAddress.getAddress().getHostAddress(), peerAddress.getPort());
        NodeRemote peer = (NodeRemote)peerRegistry.lookup("node");
        leaderAddress = peer.getLeaderAddress();

        System.out.println("Joining leader at " + leaderAddress);

        Registry leaderRegistry = LocateRegistry.getRegistry(leaderAddress.getAddress().getHostAddress(), leaderAddress.getPort());
        NodeRemote leader = (NodeRemote)leaderRegistry.lookup("node");
        
        leader.join(myAddress);
        state = State.PEER;
    }

    @Override
    public InetSocketAddress getLeaderAddress() throws RemoteException {
        return leaderAddress;
    }

    @Override
    public void join(InetSocketAddress newPeerAddress) throws RemoteException, ServerNotActiveException {
        if(state != State.LEADER){
            throw new InvalidStateError("Can only call join(InetSocketAddress) if the callee is a leader");
        }
        members.add(newPeerAddress);
        System.out.println("Node " + newPeerAddress + " joined the network");
    }
}
