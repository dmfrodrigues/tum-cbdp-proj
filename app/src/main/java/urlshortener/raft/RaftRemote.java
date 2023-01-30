package urlshortener.raft;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.List;
import java.util.Set;

public interface RaftRemote extends Remote {
    public String getLeaderAddress() throws RemoteException;

    /**
     * RPC. New peer asks leader to join the network.
     * 
     * The semantics of this function is: "the caller wants to be joined to the
     * network that the callee belongs to".
     * 
     * @throws RemoteException
     * @throws ServerNotActiveException
     * @throws NotBoundException
     */
    public Set<String> joinRPC() throws RemoteException, ServerNotActiveException, NotBoundException;

    /**
     * RPC. Leader informs followers of the entry of a new member.
     * 
     * The semantics of this function is: "the caller (the leader)
     * informs the callee (a follower) that a new node with address 
     * newPeerAddress has joined the network".
     * 
     * @param newPeerAddress    Address of the new peer
     * @throws RemoteException
     * @throws NotBoundException
     */
    public void addMemberRPC(String newPeerAddress) throws RemoteException, NotBoundException;

    /**
     * RPC. Gossip about the members of the network.
     * 
     * The semantics of this function is: "the caller provides the set of
     * members it knows to the callee, and the callee replies with the set of
     * members it knows, and that were not in the caller's set of members".
     * 
     * @return  Set of members that the callee did not know
     */
    public Set<String> membersGossipRPC(Set<String> members) throws RemoteException;

    /**
     * RPC. Append entries.
     * 
     * The semantics of this function is: "the caller (the leader) informs the
     * callee (a follower) that the log entries in entries are to be appended
     * to the callee's log".
     * 
     * @param term          Term the leader is at
     * @param prevLogIndex  Index of the log entry before the 1st item in entries
     * @param prevLogTerm   Term of the log entry before the 1st item in entries
     * @param entries       List of new entries to be appended to the callee log
     * @param leaderCommit  Index of highest log entry known to be committed
     * @return              Response; contains the term of the callee, and the
     *                      result (success or otherwise)
     * @throws RemoteException
     * @throws IOException
     */
    public RaftResponse<Boolean> appendEntriesRPC(int term, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) throws RemoteException, IOException;

    /**
     * RPC. Request vote.
     * 
     * The semantics of this function is: "the caller (a candidate) requests the
     * vote of the callee (a follower), so that it may become a leader".
     * 
     * @param term          Term the candidate is at
     * @param lastLogIndex  Index of the last log entry of the candidate log
     * @param lastLogTerm   Term of the last log entry of the candidate log
     * @return              Response; contains the term of the callee, and the
     *                      result (success or otherwise)
     * @throws RemoteException
     * @throws ServerNotActiveException
     * @throws IOException
     */
    public RaftResponse<Boolean> requestVoteRPC(int term, int lastLogIndex, int lastLogTerm) throws RemoteException, ServerNotActiveException, IOException;

    public boolean appendEntryRPC(LogEntryContent logEntryContent) throws RemoteException, InterruptedException, NotBoundException, IOException;
}
