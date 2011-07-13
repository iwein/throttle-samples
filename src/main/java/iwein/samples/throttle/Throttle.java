package iwein.samples.throttle;

/**
 * @author iwein
 */
public interface Throttle<T> {
    boolean tryAcquire(T resource);

    void release(T resource);
}
