package iwein.samples.throttle;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.StopWatch;

import java.util.concurrent.*;

import static java.lang.Math.sqrt;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * This class is severely underdocumented.
 *
 * @author iwein
 */
@Ignore //this stuff takes way too much time to run in a general build
public class ActiveCountBasedThrottlingTest {
    private final BlockingQueue<Runnable> outQueue = new LinkedBlockingQueue<Runnable>();
    private final int poolSize = 100;
    private final int numberOfLoops = 1000000;
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(poolSize, poolSize, 60, TimeUnit.SECONDS, outQueue);

    public void cpuHeavy() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        double output = 1;
        for(int i = 0 ; i< numberOfLoops;i++){
            output = output + sqrt(sqrt(sqrt(output)));
        }
        stopWatch.stop();
        System.out.println("Finished at timestamp "+ System.currentTimeMillis() +" in "+ stopWatch.getTotalTimeMillis()+" ms : "+output);
    }

    @Test
    public void shouldThrottleWhenFull() {
        int numberOfExecutions = poolSize * 3;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(numberOfExecutions);
        final CountDownLatch finish = new CountDownLatch(numberOfExecutions);

        executeAfterStart(start, started, finish, numberOfExecutions);

        Throttle<String> throttle = new ActiveCountBasedThrottle(executor);

        start.countDown();
        assertThat(throttle.tryAcquire("foo"), is(false));

        System.out.println("Executor active count: " + executor.getActiveCount());
        System.out.println("Queued: " + outQueue.size());
        try {
            started.await();
            System.out.println("Executor active count: " + executor.getActiveCount());
            System.out.println("Queued: " + outQueue.size());
            finish.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(throttle.tryAcquire("foo"), is(true));
    }

    private void executeAfterStart(final CountDownLatch start, final CountDownLatch started, final CountDownLatch finish, int numberOfExecutions) {
        for (int i = 0; i < numberOfExecutions; i++) {
             executor.execute(new Runnable() {
                 @Override
                 public void run() {
                     try {
                         start.await();
                         started.countDown();
                         cpuHeavy();
                         finish.countDown();
                     } catch (InterruptedException e) {
                         Thread.currentThread().interrupt();
                     }
                 }
             });
        }
    }
}
