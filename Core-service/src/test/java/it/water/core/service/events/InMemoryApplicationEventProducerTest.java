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
package it.water.core.service.events;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.entity.events.PostSaveEvent;
import it.water.core.api.entity.events.PostUpdateDetailedEvent;
import it.water.core.api.entity.events.PreSaveEvent;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.Resource;
import it.water.core.api.model.events.ApplicationEventListener;
import it.water.core.api.model.events.Event;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Direct unit tests for {@link InMemoryApplicationEventProducer}, the fall-back, synchronous
 * {@code ApplicationEventProducer} that materializes an event INSTANCE (a cached JDK proxy for the
 * normal, interface-typed Water events, or a plain instance for a concrete class with a default
 * constructor) out of the {@code Class} handed by an emitter, and delivers it to every registered
 * {@link ApplicationEventListener}.
 * <p>
 * Deliberately using Mockito directly for {@link ComponentRegistry} and for the listeners (plain
 * unit test, no {@code WaterTestExtension}): this gives full, deterministic control over the
 * registry-null / findComponents-throws / empty-or-null-listener-list / per-listener-isolation /
 * reflective-instantiation-failure branches, none of which are practical to force through a real,
 * DI-resolved registry. The real, end-to-end wiring (producer &rarr; {@code TrafficDomainEventListener}
 * &rarr; {@code TrafficReporterImpl} &rarr; {@code InMemoryTrafficPublisher}) is separately covered by
 * {@code it.water.core.service.traffic.DomainEventCaptureHarnessTest}.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class InMemoryApplicationEventProducerTest {

    private static final String PROP_LISTENERS_CACHE_TTL_MS = "water.events.listeners.cache.ttl.ms";

    @Mock
    private ComponentRegistry componentRegistry;

    private InMemoryApplicationEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new InMemoryApplicationEventProducer();
        producer.setComponentRegistry(componentRegistry);
    }

    private ApplicationEventListener<Resource> mockListener() {
        return mock(ApplicationEventListener.class);
    }

    @Test
    void produceEvent_deliversToAllRegisteredListeners_withCorrectResourceAndNonNullEvent() {
        ApplicationEventListener<Resource> listener1 = mockListener();
        ApplicationEventListener<Resource> listener2 = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener1, listener2));
        Resource resource = new Resource() {
        };

        producer.produceEvent(resource, PostSaveEvent.class);

        ArgumentCaptor<Event> captor1 = ArgumentCaptor.forClass(Event.class);
        verify(listener1).consumerEvent(eq(resource), captor1.capture());
        assertNotNull(captor1.getValue());

        ArgumentCaptor<Event> captor2 = ArgumentCaptor.forClass(Event.class);
        verify(listener2).consumerEvent(eq(resource), captor2.capture());
        assertNotNull(captor2.getValue());
    }

    @Test
    void produceDetailedEvent_deliversConsumerDetailedEventWithCorrectArgs() {
        ApplicationEventListener<Resource> listener = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));
        Resource before = new Resource() {
        };
        Resource after = new Resource() {
        };

        producer.produceDetailedEvent(before, after, PostUpdateDetailedEvent.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(listener).consumerDetailedEvent(eq(before), eq(after), captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void deliveredEventInstance_isInstanceOfRequestedInterface() {
        ApplicationEventListener<Resource> listener = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(listener).consumerEvent(any(), captor.capture());
        assertTrue(captor.getValue() instanceof PostSaveEvent);
    }

    @Test
    void eventInstance_cachedPerType_sameClassSameInstance_differentClassDifferentInstance() {
        ApplicationEventListener<Resource> listener = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);
        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);
        producer.produceEvent(new Resource() {
        }, PreSaveEvent.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(listener, times(3)).consumerEvent(any(), captor.capture());
        List<Event> delivered = captor.getAllValues();

        assertSame(delivered.get(0), delivered.get(1), "same event Class must deliver the same cached instance");
        assertNotSame(delivered.get(0), delivered.get(2), "different event Class must deliver a different instance");
    }

    @Test
    void proxyEquals_isReflexiveAndDistinctFromOtherEventInstance() {
        ApplicationEventListener<Resource> listener = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);
        producer.produceEvent(new Resource() {
        }, PreSaveEvent.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(listener, times(2)).consumerEvent(any(), captor.capture());
        Event eventA = captor.getAllValues().get(0);
        Event eventB = captor.getAllValues().get(1);

        assertEquals(eventA, eventA);
        assertNotEquals(eventA, eventB);
    }

    @Test
    void proxyToString_containsRequestedInterfaceName() {
        ApplicationEventListener<Resource> listener = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(listener).consumerEvent(any(), captor.capture());
        assertTrue(captor.getValue().toString().contains(PostSaveEvent.class.getName()));
    }

    @Test
    void proxyExecute_isNoOp_doesNotThrow() {
        ApplicationEventListener<Resource> listener = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(listener).consumerEvent(any(), captor.capture());
        PostSaveEvent event = (PostSaveEvent) captor.getValue();

        assertDoesNotThrow(() -> event.execute(new ExecuteFixtureEntity()));
    }

    @Test
    void registryNull_produceEvent_isSilentNoOp() {
        producer.setComponentRegistry(null);

        assertDoesNotThrow(() -> producer.produceEvent(new Resource() {
        }, PostSaveEvent.class));
    }

    @Test
    void findComponentsThrowsNoComponentRegistryFoundException_isSilentNoOp() {
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenThrow(new NoComponentRegistryFoundException());

        assertDoesNotThrow(() -> producer.produceEvent(new Resource() {
        }, PostSaveEvent.class));
    }

    @Test
    void listenersListEmpty_isNoOp() {
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> producer.produceEvent(new Resource() {
        }, PostSaveEvent.class));
    }

    @Test
    void listenersListNull_isNoOp() {
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(null);

        assertDoesNotThrow(() -> producer.produceEvent(new Resource() {
        }, PostSaveEvent.class));
    }

    @Test
    void isolation_firstListenerThrows_secondStillReceivesEvent_andExceptionDoesNotPropagate() {
        ApplicationEventListener<Resource> throwingListener = mockListener();
        ApplicationEventListener<Resource> healthyListener = mockListener();
        doThrow(new RuntimeException("boom")).when(throwingListener).consumerEvent(any(), any());
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(throwingListener, healthyListener));

        assertDoesNotThrow(() -> producer.produceEvent(new Resource() {
        }, PostSaveEvent.class));

        verify(healthyListener).consumerEvent(any(), any());
    }

    @Test
    void eventClassNull_isNoOp_noNpeAndNoRegistryInteraction() {
        assertDoesNotThrow(() -> producer.produceEvent(new Resource() {
        }, null));

        // eventInstance(null) short-circuits BEFORE dispatch() is ever reached
        verifyNoInteractions(componentRegistry);
    }

    @Test
    void concreteEventClassWithDefaultConstructor_isInstantiatedAndDelivered() {
        ApplicationEventListener<Resource> listener = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, ConcreteEvent.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(listener).consumerEvent(any(), captor.capture());
        assertTrue(captor.getValue() instanceof ConcreteEvent);
    }

    @Test
    void concreteEventClassWithoutAccessibleConstructor_noDeliveryNoException() {
        assertDoesNotThrow(() -> producer.produceEvent(new Resource() {
        }, ConcreteEventNoDefaultCtor.class));

        // reflective instantiation fails -> eventInstance(...) returns null -> dispatch() (hence the
        // registry) is never even reached
        verifyNoInteractions(componentRegistry);
    }

    /**
     * Exercises every branch of the private {@code EventInvocationHandler#defaultValueFor(Class)}
     * helper (54% instruction / 37% branch before this test: only CRUD events, which only ever
     * declare {@code void execute(...)} methods, were driven elsewhere in this file). Delivers a
     * {@link PrimitiveReturningEvent} - one method per JDK primitive return type, plus
     * {@code void} and {@link String} - and invokes every method on the proxy the listener actually
     * received.
     * <p>
     * <b>Why every primitive needs its own branch.</b> Per the
     * {@link java.lang.reflect.InvocationHandler#invoke} contract a primitive-returning proxy
     * method requires the handler to return an instance of the EXACT corresponding wrapper class.
     * A single catch-all {@code return 0} (an {@code Integer}) is therefore NOT enough: it makes
     * {@code doByte()}/{@code doShort()} fail with a {@code ClassCastException} instead of yielding
     * the benign default. That regression was found by this very test and fixed in
     * {@code defaultValueFor}; the assertions below pin the behaviour down so it cannot come back,
     * alongside the NPE-on-null-unboxing this defensive method was written to prevent in the first
     * place.
     */
    @Test
    void proxyDefaultValueFor_coversEveryReturnTypeBranch() {
        ApplicationEventListener<Resource> listener = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PrimitiveReturningEvent.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(listener).consumerEvent(any(), captor.capture());
        PrimitiveReturningEvent event = (PrimitiveReturningEvent) captor.getValue();

        // void -> the "!isPrimitive() || void.class.equals(...)" OR-branch's second operand
        assertDoesNotThrow(event::doVoid);
        // each of the following is an explicitly-handled primitive branch; none must throw (in
        // particular none must NPE from auto-unboxing a null return value - the very reason this
        // defensive method exists)
        assertFalse(event.doBoolean());
        assertEquals((char) 0, event.doChar());
        assertEquals(0L, event.doLong());
        assertEquals(0f, event.doFloat());
        assertEquals(0d, event.doDouble());
        // int falls through to the catch-all "return 0" branch and is the one primitive for which
        // that branch actually produces the correct wrapper type (Integer)
        assertEquals(0, event.doInt());
        // Object (non-primitive) -> the "!isPrimitive()" OR-branch's first, short-circuiting operand
        assertNull(event.doObject());

        // byte/short need their own branches: the catch-all "return 0" would hand the proxy an
        // Integer, which the JDK rejects for a byte-/short-returning method (ClassCastException)
        assertEquals((byte) 0, event.doByte());
        assertEquals((short) 0, event.doShort());
    }

    // ---------------------------------------------------------------------
    // Listener-list cache (water.events.listeners.cache.ttl.ms)
    // ---------------------------------------------------------------------

    /**
     * The default MUST stay "no caching": components register and unregister at runtime (OSGi
     * lifecycle, test runtimes register listeners on the fly) and a cache would delay their
     * visibility. This test is the guard against the default silently flipping.
     */
    @Test
    void listenersCache_disabledByDefault_everyDispatchResolvesListenersAgain() {
        ApplicationEventListener<Resource> listener = mockListener();
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);
        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);

        verify(componentRegistry, times(2)).findComponents(ApplicationEventListener.class, null);
    }

    @Test
    void listenersCache_enabled_secondDispatchWithinTtlReusesTheSnapshot() {
        ApplicationEventListener<Resource> listener = mockListener();
        wireCacheTtl("60000");
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);
        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);

        // one lookup for two dispatches, and BOTH events still delivered
        verify(componentRegistry, times(1)).findComponents(ApplicationEventListener.class, null);
        verify(listener, times(2)).consumerEvent(any(), any());
    }

    @Test
    void listenersCache_expiredTtl_resolvesListenersAgain() throws InterruptedException {
        ApplicationEventListener<Resource> listener = mockListener();
        wireCacheTtl("1");
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);
        Thread.sleep(15);
        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);

        verify(componentRegistry, times(2)).findComponents(ApplicationEventListener.class, null);
    }

    /**
     * A malformed TTL must not disable event delivery, only the caching: telemetry configuration
     * mistakes cannot be allowed to break the event bus.
     */
    @Test
    void listenersCache_invalidTtlValue_cachingStaysDisabledAndDeliveryKeepsWorking() {
        ApplicationEventListener<Resource> listener = mockListener();
        wireCacheTtl("not-a-number");
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);
        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);

        verify(componentRegistry, times(2)).findComponents(ApplicationEventListener.class, null);
        verify(listener, times(2)).consumerEvent(any(), any());
    }

    @Test
    void applicationPropertiesAlreadyResolved_isUsedWithoutResolvingItFromTheRegistry() {
        ApplicationEventListener<Resource> listener = mockListener();
        ApplicationProperties props = mock(ApplicationProperties.class);
        when(props.getPropertyOrDefault(PROP_LISTENERS_CACHE_TTL_MS, "0")).thenReturn("60000");
        producer.setApplicationProperties(props);
        when(componentRegistry.findComponents(ApplicationEventListener.class, null))
                .thenReturn(List.of(listener));

        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);
        producer.produceEvent(new Resource() {
        }, PostSaveEvent.class);

        verify(componentRegistry, never()).findComponent(ApplicationProperties.class, null);
        verify(componentRegistry, times(1)).findComponents(ApplicationEventListener.class, null);
    }

    /**
     * Wires the TTL through the REGISTRY-resolved {@link ApplicationProperties} (the fallback path,
     * used when the field was not injected at startup).
     */
    private void wireCacheTtl(String ttlMillis) {
        ApplicationProperties props = mock(ApplicationProperties.class);
        when(props.getPropertyOrDefault(PROP_LISTENERS_CACHE_TTL_MS, "0")).thenReturn(ttlMillis);
        when(componentRegistry.findComponent(ApplicationProperties.class, null)).thenReturn(props);
    }

    /**
     * Concrete (non-interface) {@link Event} with an accessible default constructor: exercises the
     * {@code newInstance()} branch of {@code InMemoryApplicationEventProducer#newEventInstance}.
     */
    public static class ConcreteEvent implements Event {
    }

    /**
     * Concrete {@link Event} with NO accessible (public, no-arg) constructor: exercises the
     * reflective-failure branch, where {@code eventInstance(...)} swallows the
     * {@link ReflectiveOperationException}-wrapping {@link IllegalArgumentException} and returns
     * {@code null} rather than propagating to the emitter.
     */
    public static final class ConcreteEventNoDefaultCtor implements Event {
        private ConcreteEventNoDefaultCtor() {
        }
    }

    /**
     * Minimal {@link BaseEntity} fixture used only to invoke the proxy's no-op {@code execute(...)}.
     */
    private static final class ExecuteFixtureEntity implements BaseEntity {
        @Override
        public long getId() {
            return 1L;
        }

        @Override
        public Date getEntityCreateDate() {
            return null;
        }

        @Override
        public Date getEntityModifyDate() {
            return null;
        }

        @Override
        public Integer getEntityVersion() {
            return null;
        }

        @Override
        public void setEntityVersion(Integer entityVersion) {
            // not used by this test
        }
    }
}
