package org.zalando.failsafeactuator;

import net.jodah.failsafe.CircuitBreaker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static net.jodah.failsafe.CircuitBreaker.State.CLOSED;
import static net.jodah.failsafe.CircuitBreaker.State.HALF_OPEN;
import static net.jodah.failsafe.CircuitBreaker.State.OPEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpEntity.EMPTY;
import static org.springframework.http.HttpMethod.GET;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SampleApplication.class, webEnvironment = RANDOM_PORT)
public class CircuitBreakersEndpointTest {

    private static final ParameterizedTypeReference<Map<String, CircuitBreakerView>> RESPONSE_TYPE =
            new ParameterizedTypeReference<Map<String, CircuitBreakerView>>() {
            };

    @Autowired
    private TestRestTemplate http;

    @Before
    public void before() {
        write("test", CLOSED);
    }

    @Test
    public void shouldReadAll() {
        final Map<String, CircuitBreakerView> breakers = readAll();

        assertEquals(2, breakers.size());
        assertTrue(breakers.containsKey("test"));
        assertTrue(breakers.containsKey("delay"));

        final CircuitBreakerView view = breakers.get("test");
        assertEquals(CLOSED, view.getState());
    }

    @Test
    public void shouldReadOne() {
        final CircuitBreakerView view = readOne("test");
        assertNotNull(view);
        assertEquals(CLOSED, view.getState());
    }

    @Test
    public void shouldReadUnknown() {
        final CircuitBreakerView unknown = readOne("unknown");
        assertNull(unknown);
    }

    @Test
    public void shouldOpen() {
        verifyTransitionTo(OPEN);
    }

    @Test
    public void shouldHalfOpen() {
        verifyTransitionTo(HALF_OPEN);
    }

    @Test
    public void shouldClose() {
        write("test", OPEN); // so we have an actual state change, since closed is the default

        verifyTransitionTo(CLOSED);
    }

    @Test
    public void shouldWriteUnknown() {
        assertNull(write("unknown", OPEN));
    }

    private void verifyTransitionTo(final CircuitBreaker.State state) {
        final CircuitBreakerView written = write("test", state);
        final CircuitBreakerView read = readOne("test");

        assertEquals(read, written);
        assertEquals(read.getState(), state);
    }

    private Map<String, CircuitBreakerView> readAll() {
        return http.exchange("/actuator/circuit-breakers", GET, EMPTY, RESPONSE_TYPE).getBody();
    }

    private CircuitBreakerView readOne(final String name) {
        return http.getForObject("/actuator/circuit-breakers/{name}",
                CircuitBreakerView.class, name);
    }

    private CircuitBreakerView write(final String test, final CircuitBreaker.State state) {
        return http.postForObject("/actuator/circuit-breakers/{name}",
                new CircuitBreakerView(state), CircuitBreakerView.class, test);
    }

}

