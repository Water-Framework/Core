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

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.model.events.ApplicationEventListener;
import it.water.core.api.model.events.Event;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.traffic.events.TrafficEvent;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficRecord;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InMemoryTrafficPublisher}.
 * <p>
 * Deliberately using DIRECT instantiation (not {@code WaterTestExtension}) and Mockito mocks for
 * {@link ComponentRegistry}/{@link ApplicationProperties}: this class is registered as a
 * {@code @FrameworkComponent} and shares a SINGLE {@code TestComponentRegistry}/framework-lifecycle
 * across a module's WHOLE test run, which makes several of the branches below impossible to
 * exercise deterministically through the real harness:
 * <ul>
 *   <li>forcing a tiny bounded queue to reliably trigger overflow drops without racing the real
 *       async worker thread of an already-running, framework-managed singleton instance;</li>
 *   <li>a listener that deliberately throws, to prove per-listener error isolation, without
 *       permanently registering a misbehaving component into the shared registry for the rest of
 *       the module's test run;</li>
 *   <li>defensive branches ({@code applicationProperties == null}, {@code componentRegistry == null},
 *       an invalid/non-positive queue-size property, {@code findComponents} throwing or returning
 *       {@code null}) that never occur through normal framework wiring.</li>
 * </ul>
 * The real, DI-resolved end-to-end wiring (real {@code ComponentRegistry}, real
 * {@code TrafficReporterImpl}, a listener registered as a genuine test component) is separately
 * covered by {@link TrafficReporterEgressHarnessTest}.
 * <p>
 * NOTE: worker startup is intentionally NOT triggered via a public {@code activate()}/{@code @OnActivate}
 * method (there isn't one on this class): {@link InMemoryTrafficPublisher} lazily starts its worker on
 * the FIRST {@link InMemoryTrafficPublisher#publish(TrafficEvent)} call instead, specifically so that
 * {@code @Inject} fields are already populated by then in the real (proxied) DI scenario. So every test
 * below that needs to establish a specific queue capacity does so by calling {@code publish(...)} once
 * (not a separate activation step) before asserting/continuing.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class InMemoryTrafficPublisherTest {

    private static final String PROP_QUEUE_SIZE = "water.traffic.publisher.queue.size";
    private static final int DEFAULT_QUEUE_SIZE = 10000;
    private static final long AWAIT_MS = 3000L;

    @Mock
    private ComponentRegistry componentRegistry;

    @Mock
    private ApplicationProperties applicationProperties;

    private InMemoryTrafficPublisher publisher;

    @AfterEach
    void shutdownPublisher() {
        if (publisher != null) {
            publisher.deactivate();
        }
    }

    private InMemoryTrafficPublisher newPublisher() {
        publisher = new InMemoryTrafficPublisher();
        return publisher;
    }

    private TrafficEvent<?> sampleEvent(String recordId) {
        TrafficRecord record = WaterTrafficRecord.builder().recordId(recordId).recordType(RecordType.API).build();
        return new WaterTrafficEvent<>(record, null, null);
    }

    // ---------------------------------------------------------------- capacity resolution (lazy start)

    @Test
    void publish_firstCall_withConfiguredQueueSizeProperty_resolvesThatCapacity() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("4");

        p.publish(sampleEvent("cap-1"));

        assertEquals(4, p.queueCapacity());
    }

    @Test
    void publish_firstCall_withInvalidQueueSizeProperty_fallsBackToDefaultCapacity() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("not-a-number");

        p.publish(sampleEvent("cap-2"));

        assertEquals(DEFAULT_QUEUE_SIZE, p.queueCapacity());
    }

    @Test
    void publish_firstCall_withNonPositiveQueueSizeProperty_fallsBackToDefaultCapacity() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("0");

        p.publish(sampleEvent("cap-3"));

        assertEquals(DEFAULT_QUEUE_SIZE, p.queueCapacity());
    }

    @Test
    void publish_firstCall_withNullApplicationProperties_fallsBackToDefaultCapacity() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(null);

        p.publish(sampleEvent("cap-4"));

        assertEquals(DEFAULT_QUEUE_SIZE, p.queueCapacity());
    }

    @Test
    void publish_calledTwice_startIsIdempotent_secondCallDoesNotReReadProperty() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("4");

        p.publish(sampleEvent("cap-5a"));
        assertEquals(4, p.queueCapacity());

        // change the stub - if the lazy start were NOT idempotent, capacity would flip to 8.
        // lenient: this stub is intentionally expected to be UNUSED (that is the assertion) — the
        // second publish() must not re-read the property, so strict stubbing would flag it.
        lenient().when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("8");
        p.publish(sampleEvent("cap-5b"));

        assertEquals(4, p.queueCapacity(), "worker startup triggered by a second publish() must be a no-op while already running");
    }

    @Test
    void deactivate_whenNeverStarted_doesNotThrow() {
        InMemoryTrafficPublisher p = newPublisher();
        assertDoesNotThrow(p::deactivate);
    }

    // ---------------------------------------------------------------- publish() basic contract

    @Test
    void publish_nullEvent_isNoOpAndCountersStayZero() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        p.setComponentRegistry(componentRegistry);

        assertDoesNotThrow(() -> p.publish(null));

        assertEquals(0L, p.enqueued());
        assertEquals(0L, p.published());
        assertEquals(0L, p.droppedOverflow());
        assertEquals(0L, p.droppedError());
        verifyNoInteractions(componentRegistry);
    }

    @Test
    void queueSize_beforeAnyPublish_isZero() {
        InMemoryTrafficPublisher p = newPublisher();
        assertEquals(0, p.queueSize());
    }

    @Test
    void isAvailable_and_publisherType_returnFixedValues() {
        InMemoryTrafficPublisher p = newPublisher();
        assertTrue(p.isAvailable());
        assertEquals("memory", p.publisherType());
    }

    // ---------------------------------------------------------------- dispatch to listeners

    @Test
    void publish_firstCall_lazilyStartsWorkerAndDispatchesToRegisteredListener() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        p.setComponentRegistry(componentRegistry);
        lenient().when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("100");

        ApplicationEventListener listener = mock(ApplicationEventListener.class);
        when(componentRegistry.findComponents(eq(ApplicationEventListener.class), any())).thenReturn(List.of(listener));

        TrafficEvent<?> event = sampleEvent("lazy-start-1");
        p.publish(event);

        verify(listener, timeout(AWAIT_MS)).consumerEvent(event.record(), (Event) event);
        assertTrue(p.enqueued() >= 1L);
    }

    @Test
    void publish_dispatchesToAllRegisteredListeners() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        p.setComponentRegistry(componentRegistry);
        lenient().when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("100");

        ApplicationEventListener listenerA = mock(ApplicationEventListener.class);
        ApplicationEventListener listenerB = mock(ApplicationEventListener.class);
        when(componentRegistry.findComponents(eq(ApplicationEventListener.class), any()))
                .thenReturn(Arrays.asList(listenerA, listenerB));

        TrafficEvent<?> event = sampleEvent("multi-listener-1");
        p.publish(event);

        verify(listenerA, timeout(AWAIT_MS)).consumerEvent(event.record(), (Event) event);
        verify(listenerB, timeout(AWAIT_MS)).consumerEvent(event.record(), (Event) event);
    }

    @Test
    void publish_oneListenerThrows_isolatesFailure_andStillNotifiesOtherListener_andCountsDroppedError() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        p.setComponentRegistry(componentRegistry);
        lenient().when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("100");

        ApplicationEventListener throwingListener = mock(ApplicationEventListener.class);
        ApplicationEventListener healthyListener = mock(ApplicationEventListener.class);
        doThrow(new RuntimeException("boom")).when(throwingListener).consumerEvent(any(), any());
        when(componentRegistry.findComponents(eq(ApplicationEventListener.class), any()))
                .thenReturn(Arrays.asList(throwingListener, healthyListener));

        TrafficEvent<?> event = sampleEvent("error-isolation-1");
        p.publish(event);

        verify(healthyListener, timeout(AWAIT_MS)).consumerEvent(event.record(), (Event) event);
        verify(throwingListener, timeout(AWAIT_MS)).consumerEvent(event.record(), (Event) event);
        // wait for the outer dispatch() finally-block to run too
        verify(componentRegistry, timeout(AWAIT_MS)).findComponents(eq(ApplicationEventListener.class), any());
        assertTrue(p.droppedError() >= 1L);
        assertTrue(p.published() >= 1L);
    }

    @Test
    void publish_findComponentsReturnsNull_doesNotThrow_andStillCountsPublished() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        p.setComponentRegistry(componentRegistry);
        lenient().when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("100");
        when(componentRegistry.findComponents(eq(ApplicationEventListener.class), any())).thenReturn(null);

        p.publish(sampleEvent("null-listeners-1"));

        await(() -> p.published() >= 1L);
        assertEquals(0L, p.droppedError());
    }

    @Test
    void publish_componentRegistryThrowsOnFindComponents_isIsolatedAsDroppedError() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        p.setComponentRegistry(componentRegistry);
        lenient().when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("100");
        when(componentRegistry.findComponents(eq(ApplicationEventListener.class), any()))
                .thenThrow(new NoComponentRegistryFoundException());

        p.publish(sampleEvent("registry-throws-1"));

        await(() -> p.published() >= 1L);
        assertTrue(p.droppedError() >= 1L);
    }

    @Test
    void publish_componentRegistryNull_publishesWithoutDispatch_andNoErrorCounted() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        p.setComponentRegistry(null);
        lenient().when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("100");

        p.publish(sampleEvent("null-registry-1"));

        await(() -> p.published() >= 1L);
        assertEquals(0L, p.droppedError());
    }

    // ---------------------------------------------------------------- overflow

    @Test
    void publish_queueFull_incrementsDroppedOverflow_withoutThrowing() {
        InMemoryTrafficPublisher p = newPublisher();
        p.setApplicationProperties(applicationProperties);
        p.setComponentRegistry(componentRegistry);
        // tiny bounded queue
        when(applicationProperties.getPropertyOrDefault(anyString(), anyString())).thenReturn("1");
        // the worker thread will pick up whichever event is polled and get stuck "dispatching" it for a
        // while, guaranteeing the capacity-1 queue fills up while the rapid-fire loop below is running.
        // lenient: the stub is consumed by the async worker thread, so whether it is invoked before the
        // test thread finishes is racy — strict stubbing would intermittently flag it as unnecessary.
        lenient().when(componentRegistry.findComponents(eq(ApplicationEventListener.class), any())).thenAnswer(invocation -> {
            Thread.sleep(3000);
            return Collections.emptyList();
        });

        // the very first publish() call in the loop lazily starts the worker with capacity=1
        for (int i = 0; i < 25; i++) {
            assertDoesNotThrow(() -> p.publish(sampleEvent("overflow-" + System.nanoTime())));
        }

        assertTrue(p.droppedOverflow() > 0L, "expected at least one overflow drop with a capacity-1 queue under a slow consumer");
        assertEquals(25L, p.enqueued() + p.droppedOverflow(), "every publish() call must increment exactly one of enqueued/droppedOverflow");
    }

    private void await(java.util.function.BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + AWAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting for async condition");
            }
        }
        fail("Timed out waiting for async condition");
    }
}
