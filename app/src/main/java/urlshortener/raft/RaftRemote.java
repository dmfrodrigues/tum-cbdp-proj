package urlshortener.raft;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.List;

public interface RaftRemote extends Remote {
    public String getLeaderAddress() throws RemoteException;

    /**
     * RPC. New peer asks leader to join the network.
     * 
     * @throws RemoteException
     * @throws ServerNotActiveException
     */
    public void joinRPC() throws RemoteException, ServerNotActiveException;

    public RaftResponse<Boolean> appendEntriesRPC(int term, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) throws RemoteException;

    public RaftResponse<Boolean> requestVoteRPC(int term, int lastLogIndex, int lastLogTerm) throws RemoteException, ServerNotActiveException;
}
