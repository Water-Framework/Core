/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.core.service.traffic.interceptors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct, deterministic unit tests for the package-private {@link TrafficCallContext}: LIFO
 * push/pop semantics, the {@link TrafficCallContext#MAX_DEPTH} eviction cap (R3 leak mitigation),
 * and per-thread isolation. Lives in the same package as the class under test on purpose (the
 * class and its nested {@code Span} are package-private, by design not part of any public API).
 */
class TrafficCallContextTest {

    @AfterEach
    void drainThreadLocal() {
        // JUnit reuses the same execution thread across test methods (no parallel execution
        // configured for this module) - defensively drain any leftover span so a failed assertion
        // in one test can never leak state into the next.
        while (TrafficCallContext.pop() != null) {
            // draining
        }
    }

    /**
     * Builds a span with the tree fields left at their "outermost call" defaults, so the tests that
     * predate them stay focused on push/pop semantics.
     */
    private TrafficCallContext.Span span(long startNanos, String correlationId, String operation, String destination) {
        return new TrafficCallContext.Span(startNanos, correlationId, operation, destination,
                "record-" + correlationId, null, 0);
    }

    @Test
    void pushThenPop_singleSpan_roundTripsSameInstanceAndFields() {
        TrafficCallContext.Span span = span(123L, "corr-1", "op-1", "dest-1");

        TrafficCallContext.push(span);
        TrafficCallContext.Span popped = TrafficCallContext.pop();

        assertSame(span, popped);
        assertEquals(123L, popped.startNanos());
        assertEquals("corr-1", popped.correlationId());
        assertEquals("op-1", popped.operation());
        assertEquals("dest-1", popped.destination());
    }

    @Test
    void pop_whenNothingPushed_returnsNull() {
        assertNull(TrafficCallContext.pop());
    }

    @Test
    void pushMultiple_pop_returnsInLifoOrder() {
        TrafficCallContext.Span a = span(1L, "a", "opA", "destA");
        TrafficCallContext.Span b = span(2L, "b", "opB", "destB");
        TrafficCallContext.Span c = span(3L, "c", "opC", "destC");

        TrafficCallContext.push(a);
        TrafficCallContext.push(b);
        TrafficCallContext.push(c);

        assertSame(c, TrafficCallContext.pop());
        assertSame(b, TrafficCallContext.pop());
        assertSame(a, TrafficCallContext.pop());
        assertNull(TrafficCallContext.pop(), "deque must be empty (and ThreadLocal cleared) after the last pop");
    }

    @Test
    void push_beyondMaxDepth_evictsOldestSpans_keepsMostRecentMaxDepthOnes() {
        int total = TrafficCallContext.MAX_DEPTH + 5;
        for (int i = 0; i < total; i++) {
            TrafficCallContext.push(span(i, "corr-" + i, "op-" + i, "dest-" + i));
        }

        // the 5 oldest (corr-0..corr-4) must have been evicted; the remaining MAX_DEPTH most recent
        // ones pop back out in LIFO order (last pushed first)
        for (int i = total - 1; i >= 5; i--) {
            TrafficCallContext.Span popped = TrafficCallContext.pop();
            assertNotNull(popped, "expected a surviving span for corr-" + i);
            assertEquals("corr-" + i, popped.correlationId());
        }
        assertNull(TrafficCallContext.pop(), "the oldest 5 spans must have been evicted, deque should now be empty");
    }

    // ---------------------------------------------------------------------
    // Call tree: peek()/depth() are what let a nested call know its parent
    // ---------------------------------------------------------------------

    @Test
    void peek_returnsOpenSpanWithoutRemovingIt_andNullWhenOutermost() {
        assertNull(TrafficCallContext.peek(), "no open span means the next call is the outermost one");

        TrafficCallContext.Span outer = span(1L, "corr", "op", "dest");
        TrafficCallContext.push(outer);

        assertSame(outer, TrafficCallContext.peek());
        assertSame(outer, TrafficCallContext.peek(), "peek must not consume the span");
        assertSame(outer, TrafficCallContext.pop());
    }

    @Test
    void depth_growsWithNestingAndReturnsToZero() {
        assertEquals(0, TrafficCallContext.depth());

        TrafficCallContext.push(span(1L, "a", "opA", "destA"));
        assertEquals(1, TrafficCallContext.depth());
        TrafficCallContext.push(span(2L, "b", "opB", "destB"));
        assertEquals(2, TrafficCallContext.depth());

        TrafficCallContext.pop();
        assertEquals(1, TrafficCallContext.depth());
        TrafficCallContext.pop();
        assertEquals(0, TrafficCallContext.depth());
    }

    @Test
    void span_carriesTreeFields() {
        TrafficCallContext.Span nested = new TrafficCallContext.Span(
                9L, "corr-shared", "innerOp", "InnerService", "record-inner", "record-outer", 1);

        TrafficCallContext.push(nested);
        TrafficCallContext.Span popped = TrafficCallContext.pop();

        assertEquals("record-inner", popped.recordId());
        assertEquals("record-outer", popped.parentId());
        assertEquals(1, popped.depth());
    }

    @Test
    void spans_areIsolatedPerThread() throws InterruptedException {
        TrafficCallContext.push(span(1L, "main-thread-span", "op", "dest"));

        AtomicReference<TrafficCallContext.Span> otherThreadResult = new AtomicReference<>();
        Thread other = new Thread(() -> otherThreadResult.set(TrafficCallContext.pop()));
        other.start();
        other.join();

        assertNull(otherThreadResult.get(), "a different thread must not observe the main thread's span");
        TrafficCallContext.Span mainThreadSpan = TrafficCallContext.pop();
        assertNotNull(mainThreadSpan, "the main thread's span must still be there, untouched by the other thread");
        assertEquals("main-thread-span", mainThreadSpan.correlationId());
    }
}
