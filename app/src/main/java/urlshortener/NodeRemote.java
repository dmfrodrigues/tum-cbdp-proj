package urlshortener;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.List;

public interface NodeRemote extends Remote {
    public String getLeaderAddress() throws RemoteException;

    /**
     * RPC. New peer asks leader to join the network.
     * 
     * @throws RemoteException
     * @throws ServerNotActiveException
     */
    public void join() throws RemoteException, ServerNotActiveException;

    public RaftResponse<Boolean> appendEntries(int term, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) throws RemoteException;

    public RaftResponse<Boolean> requestVote(int term, int lastLogIndex, int lastLogTerm) throws RemoteException, ServerNotActiveException;
}
