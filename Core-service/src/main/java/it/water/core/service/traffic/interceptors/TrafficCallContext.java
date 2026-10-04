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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @Author Aristide Cittadino.
 * Per-thread, nesting-safe correlation context for the S2S traffic capture. A {@link Span}
 * is pushed by {@link TrafficS2SInterceptor} on method entry and popped on successful return, so
 * nested calls on the same thread correlate correctly (LIFO) and form a call tree through
 * {@link Span#parentId()} / {@link Span#depth()}.
 * <p>
 * <b>R3 (leak mitigation).</b> The after-hook is NOT invoked on the exception path in
 * any runtime (Spring {@code @AfterReturning} fires on success only; OSGi
 * {@code OsgiServiceInterceptor} and the test {@code TestServiceProxy} run before &rarr; invoke
 * &rarr; after with no {@code finally}). An exception therefore leaves a Span un-popped. To keep
 * such a leak bounded, the deque is capped at {@link #MAX_DEPTH}: when the cap is exceeded the
 * OLDEST span (deque tail) is discarded before pushing the new one. A consequence is a possible
 * mis-correlation of an outer call to an inner span after an exception, which is an accepted
 * trade-off for the bounded, fail-safe telemetry.
 */
final class TrafficCallContext {

    /**
     * Maximum number of concurrently open spans retained per thread. Beyond this the oldest
     * span is evicted so a leak on the exception path (R3) stays bounded.
     */
    static final int MAX_DEPTH = 64;

    private static final ThreadLocal<Deque<Span>> SPANS = new ThreadLocal<>();

    private TrafficCallContext() {
        // utility holder, no instances
    }

    /**
     * Pushes a span for the current thread, evicting the oldest one if the depth cap is reached.
     *
     * @param span the span opened on method entry
     */
    static void push(Span span) {
        Deque<Span> deque = SPANS.get();
        if (deque == null) {
            deque = new ArrayDeque<>();
            SPANS.set(deque);
        }
        // bounded: drop the oldest (tail) span if a leak has filled the deque (R3)
        while (deque.size() >= MAX_DEPTH) {
            deque.pollLast();
        }
        deque.push(span);
    }

    /**
     * @return the currently open span WITHOUT removing it - i.e. the parent of a call that is about
     * to start on this thread - or {@code null} when this is the outermost call
     */
    static Span peek() {
        Deque<Span> deque = SPANS.get();
        return deque == null ? null : deque.peek();
    }

    /**
     * @return how many spans are currently open on this thread, i.e. the depth at which the next
     * call will sit
     */
    static int depth() {
        Deque<Span> deque = SPANS.get();
        return deque == null ? 0 : deque.size();
    }

    /**
     * Pops the most recent span for the current thread, cleaning up the ThreadLocal when the
     * deque becomes empty.
     *
     * @return the most recent span, or {@code null} if none is open (defensive)
     */
    static Span pop() {
        Deque<Span> deque = SPANS.get();
        if (deque == null || deque.isEmpty()) {
            return null;
        }
        Span span = deque.pop();
        if (deque.isEmpty()) {
            SPANS.remove();
        }
        return span;
    }

    /**
     * @Author Aristide Cittadino.
     * Immutable open-call marker carrying the timing and correlation data captured on method entry.
     */
    static final class Span {
        private final long startNanos;
        private final String correlationId;
        private final String operation;
        private final String destination;
        private final String recordId;
        private final String parentId;
        private final int depth;

        Span(long startNanos, String correlationId, String operation, String destination,
             String recordId, String parentId, int depth) {
            this.startNanos = startNanos;
            this.correlationId = correlationId;
            this.operation = operation;
            this.destination = destination;
            this.recordId = recordId;
            this.parentId = parentId;
            this.depth = depth;
        }

        long startNanos() {
            return startNanos;
        }

        /**
         * Identity of the record this span will produce, assigned on ENTRY rather than on exit: the
         * calls nested inside this one need it as their {@code parentId} while this one is still open.
         */
        String recordId() {
            return recordId;
        }

        String parentId() {
            return parentId;
        }

        int depth() {
            return depth;
        }

        String correlationId() {
            return correlationId;
        }

        String operation() {
            return operation;
        }

        String destination() {
            return destination;
        }
    }
}
