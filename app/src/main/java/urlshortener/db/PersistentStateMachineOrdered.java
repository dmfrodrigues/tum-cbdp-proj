package urlshortener.db;

public interface PersistentStateMachineOrdered<T> extends PersistentStateMachine<T> {
    T getHighestKey();
}
