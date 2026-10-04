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
package it.water.core.service.traffic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TrafficCorrelationContext}, the per-thread correlation scope holder (F4,
 * ADR-7). Plain JUnit, no Mockito/{@code WaterTestExtension} needed: the class is a static
 * ThreadLocal utility with no collaborators.
 * <p>
 * Every test that opens a scope closes it before returning (or relies on the {@link #clearScope()}
 * safety net below), so nothing here leaks into another test class that happens to run later on
 * the same thread - {@link TrafficRecordPolicy} enriches {@code correlationId} straight off this
 * same ThreadLocal, so a leaked scope would silently corrupt an unrelated test's assertions.
 */
class TrafficCorrelationContextTest {

    @AfterEach
    void clearScope() {
        TrafficCorrelationContext.close();
    }

    @Test
    void open_generatesNonNullId() {
        String id = TrafficCorrelationContext.open();

        assertNotNull(id);
    }

    @Test
    void open_generatesDifferentIdOnEachCall() {
        String first = TrafficCorrelationContext.open();
        String second = TrafficCorrelationContext.open();

        assertNotEquals(first, second);
    }

    @Test
    void openWithId_adoptsGivenId() {
        String adopted = TrafficCorrelationContext.open("propagated-id");

        assertEquals("propagated-id", adopted);
        assertEquals("propagated-id", TrafficCorrelationContext.current());
    }

    @Test
    void openWithNullId_generatesFreshId() {
        String id = TrafficCorrelationContext.open(null);

        assertNotNull(id);
        assertEquals(id, TrafficCorrelationContext.current());
    }

    @Test
    void openWithBlankId_generatesFreshId() {
        String id = TrafficCorrelationContext.open("   ");

        assertNotNull(id);
        assertNotEquals("   ", id);
        assertEquals(id, TrafficCorrelationContext.current());
    }

    @Test
    void current_outsideAnyScope_returnsNull() {
        assertNull(TrafficCorrelationContext.current());
    }

    @Test
    void close_clearsTheScope() {
        TrafficCorrelationContext.open();

        TrafficCorrelationContext.close();

        assertNull(TrafficCorrelationContext.current());
    }

    @Test
    void close_withoutAnOpenScope_doesNotThrow() {
        assertDoesNotThrow(TrafficCorrelationContext::close);
        assertNull(TrafficCorrelationContext.current());
    }

    /**
     * Anti-leak guarantee (see the class Javadoc on {@link TrafficCorrelationContext}):
     * {@link TrafficCorrelationContext#open()} always OVERWRITES whatever was left on the thread, so
     * a pooled thread whose previous scope was never closed self-heals on its next request instead
     * of correlating two unrelated requests forever.
     */
    @Test
    void open_overwritesAnAlreadyOpenScope() {
        String first = TrafficCorrelationContext.open();

        String second = TrafficCorrelationContext.open();

        assertNotEquals(first, second);
        assertEquals(second, TrafficCorrelationContext.current());
    }
}
