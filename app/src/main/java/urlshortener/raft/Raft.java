package urlshortener.raft;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectIOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import urlshortener.Utils;
import urlshortener.db.PersistentLog;
import urlshortener.db.PersistentMap;
import urlshortener.db.PersistentMap.Stored;
import urlshortener.rmi.MyRMISocketFactory;
import urlshortener.utils.Timer;

public class Raft implements RaftRemote {
    private Logger logger = LogManager.getLogger(Raft.class);
    public static RaftRemote connect(String peerAddress) throws RemoteException, NotBoundException {
        Registry peerRegistry = LocateRegistry.getRegistry(peerAddress);
        RaftRemote peer = (RaftRemote) peerRegistry.lookup("raft");
        return peer;
    }

    static private int RMI_TIMEOUT_MILLIS = 100;
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
    Members members = new Members();
    ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    // Persistent state
    Stored<Integer> currentTerm;
    Stored<String> votedFor;
    PersistentLog log;

    // Volatile state
    int commitIndex = -1;
    int lastApplied = -1;

    // Volatile state on leader
    Map<String, Integer> nextIndex = null;
    Map<String, Integer> matchIndex = null;

    // Volatile state on followers
    Timer heartbeatTimer = new Timer(TimeUnit.MILLISECONDS);
    //
    ExecutorService executor = Executors.newFixedThreadPool(4);

    public Raft(String myAddress, PersistentMap<String, ?> map, PersistentLog log) throws IOException {
        this.myAddress = myAddress;
        this.leaderAddress = myAddress;
        this.state = State.LEADER;

        currentTerm = map.loadStoredVariable("currentTerm", 0);
        votedFor = map.loadStoredVariable("votedFor", null);
        this.log = log;

        members.add(myAddress);
        nextIndex = new HashMap<>() {
            {
                put(myAddress, log.size());
            }
        };
        matchIndex = new HashMap<>() {
            {
                put(myAddress, log.size() - 1);
            }
        };

        int seed = myAddress.hashCode();
        random = new Random(seed);

        FOLLOWER_TIMEOUT_MILLIS = Utils.Rand.inRange(random, FOLLOWER_TIMEOUT_MIN_MILLIS, FOLLOWER_TIMEOUT_MAX_MILLIS);

        logger.info("Created node with address " + myAddress + ", timeout " + FOLLOWER_TIMEOUT_MILLIS + "ms");
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
        logger.info("Joining node at " + peerAddress);

        RaftRemote peer = connect(peerAddress);
        leaderAddress = peer.getLeaderAddress();

        logger.info("Joining leader at " + leaderAddress);
        RaftRemote leader = connect(leaderAddress);

        synchronized (state) {
            state = State.FOLLOWER;
            nextIndex = null;
            matchIndex = null;

            members = leader.joinRPC();
            logger.info("Joined leader at " + leaderAddress + ", got list of members: [" + String.join(", ", members) + "]");

            startMembersGossip();

            heartbeatTimer.tic();
        }
    }

