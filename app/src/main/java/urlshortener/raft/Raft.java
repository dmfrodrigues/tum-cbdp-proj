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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import urlshortener.LogEntryContentPut;
import urlshortener.Utils;

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

    // Time between consecutive members gossip
    static private long LEADER_JOIN_GOSSIP_MILLIS = 1000;
    Set<String> members = new HashSet<>();
    ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    // Persistent state
    // TODO: all the persistent state variables must be stored in secondary
    // memory
    int currentTerm = 0;
    String votedFor;
    ArrayList<LogEntry> log = new ArrayList<>();

    // Volatile state
    int commitIndex = -1;
    int lastApplied = -1;

    // Volatile state on leader
    Map<String, Integer> nextIndex = null;
    Map<String, Integer> matchIndex = null;

    // Volatile state on followers
    AtomicLong heartbeatTimestampNanos = new AtomicLong(System.currentTimeMillis());

    //
    ExecutorService executor = Executors.newFixedThreadPool(4);

    public Raft(String myAddress){
        this.myAddress = myAddress;
        this.leaderAddress = myAddress;
        this.state = State.LEADER;
        members.add(myAddress);
        nextIndex = new HashMap<>(){{
            put(myAddress, 0);
        }};
        matchIndex = new HashMap<>(){{
            put(myAddress, -1);
        }};

        int seed = myAddress.hashCode();
        random = new Random(seed);

        FOLLOWER_TIMEOUT_MILLIS = Utils.Rand.inRange(random, FOLLOWER_TIMEOUT_MIN_MILLIS, FOLLOWER_TIMEOUT_MAX_MILLIS);

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

        Registry leaderRegistry = LocateRegistry.getRegistry(leaderAddress);
        RaftRemote leader = (RaftRemote)leaderRegistry.lookup("raft");
            
        synchronized(state){
            state = State.FOLLOWER;
            nextIndex = null;
            matchIndex = null;

            members = leader.joinRPC();
            System.out.println("Joined leader at " + leaderAddress + ", got list of members: [" + String.join(", ", members) + "]");

            startMembersGossip();

            heartbeatTimestampNanos.set(System.nanoTime());
        }
    }

    public void startMembersGossip(){
        Runnable membersGossipRunnable = new Runnable() {
            public void run() {
                while(true){
                    String peerAddress = null;

                    Set<String> membersCopy;
                    synchronized(members){
                        if(members.size() <= 1) break;
                        peerAddress = Utils.Rand.getRandomFromSet(random, members);
                        membersCopy = new HashSet<>(members);
                    }

                    if(peerAddress == null || peerAddress.equals(myAddress)){
                        peerAddress = null;
                        continue;
                    }
                    try {
                        // TODO: check if all members are online before gossipping
                        RaftRemote peer = Raft.connect(peerAddress);
                        Set<String> newMembers = peer.membersGossipRPC(membersCopy);
                        // System.out.println("Gossipped with " + peerAddress + " about network members");
                        
                        synchronized(members){
                            newMembers.removeAll(members);
                            if(newMembers.size() > 0)
                                System.out.println("Gossip response: just learned about members [" + String.join(", ", newMembers) + "]");
                        
                            members.addAll(newMembers);
                        }
                        break;
                    } catch (RemoteException e) {
                        System.err.println("Peer " + peerAddress + " is not working, removing from members");
                        members.remove(peerAddress);
                    } catch (NotBoundException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };

        scheduledExecutor.scheduleAtFixedRate(membersGossipRunnable, 0, LEADER_JOIN_GOSSIP_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public Set<String> membersGossipRPC(Set<String> members) throws RemoteException {
        members.removeAll(this.members);
        if(members.size() > 0)
            System.out.println("Gossip request: just learned about members [" + String.join(", ", members) + "]");

        Set<String> ret;
        synchronized(this.members){
            this.members.addAll(members);
            ret = new HashSet<>(this.members);
        }
        ret.removeAll(members);
        return ret;
    }

    @Override
    public Set<String> joinRPC() throws RemoteException, ServerNotActiveException, NotBoundException {
        if(state != State.LEADER){
            throw new InvalidStateError("Can only call joinRPC() if the callee is a leader");
        }
        String newPeerAddress = RemoteServer.getClientHost();
        synchronized(members){
            members.add(newPeerAddress);
            nextIndex.put(newPeerAddress, 0);
            matchIndex.put(newPeerAddress, -1);
        
            // Inform all members that a new peer has joined
            Iterator<String> it = members.iterator();
            while(it.hasNext()){
                String peerAddress = it.next();
                if(peerAddress.equals(myAddress) || peerAddress.equals(newPeerAddress)) continue;
                try {
                    RaftRemote peer = Raft.connect(peerAddress);
                    peer.addMemberRPC(newPeerAddress);
                } catch (RemoteException e) {
                    System.err.println("Peer " + peerAddress + " is not working, removing from members");
                    it.remove();
                }
            }
            System.out.println("Node " + newPeerAddress + " joined the network");

            return members;
        }
    }

    @Override
    public void addMemberRPC(String newPeerAddress) throws RemoteException, NotBoundException {
        if(state != State.FOLLOWER){
            throw new InvalidStateError("Can only call addMemberRPC() if the callee is a follower");
        }
        synchronized(members){ members.add(newPeerAddress); }
        System.out.println("Added member " + newPeerAddress);
    }

    @Override
    public RaftResponse<Boolean> appendEntriesRPC(int term, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
        // Initial work
        if(entries.size() > 0)
            System.out.println("Got non-empty appendEntriesRPC");

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
        while(lastApplied < log.size() && commitIndex > lastApplied){
            ++lastApplied;
            log.get(lastApplied).apply();
        }
        if(term > currentTerm){
            currentTerm = term;
            synchronized(state){
                state = State.FOLLOWER;
                nextIndex = null;
                matchIndex = null;
            }
        }

        // System.out.println("Updated heartbeat timestamp");
        heartbeatTimestampNanos.set(System.nanoTime());

        return new RaftResponse<Boolean>(currentTerm, true);
    }

    @Override
    public RaftResponse<Boolean> requestVoteRPC(int term, int lastLogIndex, int lastLogTerm) throws ServerNotActiveException {
        String candidateAddress = RemoteServer.getClientHost();

        if(term < currentTerm)
            return new RaftResponse<Boolean>(currentTerm, false);

        boolean candidateLogIsAtLeastAsUpToDateAsReceiverLog = true; // TODO

        if(
            (votedFor == null || votedFor.equals(candidateAddress)) &&
            candidateLogIsAtLeastAsUpToDateAsReceiverLog
        ){
            votedFor = candidateAddress;
            System.out.println("Candidate " + candidateAddress + " asked me to vote for term " + term + ", I said YES");
            heartbeatTimestampNanos.set(System.nanoTime());
            return new RaftResponse<Boolean>(currentTerm, true);
        }

        // Rules for all servers
        if(term > currentTerm){
            currentTerm = term;
            synchronized(state){
                state = State.FOLLOWER;
                nextIndex = null;
                matchIndex = null;
            }
        }
        
        return new RaftResponse<Boolean>(currentTerm, false);
    }

    public void run() throws InterruptedException, NotBoundException, ServerNotActiveException {
        long sleep = 0;
        while(true){
            synchronized(state){
                switch(state){
                    case LEADER:
                        sleep = loopLeader();
                        break;
                    case FOLLOWER:
                        sleep = loopFollower();
                        break;
                    case CANDIDATE:
                        sleep = loopCandidate();
                        break;
                    default:
                        break;
                }
            }
            Thread.sleep(sleep);
        }
    }

    private long loopLeader() throws InterruptedException, NotBoundException {
        long tBeginNanos = System.nanoTime();
        synchronized(members){
            // System.out.println("Sending heartbeat");
            Iterator<String> it = members.iterator();
            while(it.hasNext()){
                String peerAddress = it.next();
                if(peerAddress.equals(myAddress)) continue;
                try {
                    RaftRemote peer = Raft.connect(peerAddress);
                    heartbeatPeer(peer, peerAddress);
                    // System.out.println("    Sent heartbeat to " + peerAddress);
                } catch (RemoteException e) {
                    System.err.println("Peer " + peerAddress + " is not working, removing from members");
                    it.remove();
                }
            }

            while(true){
                int numberPeersThatHaveIndex = 0;
                it = members.iterator();
                while(it.hasNext()){
                    String peerAddress = it.next();
                    if(matchIndex.get(peerAddress) > commitIndex) ++numberPeersThatHaveIndex;
                }
                if(numberPeersThatHaveIndex >= members.size()/2 + 1){
                    ++commitIndex;
                    log.get(commitIndex).apply();
                } else break;
            }
        }
        long tEndNanos = System.nanoTime();
        long elapsedMillis = (tEndNanos-tBeginNanos)/1000000;
        long sleep = LEADER_HEARTBEAT_MILLIS-elapsedMillis;
        return sleep;
    }

    private void heartbeatPeer(RaftRemote peer, String peerAddress) throws RemoteException {
        int nextIndexPeer = nextIndex.get(peerAddress);

        while(true){
            int N = log.size();
            List<LogEntry> entries = new ArrayList<LogEntry>(log.subList(nextIndexPeer, N));
            RaftResponse<Boolean> response = peer.appendEntriesRPC(
                currentTerm,
                nextIndexPeer-1,
                (nextIndexPeer >= N ? -1 : log.get(nextIndexPeer).term),
                entries,
                commitIndex
            );
            if(response.get()){
                nextIndex.put(peerAddress, N);
                matchIndex.put(peerAddress, N-1);
                break;
            } else {
                nextIndex.put(peerAddress, nextIndex.get(peerAddress)-1);
            }
        }
    }

    private long loopFollower() throws InterruptedException {
        long nowMillis = System.nanoTime()/1000000;
        long heartbeatTimestampMillis = heartbeatTimestampNanos.get()/1000000;
        long elapsedMillis = nowMillis - heartbeatTimestampMillis;
        // System.out.println("Elapsed: " + elapsedMillis);
        if(elapsedMillis > FOLLOWER_TIMEOUT_MILLIS){
            System.out.println("Suspect leader is dead, I am now a candidate");
            synchronized(members){ members.remove(leaderAddress); }
            state = State.CANDIDATE;
            return 0;
        }

        long sleepUntilMillis = heartbeatTimestampMillis + FOLLOWER_TIMEOUT_MILLIS;
        nowMillis = System.nanoTime()/1000000;
        long delta = Math.max(sleepUntilMillis - nowMillis, FOLLOWER_SLEEP_MILLIS);
        return delta;
    }

    // Start election
    private long loopCandidate() throws NotBoundException, ServerNotActiveException {
        ++currentTerm;
        votedFor = myAddress;

        System.out.println("Starting election for term " + currentTerm);

        synchronized(members){
            // This node is alone, so he is automatically the leader
            if(members.size() <= 1){
                state = State.LEADER;
                System.out.println("Got elected leader for term " + currentTerm);
                return 0;
            }

            int numberVotes = 1;

            Iterator<String> it = members.iterator();
            while(state == State.CANDIDATE && it.hasNext()){
                String peerAddress = it.next();
                if(peerAddress.equals(myAddress)) continue;

                try {
                    RaftRemote peer = Raft.connect(peerAddress);
                    RaftResponse<Boolean> response = peer.requestVoteRPC(
                        currentTerm,
                        log.size()-1,
                        (log.size() >= 1 ? log.get(log.size()-1).term : -1)
                    );
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
                    nextIndex = new HashMap<>();
                    matchIndex = new HashMap<>();
                    for(String peerAddr: members){
                        nextIndex.put(peerAddr, log.size());
                        matchIndex.put(peerAddr, 0);
                    }
                    break;
                }
            }
        }

        return 0;
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

    public void appendEntry(LogEntryContentPut logEntryContent) throws InterruptedException, NotBoundException {
        if(!state.equals(State.LEADER))
            return;

        LogEntry logEntry = new LogEntry(currentTerm, logEntryContent);

        log.add(logEntry);

        matchIndex.put(myAddress, log.size());
        nextIndex.put(myAddress, log.size()+1);

        loopLeader();
    }
}
