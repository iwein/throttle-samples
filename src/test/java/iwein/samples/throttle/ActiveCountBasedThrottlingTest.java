package iwein.samples.throttle;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StopWatch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static java.lang.Math.sqrt;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * This class is severely underdocumented.
 *
 * @author iwein
 */
public class ActiveCountBasedThrottlingTest {
    private final BlockingQueue<Runnable> outQueue = new LinkedBlockingQueue<Runnable>();
    private final int poolSize = 100;
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(poolSize, poolSize, 60, TimeUnit.SECONDS, outQueue);
    private ThreadPoolActiveCountBasedThrottle<String> throttle;

    @Before
    public void setupResourceMapAndExecutorAndThrottle() {
        HashMap<String, Integer> thresholdPerResource = new HashMap<String, Integer>();
        thresholdPerResource.put("foo", poolSize);
        thresholdPerResource.put("bar", poolSize / 2);

        throttle = new ThreadPoolActiveCountBasedThrottle<String>(
                executor,
                Collections.unmodifiableMap(thresholdPerResource)
        );
    }

    public void cpuHeavy(long timeToWaste) {
        StopWatch stopWatch = new StopWatch();
        double output = 1;
        while (stopWatch.getTotalTimeMillis() < timeToWaste) {
            System.out.println(stopWatch.getTotalTimeMillis());
            stopWatch.start();
            for (int i = 0; i < 1000; i++) {
                output = output + sqrt(sqrt(sqrt(output)));
            }
            stopWatch.stop();
            System.out.println(stopWatch.getTotalTimeMillis());
        }
    }

    @Test
    public void shouldThrottleWhenFull() {
        int numberOfExecutions = poolSize;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(numberOfExecutions);
        final CountDownLatch finish = new CountDownLatch(numberOfExecutions);

        executeAfterStart(start, started, finish, numberOfExecutions);

        for (int i = 0; i < numberOfExecutions; i++) {
            throttle.tryAcquire("foo");
        }

        start.countDown();

        //foo is at threshold, executor is fully busy
        assertThat(throttle.tryAcquire("foo"), is(false));

        try {
            started.await();
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
                         cpuHeavy(5);
                         finish.countDown();
                     } catch (InterruptedException e) {
                         Thread.currentThread().interrupt();
                     }
                 }
             });
        }
    }

    @Test
    public void shouldThrottleWhenFullOnlyAboveThreshold() {
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(poolSize);
        final CountDownLatch finish = new CountDownLatch(poolSize);

        executeAfterStart(start, started, finish, poolSize);

        for (int i = 0; i < poolSize / 2; i++) {
            throttle.tryAcquire("foo");
            throttle.tryAcquire("bar");
        }

        start.countDown();

        //foo is below threshold, bar is at threshold
        assertThat(throttle.tryAcquire("foo"), is(true));
        assertThat(throttle.tryAcquire("bar"), is(false));

        try {
            started.await();
            finish.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(throttle.tryAcquire("foo"), is(true));
        assertThat(throttle.tryAcquire("bar"), is(true));
    }

    @Test
    public void shouldNotThrottleWhenAboveThresholdButNotFull() {
        final CountDownLatch start = new CountDownLatch(1);
        int numberOfExecutions = poolSize / 2;
        final CountDownLatch started = new CountDownLatch(numberOfExecutions);
        final CountDownLatch finish = new CountDownLatch(numberOfExecutions);

        executeAfterStart(start, started, finish, numberOfExecutions);

        for (int i = 0; i < numberOfExecutions; i++) {
            throttle.tryAcquire("bar");
        }

        start.countDown();

        //bar is at threshold, but executor isn't fully loaded yet
        assertThat(throttle.tryAcquire("bar"), is(true));

        try {
            started.await();
            finish.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(throttle.tryAcquire("foo"), is(true));
        assertThat(throttle.tryAcquire("bar"), is(true));
    }
}
