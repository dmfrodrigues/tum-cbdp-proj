package urlshortener.raft;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class Raft implements RaftRemote {
    private static RaftRemote connect(String peerAddress) throws RemoteException, NotBoundException {
        Registry peerRegistry = LocateRegistry.getRegistry(peerAddress);
        RaftRemote peer = (RaftRemote)peerRegistry.lookup("raft");
        return peer;
    }

    static private long LEADER_HEARTBEAT_MILLIS = 500;
    static private long FOLLOWER_TIMEOUT_MIN_MILLIS = 1000;
    static private long FOLLOWER_TIMEOUT_MAX_MILLIS = 2000;

    private long FOLLOWER_TIMEOUT_MILLIS;

    // Minimum amount that follower sleeps between loop executions.
    // This allows to release some CPU time.
    private long FOLLOWER_SLEEP_MILLIS = 50;

    private Random random;

    public enum State {
        FOLLOWER,
        CANDIDATE,
        LEADER
    }

    String myAddress;
    String leaderAddress;
    State state;

    Set<String> members = new HashSet<>();

    // Persistent state
    // TODO: all the persistent state variables must be stored in secondary
    // memory
    int currentTerm = 0;
    String votedFor;
    ArrayList<LogEntry> log = new ArrayList<>();

    // Volatile state
    int commitIndex = 0;
    int lastApplied = 0;

    // Volatile state on leader
    Map<String, Integer> nextIndex = null;
    Map<String, Integer> matchIndex = null;

    // Volatile state on followers
    AtomicLong heartbeatTimestampNanos = new AtomicLong(System.currentTimeMillis());

    public Raft(String myAddress){
        this.myAddress = myAddress;
        this.leaderAddress = myAddress;
        this.state = State.LEADER;
        members.add(myAddress);
        nextIndex = new HashMap<>();
        matchIndex = new HashMap<>();

        int seed = myAddress.hashCode();
        random = new Random(seed);

        long r = random.nextLong();
        FOLLOWER_TIMEOUT_MILLIS = FOLLOWER_TIMEOUT_MIN_MILLIS +
            (r % (FOLLOWER_TIMEOUT_MAX_MILLIS - FOLLOWER_TIMEOUT_MIN_MILLIS));

        System.out.println("Created node with address " + myAddress + ", timeout " + FOLLOWER_TIMEOUT_MILLIS + "ms");
    }

    @Override
    public String getLeaderAddress() throws RemoteException {
        return leaderAddress;
    }

    /**
     * Make current node join a network, by using peer already in the network.
     * 
     * @param peerHost
     * @throws RemoteException
     * @throws NotBoundException
     * @throws ServerNotActiveException
     */
    public void join(String peerAddress) throws RemoteException, NotBoundException, ServerNotActiveException {
        RaftRemote peer = connect(peerAddress);
        leaderAddress = peer.getLeaderAddress();

        System.out.println("Joining leader at " + leaderAddress);

        Registry leaderRegistry = LocateRegistry.getRegistry(leaderAddress);
        RaftRemote leader = (RaftRemote)leaderRegistry.lookup("raft");
        
        leader.joinRPC();

        state = State.FOLLOWER;
        nextIndex = null;
        matchIndex = null;
        heartbeatTimestampNanos.set(System.nanoTime());
    }

    @Override
    public void joinRPC() throws RemoteException, ServerNotActiveException {
        if(state != State.LEADER){
            throw new InvalidStateError("Can only call joinRPC() if the callee is a leader");
        }
        String newPeerAddress = RemoteServer.getClientHost();
        synchronized(members){
            members.add(newPeerAddress);
            nextIndex.put(newPeerAddress, 0);
            matchIndex.put(newPeerAddress, -1);
        }
        System.out.println("Node " + newPeerAddress + " joined the network");
    }

    @Override
    public RaftResponse<Boolean> appendEntriesRPC(int term, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
        // Initial work
        heartbeatTimestampNanos.set(System.nanoTime());

        // 1.
        if(term < currentTerm)
            return new RaftResponse<Boolean>(currentTerm, false);
        // 2.
        if(prevLogIndex >= 0 && (prevLogIndex >= log.size() || log.get(prevLogIndex).term != term))
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
        log.addAll(entries.subList(log.size()-prevLogIndex-1, entries.size()));
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
    public RaftResponse<Boolean> requestVoteRPC(int term, int lastLogIndex, int lastLogTerm) throws ServerNotActiveException {
        String candidateAddress = RemoteServer.getClientHost();

        if(term < currentTerm)
            return new RaftResponse<Boolean>(currentTerm, false);

        boolean candidateLogIsAtLeastAsUpToDateAsReceiverLog = true;

        if(
            (votedFor == null || votedFor.equals(candidateAddress)) &&
            candidateLogIsAtLeastAsUpToDateAsReceiverLog
        ){
            votedFor = candidateAddress;
            System.out.println("Candidate " + candidateAddress + " asked me to vote for term " + term + ", I said YES");
            return new RaftResponse<Boolean>(currentTerm, true);
        }

        // Rules for all servers
        if(term > currentTerm){
            currentTerm = term;
            state = State.FOLLOWER;
        }
        
        return new RaftResponse<Boolean>(currentTerm, false);
    }

    public void run() throws InterruptedException, NotBoundException, ServerNotActiveException {
        while(true){
            synchronized(state){
                switch(state){
                    case LEADER:
                        loopLeader();
                        break;
                    case FOLLOWER:
                        loopFollower();
                        break;
                    case CANDIDATE:
                        loopCandidate();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void loopLeader() throws InterruptedException, NotBoundException {
        long tBeginNanos = System.nanoTime();
        synchronized(members){
            Iterator<String> it = members.iterator();
            while(it.hasNext()){
                String peerAddress = it.next();
                if(peerAddress.equals(myAddress)) continue;
                try {
                    RaftRemote peer = Raft.connect(peerAddress);
                    int matchIndexPeer = matchIndex.get(peerAddress);
                    peer.appendEntriesRPC(
                        currentTerm,
                        matchIndexPeer,
                        (matchIndexPeer != -1 ? log.get(matchIndexPeer).term : -1),
                        new ArrayList<>(),
                        commitIndex
                    );
                    // TODO: do things with the result of appendEntriesRPC
                } catch (RemoteException e) {
                    System.err.println("Peer " + peerAddress + " is not working, removing from members");
                    it.remove();
                }
            }
        }
        long tEndNanos = System.nanoTime();
        long elapsedMillis = (tEndNanos-tBeginNanos)/1000000;
        Thread.sleep(LEADER_HEARTBEAT_MILLIS-elapsedMillis);
    }

    private void loopFollower() throws InterruptedException {
        long nowMillis = System.nanoTime()/1000000;
        long heartbeatTimestampMillis = heartbeatTimestampNanos.get()/1000000;
        long elapsedMillis = nowMillis - heartbeatTimestampMillis;
        // System.out.println("Elapsed: " + elapsedMillis);
        if(elapsedMillis > FOLLOWER_TIMEOUT_MILLIS){
            System.out.println("Suspect leader is dead, I am now a candidate");
            members.remove(leaderAddress);
            state = State.CANDIDATE;
            return;
        }

        long sleepUntilMillis = heartbeatTimestampMillis + FOLLOWER_TIMEOUT_MILLIS;
        nowMillis = System.nanoTime()/1000000;
        long delta = Math.max(sleepUntilMillis - nowMillis, FOLLOWER_SLEEP_MILLIS);
        Thread.sleep(delta);
    }

    // Start election
    private void loopCandidate() throws NotBoundException, ServerNotActiveException {
        ++currentTerm;
        votedFor = myAddress;

        System.out.println("Starting election for term " + currentTerm);

        // This node is alone, so he is automatically the leader
        if(members.size() <= 1){
            state = State.LEADER;
            System.out.println("Got elected leader for term " + currentTerm);
            return;
        }

        int numberVotes = 1;

        Iterator<String> it = members.iterator();
        while(state == State.CANDIDATE && it.hasNext()){
            String peerAddress = it.next();
            if(peerAddress.equals(myAddress)) continue;

            try {
                RaftRemote peer = Raft.connect(peerAddress);
                RaftResponse<Boolean> response = peer.requestVoteRPC(currentTerm, log.size()-1, log.get(log.size()-1).term);
                if(response.get()){
                    System.out.println("Got vote from " + peerAddress);
                    ++numberVotes;
                }
                // TODO: do something with the term in the response
            } catch (RemoteException e) {
                System.err.println("Peer " + peerAddress + " is not working, removing from members");
                it.remove();
            }

            if(numberVotes >= members.size()/2 + 1){
                state = State.LEADER;
                System.out.println("Got elected leader for term " + currentTerm);
                break;
            }
        }

    }

    public void register() throws RemoteException, AlreadyBoundException {
        Registry registry = LocateRegistry.getRegistry();
        RaftRemote stub = (RaftRemote) UnicastRemoteObject.exportObject(this, 0);
        registry.bind("raft", stub);
    }
    public void deregister() throws RemoteException, NotBoundException{
        Registry registry = LocateRegistry.getRegistry();
        registry.unbind("raft");
    }
}
