package iwein.samples.throttle;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author iwein
 */
public class SimpleSharingThrottleTest {
    SimpleSharingThrottle<String> throttle = new SimpleSharingThrottle<String>(
            mapOf("foo", 10).entry("bar", 10).entry("baz", 10).build()
    );

    @Test
    public void shouldAllowTrafficUpToMaxLoad() throws Exception {
        for (int i = 0; i < 10; i++) {
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
        for (int i = 0; i < 10; i++) {
            assertThat(throttle.tryAcquire("foo"), is(true));
            assertThat(throttle.tryAcquire("bar"), is(true));
            assertThat(throttle.tryAcquire("baz"), is(true));
        }
        for (int i = 0; i < 10; i++) {
            throttle.release("foo");
            throttle.release("bar");
            throttle.release("baz");
        }
        for (int i = 0; i < 10; i++) {
            assertThat(throttle.tryAcquire("foo"), is(true));
            assertThat(throttle.tryAcquire("bar"), is(true));
            assertThat(throttle.tryAcquire("baz"), is(true));
        }
    }

    private <S, T> MapBuilder<S, T> mapOf(S key, T value) {
        return new MapBuilder<S, T>(key, value);
    }

    private class MapBuilder<S, T> {
        private final Map<S, T> map = new HashMap<S, T>();

        public MapBuilder(S key, T value) {
            entry(key, value);
        }

        public MapBuilder<S, T> entry(S key, T value) {
            map.put(key, value);
            return this;
        }

        public Map<S, T> build() {
            return Collections.unmodifiableMap(map);
        }
    }
}