    public void startMembersGossip() {
        Runnable membersGossipRunnable = new Runnable() {
            public void run() {
                while (true) {
                    String peerAddress = null;

                    Members membersCopy;
                    synchronized (members) {
                        if (members.size() <= 1)
                            break;
                        peerAddress = Utils.Rand.getRandomFromSet(random, members);
                        members.checkIfAlive();
                        membersCopy = new Members(members);
                    }

                    if (peerAddress == null || peerAddress.equals(myAddress)) {
                        peerAddress = null;
                        continue;
                    }

                    Members newMembers = new Members();
                    try {
                        boolean b = membersCopy.call(peerAddress, (RaftRemote peer) -> {
                            newMembers.addAll(peer.membersGossipRPC(membersCopy));
                        });
                        if (b) {
                            // System.out.println("Gossipped with " + peerAddress + " about network
                            // members");

                            synchronized (members) {
                                newMembers.removeAll(members);
                                if (newMembers.size() > 0)
                                    logger.info("Gossip response: just learned about members [" + String.join(", ", newMembers) + "]");

                                members.addAll(newMembers);
                            }
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };

        scheduledExecutor.scheduleAtFixedRate(membersGossipRunnable, 0, LEADER_JOIN_GOSSIP_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public Members membersGossipRPC(Members members) throws RemoteException {
        members.removeAll(this.members);
        if (members.size() > 0)
            logger.info("Gossip request: just learned about members [" + String.join(", ", members) + "]");

        Members ret;
        synchronized (this.members) {
            this.members.checkIfAlive();
            this.members.addAll(members);
            ret = new Members(this.members);
        }
        ret.removeAll(members);
        return ret;
    }

    @Override
    public Members joinRPC() throws RemoteException, ServerNotActiveException, NotBoundException {
        if (state != State.LEADER) {
            throw new InvalidStateError("Can only call joinRPC() if the callee is a leader");
        }
        String newPeerAddress = RemoteServer.getClientHost();
        synchronized (members) {
            members.add(newPeerAddress);
            nextIndex.put(newPeerAddress, 0);
            matchIndex.put(newPeerAddress, -1);

            // Inform all members that a new peer has joined
            try {
                members.forEach((String peerAddress, RaftRemote peer) -> {
                    if (peerAddress.equals(myAddress) || peerAddress.equals(newPeerAddress))
                        return;
                    peer.addMemberRPC(newPeerAddress);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("Node " + newPeerAddress + " joined the network");

            return members;
        }
    }

    @Override
    public void addMemberRPC(String newPeerAddress) throws RemoteException, NotBoundException {
        if (state != State.FOLLOWER) {
            throw new InvalidStateError("Can only call addMemberRPC() if the callee is a follower");
        }
        synchronized (members) {
            members.add(newPeerAddress);
        }
        logger.info("Added member " + newPeerAddress);
    }

    @Override
    public RaftResponse<Boolean> appendEntriesRPC(int term, int prevLogIndex, int prevLogTerm, List<LogEntry> entries,
            int leaderCommit) throws IOException {
        // if(entries.size() > 0)
        // System.out.println("Got non-empty appendEntriesRPC");

        // 1.
        if (term < currentTerm.get())
            return new RaftResponse<Boolean>(currentTerm.get(), false);
        // 2.
        if (prevLogIndex >= 0 && (prevLogIndex >= log.size() || log.get(prevLogIndex).term != term))
            return new RaftResponse<Boolean>(currentTerm.get(), false);
        // 3.
        for (int i = prevLogIndex + 1; i < log.size(); ++i) {
            int k = i - prevLogIndex;
            if (k >= entries.size())
                break;
            if (log.get(i).term != entries.get(k).term) {
                log.deleteAfter(i);
                break;
            }
        }
        // 4.
        log.addAll(entries.subList(log.size() - prevLogIndex - 1, entries.size()));
        // 5.
        if (leaderCommit > commitIndex) {
            int indexOfLastNewEntry = prevLogIndex + entries.size();
            commitIndex = Math.min(leaderCommit, indexOfLastNewEntry);
        }

        // Rules for all servers
        while (lastApplied < log.size() && commitIndex > lastApplied) {
            ++lastApplied;
            log.get(lastApplied).apply();
        }
        if (term > currentTerm.get()) {
            currentTerm.set(term);
            synchronized (state) {
                state = State.FOLLOWER;
                nextIndex = null;
                matchIndex = null;
            }
        }

        // System.out.println("Updated heartbeat timestamp");
        heartbeatTimer.tic();

        return new RaftResponse<Boolean>(currentTerm.get(), true);
    }

    @Override
    public RaftResponse<Boolean> requestVoteRPC(int term, int lastLogIndex, int lastLogTerm)
            throws ServerNotActiveException, IOException {
        String candidateAddress = RemoteServer.getClientHost();

        if (term < currentTerm.get())
            return new RaftResponse<Boolean>(currentTerm.get(), false);

        boolean candidateLogIsAtLeastAsUpToDateAsReceiverLog = true; // TODO

        if ((votedFor.get() == null || candidateAddress.equals(votedFor.get())) &&
                candidateLogIsAtLeastAsUpToDateAsReceiverLog) {
            votedFor.set(candidateAddress);
            logger.info("Candidate " + candidateAddress + " asked me to vote for term " + term + ", I said YES");
            heartbeatTimer.tic();
            return new RaftResponse<Boolean>(currentTerm.get(), true);
        }

        // Rules for all servers
        if (term > currentTerm.get()) {
            currentTerm.set(term);
            synchronized (state) {
                state = State.FOLLOWER;
                nextIndex = null;
                matchIndex = null;
            }
        }

        return new RaftResponse<Boolean>(currentTerm.get(), false);
    }

    public void run() throws InterruptedException, NotBoundException, ServerNotActiveException, IOException {
        long sleep = 0;
        while (true) {
            synchronized (state) {
                switch (state) {
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
            TimeUnit.MILLISECONDS.sleep(sleep);
        }
    }

    private long loopLeader() throws InterruptedException, NotBoundException, IOException {
        Timer timer = new Timer(TimeUnit.MILLISECONDS);
        timer.tic();

        synchronized (members) {

            // System.out.println("Sending heartbeat");
            try {
                members.forEach((String peerAddress, RaftRemote peer) -> {
                    if (peerAddress.equals(myAddress))
                        return;
                    try {
                        // System.out.println(" Trying to send heartbeat to " + peerAddress);
                        heartbeatPeer(peer, peerAddress);
                        // System.out.println(" Sent heartbeat to " + peerAddress);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            int oldCommitIndex = commitIndex;
            while (true) {
                int numberPeersThatHaveIndex = 0;
                for (String peerAddress : members) {
                    if (matchIndex.get(peerAddress) > commitIndex)
                        ++numberPeersThatHaveIndex;
                }
                if (numberPeersThatHaveIndex >= members.size() / 2 + 1 && commitIndex + 1 < log.size()) {
                    ++commitIndex;
                    log.get(commitIndex).apply();
                } else
                    break;
            }

            /**
             * This allows that, if there are commits, an extraordinary
             * heartbeat is sent to commit it. This does not make Raft
             * linearizable. It merely expedites the process of committing log
             * entries in the peers.
             */
            if (oldCommitIndex != commitIndex) {
                loopLeader();
            }
        }

        return LEADER_HEARTBEAT_MILLIS - timer.toc();
    }

    private void heartbeatPeer(RaftRemote peer, String peerAddress) throws IOException {
        int nextIndexPeer = nextIndex.get(peerAddress);

        while (true) {
            int N = log.size();
            List<LogEntry> entries = new ArrayList<>(log.getAfter(nextIndexPeer));
            RaftResponse<Boolean> response = peer.appendEntriesRPC(
                    currentTerm.get(),
                    nextIndexPeer - 1,
                    (nextIndexPeer >= N ? -1 : log.get(nextIndexPeer).term),
                    entries,
                    commitIndex);
            if (response.get()) {
                nextIndex.put(peerAddress, N);
                matchIndex.put(peerAddress, N - 1);
                break;
            } else {
                nextIndex.put(peerAddress, nextIndex.get(peerAddress) - 1);
            }
        }
    }

    private long loopFollower() throws InterruptedException {
        // System.out.println("Elapsed: " + heartbeatTimer.toc());
        if (heartbeatTimer.toc() > FOLLOWER_TIMEOUT_MILLIS) {
            logger.info("Suspect leader is dead, I am now a candidate");
            synchronized (members) {
                members.remove(leaderAddress);
            }
            state = State.CANDIDATE;
            return 0;
        }

        long delta = Math.max(FOLLOWER_TIMEOUT_MILLIS - heartbeatTimer.toc(), FOLLOWER_SLEEP_MILLIS);
        return delta;
    }

    // Start election
    private long loopCandidate() throws NotBoundException, ServerNotActiveException, IOException {
        Timer timer = new Timer(TimeUnit.MILLISECONDS);

        currentTerm.set(currentTerm.get() + 1);
        votedFor.set(myAddress);

        logger.info("Starting election for term " + currentTerm);

        synchronized (members) {
            // This node is alone, so he is automatically the leader
            if (members.size() <= 1) {
                state = State.LEADER;
                logger.info("Got elected leader for term " + currentTerm);
                return 0;
            }

            int numberVotes = 1;

            Set<String> abortedMembers = new HashSet<>();
            /**
             * These futures complete with:
             * - True, if the candidate got a vote
             * - False, if the candidate did not get a vote
             * - null, if the system is already in a term that is greater than
             *   that of the candidate. So the candidate has to step down.
             */
            List<CompletableFuture<Boolean>> futures = members
                    .stream()
                    .map((String peerAddress) -> {
                        CompletableFuture<Boolean> future = new CompletableFuture<>();
                        executor.submit(() -> {
                            try {
                                RaftRemote peer = Raft.connect(peerAddress);
                                RaftResponse<Boolean> response = peer.requestVoteRPC(
                                        currentTerm.get(),
                                        log.size() - 1,
                                        (log.size() >= 1 ? log.get(log.size() - 1).term : -1));
                                if (response.get()) {
                                    logger.info("Got vote from " + peerAddress);
                                }
                                Boolean ret = response.get();
                                if(response.term() > currentTerm.get())
                                    ret = null;
                                future.complete(ret);
                            } catch (ConnectIOException e) {
                                System.err.println("Peer " + peerAddress + " not working");
                                synchronized (abortedMembers) {
                                    abortedMembers.add(peerAddress);
                                }
                            } catch (ServerNotActiveException | IOException | NotBoundException e) {
                                e.printStackTrace();
                            }
                        });
                        return future;
                    })
                    .collect(Collectors.toList());

            while (futures.size() > 0 && numberVotes < members.size() / 2 + 1) {
                CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(
                        futures.toArray(new CompletableFuture[futures.size()]));

                long sleep = FOLLOWER_TIMEOUT_MILLIS - timer.toc();
                try {
                    Boolean b = (Boolean) anyFuture.get(sleep, TimeUnit.MILLISECONDS);
                    if(b == null){
                        return FOLLOWER_TIMEOUT_MILLIS - timer.toc();
                    }
                    numberVotes += (b ? 1 : 0);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    continue;
                } catch (TimeoutException e) {
                    System.out.println("Election timed out");
                    break;
                }
            }

            members.removeAll(abortedMembers);

            if (numberVotes >= members.size() / 2 + 1) {
                becomeLeader();
                return 0;
            }
        }

        // Did not get enough votes
        return FOLLOWER_TIMEOUT_MILLIS - timer.toc();
    }

    private void becomeLeader() {
        state = State.LEADER;
        logger.info("Became leader for term " + currentTerm);
        nextIndex = new HashMap<>();
        matchIndex = new HashMap<>();
        for (String peerAddr : members) {
            nextIndex.put(peerAddr, log.size());
            matchIndex.put(peerAddr, 0);
        }
    }

    public void register() throws AlreadyBoundException, IOException {
        Registry registry = LocateRegistry.getRegistry();
        RaftRemote stub = (RaftRemote) UnicastRemoteObject.exportObject(this, 0);
        registry.bind("raft", stub);

        RMISocketFactory.setSocketFactory(new MyRMISocketFactory(RMI_TIMEOUT_MILLIS));
    }

    public void deregister() throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry();
        registry.unbind("raft");
    }

    public boolean appendEntryRPC(LogEntryContent logEntryContent)
            throws InterruptedException, NotBoundException, IOException {
        if (!state.equals(State.LEADER)) {
            try {
                RaftRemote leader = Raft.connect(leaderAddress);
                return leader.appendEntryRPC(logEntryContent);
            } catch (RemoteException e) {
                return false;
            }
        }

        LogEntry logEntry = new LogEntry(currentTerm.get(), logEntryContent);

        log.add(logEntry);

        matchIndex.put(myAddress, log.size());
        nextIndex.put(myAddress, log.size() + 1);

        loopLeader();

        return true;
    }
}
