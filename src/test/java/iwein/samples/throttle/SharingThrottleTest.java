package iwein.samples.throttle;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author iwein
 */
public class SharingThrottleTest {

    SharingThrottle<String> throttle;

    @Before
    public void configureThrottle() {
        throttle = SharingThrottle.builderWith("foo", 5,5)
                .and("bar", 5, 5)
                .and("baz", 5, 5)
                .build();
    }

    @Test
    public void shouldAllowTrafficUpToMaxLoad() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(throttle.tryAcquire("foo"), is(true));
            assertThat(throttle.tryAcquire("foo"), is(true));
            assertThat(throttle.tryAcquire("bar"), is(true));
            assertThat(throttle.tryAcquire("baz"), is(true));
        }
        assertThat(throttle.tryAcquire("foo"), is(false));
        assertThat(throttle.tryAcquire("bar"), is(false));
        assertThat(throttle.tryAcquire("baz"), is(false));
    }

    @Test
    public void shouldReleaseProperly() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(throttle.tryAcquire("foo"), is(true));
            assertThat(throttle.tryAcquire("foo"), is(true));
            assertThat(throttle.tryAcquire("bar"), is(true));
            assertThat(throttle.tryAcquire("baz"), is(true));
        }
        for (int i = 0; i < 5; i++) {
            throttle.release("foo");
            throttle.release("foo");
            throttle.release("bar");
            throttle.release("baz");
        }
        for (int i = 0; i < 5; i++) {
            assertThat(throttle.tryAcquire("foo"), is(true));
            assertThat(throttle.tryAcquire("foo"), is(true));
            assertThat(throttle.tryAcquire("bar"), is(true));
            assertThat(throttle.tryAcquire("baz"), is(true));
        }
    }
}
