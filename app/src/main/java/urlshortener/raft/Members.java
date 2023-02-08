package urlshortener.raft;

import java.net.SocketTimeoutException;
import java.rmi.ConnectIOException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Iterator;

public class Members extends HashSet<String> {

    @FunctionalInterface
    public interface Function {
        void apply(RaftRemote peer) throws Exception;
    }

    @FunctionalInterface
    public interface FunctionPair {
        void apply(String peerAddress, RaftRemote peer) throws Exception;
    }

    public Members(){
        super();
    }

    public Members(Members members) {
        super(members);
    }
    
    public boolean call(String address, Function f) throws Exception {
        RaftRemote peer;
        try {
            peer = Raft.connect(address);
            f.apply(peer);
        } catch (SocketTimeoutException e) {
            System.err.println("Peer " + address + " not working");
            this.remove(address);
            return false;
        }
        
        return true;
    }

    public void checkIfAlive(){
        Iterator<String> it = iterator();
        while(it.hasNext()){
            String address = it.next();
            try {
                Raft.connect(address);
            } catch (ConnectIOException | ConnectException e) {
                System.err.println("Peer " + address + " not working");
                it.remove();
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void forEach(FunctionPair f) throws Exception {
        Iterator<String> it = iterator();
        while(it.hasNext()){
            String address = it.next();
            try {
                RaftRemote peer = Raft.connect(address);
                f.apply(address, peer);
            } catch (ConnectIOException | ConnectException e) {
                System.err.println("Peer " + address + " not working");
                it.remove();
            }
        }
    }
}
