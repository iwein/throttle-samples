package iwein.samples.throttle;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * @author iwein
 */
public class SimpleSharingThrottle<T> {
    private final Map<T, Semaphore> maxConcurrentPerResource;
    private final Logger log = LoggerFactory.getLogger(SimpleSharingThrottle.class);

    public SimpleSharingThrottle(Map<T, Integer> maxConcurrentPerResource) {
        this.maxConcurrentPerResource = transform(maxConcurrentPerResource);
    }

    private Map<T, Semaphore> transform(Map<T, Integer> maxConcurrentPerResource) {
        Map<T,Semaphore> newMap = new HashMap<T, Semaphore>(maxConcurrentPerResource.size());
        for (Map.Entry<T, Integer> entry : maxConcurrentPerResource.entrySet()) {
            newMap.put(entry.getKey(), new Semaphore(entry.getValue()));
        }
        return newMap;
    }

    public boolean tryAcquire(T resource) {
        Semaphore semaphore = maxConcurrentPerResource.get(resource);
        Assert.state(semaphore != null, "You're trying to acquire a token for an unknown resource.");
        assert semaphore != null;
        log.debug("Acquiring lock for resource [{}]; {} locks remaining", resource, semaphore.availablePermits());
        return semaphore.tryAcquire();
    }

    public void release(T resource) {
        Semaphore semaphore = maxConcurrentPerResource.get(resource);
        Assert.state(semaphore != null, "You're trying to release a token for an unknown resource.");
        assert semaphore != null;
        log.debug("Acquiring lock for resource [{}]; {} locks remaining", resource, semaphore.availablePermits());
        semaphore.release();
    }
}
