package iwein.samples.throttle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * @author iwein
 */
public class SharingThrottle<T> implements Throttle<T> {
    private static final Logger log = LoggerFactory.getLogger(SharingThrottle.class);
    private final Map<T, RollOverSemaphore> semaphorePerResource;

    private SharingThrottle(Map<T, RollOverSemaphore> semaphorePerResource) {
        this.semaphorePerResource = semaphorePerResource;
    }

    public static <T> SharingThrottleBuilder<T> builderWith(T resource, int reserved, int rollOver){
        return new SharingThrottleBuilder<T>().and(resource, reserved, rollOver);
    }

    public static class SharingThrottleBuilder<T> {
        int maxShared = 0;
        private final Map<T, List<Integer>> reservedAndSharedPerResource = new HashMap<T, List<Integer>>();

        public SharingThrottleBuilder<T> and(T resource, int reserved, int shared) {
            maxShared = Math.max(maxShared, shared);
            reservedAndSharedPerResource.put(resource, Arrays.asList(reserved, shared));
            return this;
        }

        public SharingThrottle<T> build() {
            Map<T, RollOverSemaphore> semaphorePerResource = new HashMap<T, RollOverSemaphore>();
            Semaphore shared = new Semaphore(maxShared);
            for (Map.Entry<T, List<Integer>> entry : reservedAndSharedPerResource.entrySet()) {
                Integer reserved = entry.getValue().get(0);
                Integer rollOver = entry.getValue().get(1);
                semaphorePerResource.put(entry.getKey(),
                        new RollOverSemaphore(new Semaphore(reserved), new Semaphore(rollOver), shared));
            }
            return new SharingThrottle<T>(semaphorePerResource);
        }
    }

    @Override
    public boolean tryAcquire(T resource) {
        RollOverSemaphore semaphore = semaphorePerResource.get(resource);
        Assert.state(semaphore != null, "You're trying to acquire a token for an unknown resource.");
        assert semaphore != null;
        log.debug("Acquiring lock for resource [{}]; {} locks remaining", resource, semaphore.availablePermits());
        return semaphore.tryAcquire();
    }

    @Override
    public void release(T resource) {
        RollOverSemaphore semaphore = semaphorePerResource.get(resource);
        Assert.state(semaphore != null, "You're trying to release a token for an unknown resource.");
        assert semaphore != null;
        log.debug("Acquiring lock for resource [{}]; {} locks remaining", resource, semaphore.availablePermits());
        semaphore.release();
    }

    private static class RollOverSemaphore {
        private final Semaphore reserved;
        private final Semaphore rollOver;
        private final Semaphore shared;

        private final Object monitor = new Object();
        private final int maxReservedPermits;

        /**
         * Create a RollOverSemaphore that acquires permits from a reserved pool, but also rolls over to a shared pool
         * if the need arises.
         * @param reserved the s
         * @param rollOver
         * @param shared
         */
        public RollOverSemaphore(Semaphore reserved, Semaphore rollOver, Semaphore shared) {
            this.reserved = reserved;
            this.rollOver = rollOver;
            this.shared = shared;
            this.maxReservedPermits = reserved.availablePermits();
        }

        public boolean tryAcquire() {
            synchronized (monitor) {
                return (reserved.tryAcquire() || (rollOver.tryAcquire() && shared.tryAcquire()));
            }
        }

        public int availablePermits() {
            synchronized (monitor) {
                return reserved.availablePermits() + rollOver.availablePermits();
            }
        }

        public void release() {
            synchronized (monitor) {
                if (reserved.availablePermits() < maxReservedPermits) {
                    reserved.release();
                } else {
                    rollOver.release();
                    shared.release();
                }
            }
        }
    }
}
