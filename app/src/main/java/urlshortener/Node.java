package urlshortener;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.List;

public class Node implements NodeRemote {
    public enum State {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }

    String myAddress;
    String leaderAddress;
    State state;

    ArrayList<String> members = new ArrayList<>();

    // Persistent state
    int currentTerm = 0;
    String votedFor;
    ArrayList<LogEntry> log = new ArrayList();

    // Volatile state
    int commitIndex = 0;
    int lastApplied = 0;

    public Node(String myAddress){
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
    public void joinNetwork(String peerAddress) throws RemoteException, NotBoundException, ServerNotActiveException {
        Registry peerRegistry = LocateRegistry.getRegistry(peerAddress);
        NodeRemote peer = (NodeRemote)peerRegistry.lookup("node");
        leaderAddress = peer.getLeaderAddress();

        System.out.println("Joining leader at " + leaderAddress);

        Registry leaderRegistry = LocateRegistry.getRegistry(leaderAddress);
        NodeRemote leader = (NodeRemote)leaderRegistry.lookup("node");
        
        leader.join();
        state = State.FOLLOWER;
    }

    @Override
    public String getLeaderAddress() throws RemoteException {
        return leaderAddress;
    }

    @Override
    public void join() throws RemoteException, ServerNotActiveException {
        if(state != State.LEADER){
            throw new InvalidStateError("Can only call join(InetSocketAddress) if the callee is a leader");
        }
        String newPeerAddress = RemoteServer.getClientHost();
        members.add(newPeerAddress);
        System.out.println("Node " + newPeerAddress + " joined the network");
    }

    @Override
    public RaftResponse<Boolean> appendEntries(int term, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
        // 1.
        if(term < currentTerm)
            return new RaftResponse<Boolean>(currentTerm, false);
        // 2.
        if(prevLogIndex >= log.size() || log.get(prevLogIndex).term != term)
            return new RaftResponse<Boolean>(currentTerm, false);
        // 3.
        for(int i = prevLogIndex + 1; i < log.size(); ++i){
            int k = i - prevLogIndex;
            if(k >= entries.size()) break;
            if(log.get(i).term != entries.get(k).term){
                log.subList(i, log.size()).clear();
                break;
            }
        }
        // 4.
        log.addAll(entries.subList(log.size()-prevLogIndex, entries.size()));
        // 5.
        if(leaderCommit > commitIndex){
            int indexOfLastNewEntry = prevLogIndex + entries.size();
            commitIndex = Math.min(leaderCommit, indexOfLastNewEntry);
        }

        // Rules for all servers
        while(commitIndex > lastApplied){
            ++lastApplied;
            log.get(lastApplied).apply();
        }
        if(term > currentTerm){
            currentTerm = term;
            state = State.FOLLOWER;
        }

        return new RaftResponse<Boolean>(currentTerm, true);
    }

    @Override
    public RaftResponse<Boolean> requestVote(int term, int lastLogIndex, int lastLogTerm) throws ServerNotActiveException {
        String candidateAddress = RemoteServer.getClientHost();

        if(term < currentTerm)
            return new RaftResponse<Boolean>(currentTerm, false);

        boolean candidateLogIsAtLeastAsUpToDateAsReceiverLog = true;

        if(
            (votedFor == null || votedFor.equals(candidateAddress)) &&
            candidateLogIsAtLeastAsUpToDateAsReceiverLog
        ){
            votedFor = candidateAddress;
            return new RaftResponse<Boolean>(currentTerm, true);
        }

        // Rules for all servers
        if(term > currentTerm){
            currentTerm = term;
            state = State.FOLLOWER;
        }
        
        return new RaftResponse<Boolean>(currentTerm, false);
    }
}
