package urlshortener.urlshortener;

public interface PersistentStateMachineOrdered<T> extends PersistentStateMachine<T> {
    T getHighestKey();
}
