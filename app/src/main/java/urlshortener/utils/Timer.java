package urlshortener.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class Timer {
    TimeUnit unit;
    long t1;
    ReadWriteLock rwLock;
    public Timer(TimeUnit unit){
        this.unit = unit;
        tic();
    }
    public TimeUnit getUnit(){
        return unit;
    }
    public void tic(){
        Lock lock = rwLock.writeLock();
        lock.lock();
        t1 = System.nanoTime();
        lock.unlock();
    }
    public long toc(){
        Lock lock = rwLock.readLock();
        lock.lock();
        long t2 = System.nanoTime();
        long delta = t2-t1;
        lock.unlock();
        return unit.convert(delta, TimeUnit.NANOSECONDS);
    }
}
