package iwein.samples.throttle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This class is severely underdocumented.
 *
 * @author iwein
 */
public class ThreadPoolActiveCountBasedThrottle<T> implements Throttle<T> {
    private final ThreadPoolExecutor executor;
    private final Map<T, Semaphore> availablePerResource;

    public ThreadPoolActiveCountBasedThrottle(ThreadPoolExecutor executor, Map<T, Integer> thresholdPerResource) {
        this.availablePerResource = transform(thresholdPerResource);
        this.executor = executor;
    }

    private Map<T, Semaphore> transform(Map<T, Integer> maxConcurrentPerResource) {
        Map<T, Semaphore> newMap = new HashMap<T, Semaphore>(maxConcurrentPerResource.size());
        for (Map.Entry<T, Integer> entry : maxConcurrentPerResource.entrySet()) {
            newMap.put(entry.getKey(), new Semaphore(entry.getValue()));
        }
        return newMap;
    }

    @Override
    public boolean tryAcquire(T resource) {
        return availablePerResource.containsKey(resource) &&
                availablePerResource.get(resource).tryAcquire() || executor.getActiveCount() < executor.getMaximumPoolSize();
    }

    @Override
    public void release(T resource) {
        if (availablePerResource.containsKey(resource)) {
            availablePerResource.get(resource).release();
        }
    }
}
