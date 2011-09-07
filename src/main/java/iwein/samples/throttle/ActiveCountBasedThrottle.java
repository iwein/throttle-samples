package iwein.samples.throttle;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * This class is severely underdocumented.
 *
 * @author iwein
 */
public class ActiveCountBasedThrottle implements Throttle<String> {
    private ThreadPoolExecutor executor;

    //TODO remove dependency on Executor implementation?
    public ActiveCountBasedThrottle(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean tryAcquire(String resource) {
        return executor.getActiveCount() < 20;
    }

    @Override
    public void release(String resource) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
