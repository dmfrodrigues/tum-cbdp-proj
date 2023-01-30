package urlshortener.raft;

import java.util.List;

public interface PersistentLog {
    /**
     * Size of log.
     * 
     * @return      Size of log
     */
    int size();

    /**
     * Get log entry at a position.
     * 
     * @param index Position from which to retrieve log entry
     * @return      Log entry at that position
     */
    LogEntry get(int index);

    /**
     * Delete all entries from a log that come after index, including the entry
     * at index.
     * 
     * @param index Position from which log entries are to be deleted
     * @return      True if successful, false otherwise
     */
    boolean deleteAfter(int index);

    /**
     * Append a single log entry to the end of the log.
     * 
     * @param logEntry  Log entry to append to the log.
     * @return          True if successful, false otherwise
     */
    boolean add(LogEntry logEntry);

    /**
     * Append all entries to the end of the log.
     * 
     * @param list  List of log entries to append to the log.
     * @return      True if successful, false otherwise
     */
    boolean addAll(List<LogEntry> list);

    /**
     * Get all log entries that come after and including index.
     * 
     * @param index Position form which log entries are to be returned
     * @return      List of log entries starting at index
     */
    List<LogEntry> getAfter(int index);
}
