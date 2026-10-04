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
import it.water.core.api.model.Resource;
import it.water.core.api.model.events.ApplicationEventListener;
import it.water.core.api.model.events.ApplicationEventProducer;
import it.water.core.api.model.events.Event;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @Author Aristide Cittadino.
 * Default, in-process {@link ApplicationEventProducer}: turns the {@code Event} TYPE handed by an
 * emitter (e.g. {@code BaseEntitySystemServiceImpl} around every CRUD operation) into an event
 * INSTANCE and delivers it to every registered {@link ApplicationEventListener}.
 * <p>
 * Registered with {@code priority = 0} (below the framework default of 1) so it acts as the
 * fall-back producer: any dedicated {@code ApplicationEventProducer} (broker-backed, out-of-process,
 * ...) declared with a higher priority transparently replaces it.
 * <p>
 * <b>Synchronous by design.</b> Delivery happens on the emitter's thread. This is required for the
 * {@code Pre*} events to mean anything at all - a "before the change" notification delivered after
 * the change has already happened is worthless - and it keeps event ordering trivially consistent
 * with the operations that produced them. Listeners that need to do slow work are expected to hand
 * it off themselves (as the traffic pipeline does: its listener only enqueues on a non-blocking
 * publisher).
 * <p>
 * <b>Delivery is OUTSIDE the persistence transaction.</b> The transactional boundary of a Water CRUD
 * operation sits on the REPOSITORY ({@code SpringBaseJpaRepositoryImpl} carries the
 * {@code @Transactional} annotations), while the events are emitted by
 * {@code BaseEntitySystemServiceImpl} one layer above, around the repository call. Therefore a
 * {@code Pre*} event is delivered BEFORE any transaction exists and a {@code Post*} event AFTER the
 * commit. Two consequences that callers must not get wrong:
 * <ul>
 *     <li>a {@code Pre*} listener cannot veto the operation nor enlist in its rollback: throwing
 *     changes nothing (see fail-safe below) and the write proceeds;</li>
 *     <li>a {@code Post*} listener runs on already-durable state, so a failure there cannot be
 *     compensated by the framework - it must be handled by the listener itself.</li>
 * </ul>
 * Making the events transactional would require moving the transaction boundary up to the system
 * service, which is a deliberate architectural change and not something this producer can decide.
 * <p>
 * <b>Fail-safe.</b> A listener that throws can never break the emitter, nor stop delivery to the
 * other listeners: every dispatch is isolated and failures are logged at WARN.
 * <p>
 * <b>Event instances.</b> Water events are declared as INTERFACES ({@code PostSaveEvent}, ...) and
 * the API passes their {@link Class}, not an instance. This producer therefore materializes a
 * stateless JDK proxy per event type (cached, since it carries no state: the affected resource
 * travels as a separate argument of the listener callback). The event's own {@code execute(...)}
 * method is a deliberate no-op: no component in the framework implements the event interfaces as
 * handlers, the {@link ApplicationEventListener} callbacks are the delivery channel.
 */
@FrameworkComponent(priority = 0)
public class InMemoryApplicationEventProducer implements ApplicationEventProducer {

    private static final Logger log = LoggerFactory.getLogger(InMemoryApplicationEventProducer.class);

    /**
     * TTL, in milliseconds, of the cached listener list. Every CRUD operation of every entity goes
     * through this producer, and each dispatch would otherwise resolve the listeners from the
     * registry again - an operation that in Spring means {@code getBeansOfType} plus a sort over the
     * matching bean definitions, uncached.
     * <p>
     * Caching is DISABLED by default ({@code 0}) because components can register and unregister at
     * runtime - the OSGi service lifecycle does exactly that, and the test runtimes register
     * listeners on the fly - and a cache would delay their visibility. Setting a small positive TTL
     * trades a bounded amount of staleness for the lookup: appropriate for a deployed runtime whose
     * component set is stable after startup.
     */
    private static final String PROP_LISTENERS_CACHE_TTL_MS = "water.events.listeners.cache.ttl.ms";

    private static final long NANOS_PER_MILLI = 1_000_000L;

    /**
     * Injected ONCE at startup: this producer is a framework component but NOT a Water
     * {@code Service}, so it is registered un-proxied and the lazy, per-invocation field injection
     * never runs on it - a plain {@code @Inject} would silently stay null.
     */
    @Inject(injectOnceAtStartup = true)
    @Setter
    @Getter
    private ComponentRegistry componentRegistry;

    /**
     * Resolved LAZILY (on first use) and then kept: it is a runtime-wide singleton, so paying a
     * registry lookup for it on every dispatch would buy nothing.
     * <p>
     * Deliberately NOT an {@code @Inject(injectOnceAtStartup = true)} field: in OSGi this component
     * can be activated before {@code ApplicationProperties} is registered, and a startup injection
     * would then fail the activator and take the whole container down with it.
     */
    @Setter
    @Getter
    private volatile ApplicationProperties applicationProperties;

    /**
     * One stateless event instance per event type. Safe to share: the proxy holds no state and the
     * resource always travels as a separate argument.
     */
    private final Map<Class<?>, Event> eventInstances = new ConcurrentHashMap<>();

    /**
     * Last resolved listener list, or {@code null} when nothing has been cached yet. Volatile and
     * swapped as a whole: never mutated in place.
     */
    private volatile ListenersSnapshot listenersSnapshot;

    @Override
    public <T extends Resource, K extends Event> void produceEvent(T resource, Class<K> eventClass) {
        Event event = eventInstance(eventClass);
        if (event == null) {
            return;
        }
        dispatch(listener -> listener.consumerEvent(resource, event), eventClass);
    }

    @Override
    public <T extends Resource, K extends Event> void produceDetailedEvent(T beforeResource, T afterResource, Class<K> eventClass) {
        Event event = eventInstance(eventClass);
        if (event == null) {
            return;
        }
        dispatch(listener -> listener.consumerDetailedEvent(beforeResource, afterResource, event), eventClass);
    }

    /**
     * Delivers to every registered listener, isolating each one.
     * <p>
     * NOTE: due to generics erasure {@code findComponents(ApplicationEventListener.class, ...)}
     * returns ALL listeners regardless of their {@code <T extends Resource>} bound, so a listener
     * receives events about resources it may not understand. Listeners are expected to check the
     * resource type they get; a listener that throws is isolated here anyway.
     */
    @SuppressWarnings("rawtypes")
    private void dispatch(Consumer<ApplicationEventListener> delivery, Class<?> eventClass) {
        if (componentRegistry == null) {
            return;
        }
        List<ApplicationEventListener> listeners = resolveListeners(eventClass);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (ApplicationEventListener listener : listeners) {
            try {
                delivery.accept(listener);
            } catch (Exception e) {
                log.warn("Listener {} failed handling event {}: {}",
                        listener.getClass().getName(), eventClass.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Resolves the listeners, optionally through the short-lived cache described on
     * {@link #PROP_LISTENERS_CACHE_TTL_MS}.
     */
    @SuppressWarnings("rawtypes")
    private List<ApplicationEventListener> resolveListeners(Class<?> eventClass) {
        long ttlMillis = listenersCacheTtlMillis();
        if (ttlMillis <= 0) {
            return lookupListeners(eventClass);
        }
        ListenersSnapshot snapshot = listenersSnapshot;
        long now = System.nanoTime();
        if (snapshot != null && now - snapshot.takenAtNanos < ttlMillis * NANOS_PER_MILLI) {
            return snapshot.listeners;
        }
        List<ApplicationEventListener> listeners = lookupListeners(eventClass);
        // a benign race here just means two threads did the same lookup: no state is corrupted
        listenersSnapshot = new ListenersSnapshot(listeners, now);
        return listeners;
    }

    @SuppressWarnings("rawtypes")
    private List<ApplicationEventListener> lookupListeners(Class<?> eventClass) {
        try {
            return componentRegistry.findComponents(ApplicationEventListener.class, null);
        } catch (NoComponentRegistryFoundException e) {
            log.debug("No ApplicationEventListener registered, skipping event {}", eventClass.getName());
            return Collections.emptyList();
        }
    }

    /**
     * Reads the TTL on every dispatch so it stays re-tunable at runtime; the read itself is a map
     * access, i.e. nothing compared to the registry lookup it may save.
     */
    private long listenersCacheTtlMillis() {
        ApplicationProperties props = resolveApplicationProperties();
        if (props == null) {
            return 0L;
        }
        try {
            return Long.parseLong(props.getPropertyOrDefault(PROP_LISTENERS_CACHE_TTL_MS, "0").trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid value for {}, listener caching stays disabled", PROP_LISTENERS_CACHE_TTL_MS);
            return 0L;
        }
    }

    private ApplicationProperties resolveApplicationProperties() {
        ApplicationProperties resolved = applicationProperties;
        if (resolved != null) {
            return resolved;
        }
        if (componentRegistry == null) {
            return null;
        }
        try {
            resolved = componentRegistry.findComponent(ApplicationProperties.class, null);
        } catch (Exception e) {
            // no properties yet (or no registry at all): the caller falls back to its defaults, and
            // a later call retries - nothing is cached when the lookup did not succeed
            log.debug("ApplicationProperties not resolvable yet: {}", e.getMessage());
            return null;
        }
        applicationProperties = resolved;
        return resolved;
    }

    /**
     * @Author Aristide Cittadino.
     * Immutable listener list plus the instant it was taken, swapped atomically as a whole so a
     * reader can never observe a half-updated cache.
     */
    @SuppressWarnings("rawtypes")
    private static final class ListenersSnapshot {

        private final List<ApplicationEventListener> listeners;
        private final long takenAtNanos;

        private ListenersSnapshot(List<ApplicationEventListener> listeners, long takenAtNanos) {
            this.listeners = listeners;
            this.takenAtNanos = takenAtNanos;
        }
    }

    /**
     * Materializes (and caches) an instance for the requested event type: a JDK proxy when the
     * type is an interface - the normal case for Water events - or a plain instance when a
     * concrete class with a default constructor is passed instead.
     *
     * @return the event instance, or {@code null} when no instance can be built, in which case
     * nothing is delivered rather than propagating to the emitter
     */
    private Event eventInstance(Class<? extends Event> eventClass) {
        if (eventClass == null) {
            return null;
        }
        try {
            return eventInstances.computeIfAbsent(eventClass, this::newEventInstance);
        } catch (Exception e) {
            log.warn("Cannot create an instance of event {}, skipping delivery: {}", eventClass.getName(), e.getMessage());
            return null;
        }
    }

    private Event newEventInstance(Class<?> eventClass) {
        if (eventClass.isInterface()) {
            return (Event) Proxy.newProxyInstance(
                    eventClass.getClassLoader(),
                    new Class<?>[]{eventClass},
                    new EventInvocationHandler(eventClass));
        }
        try {
            return (Event) eventClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Event class " + eventClass.getName()
                    + " is not an interface and has no accessible default constructor", e);
        }
    }

    /**
     * @Author Aristide Cittadino.
     * Invocation handler of the synthetic event instances. {@code equals}/{@code hashCode}/
     * {@code toString} keep the proxy well-behaved as a map key or in a log line; every other
     * method - in practice only the event's own {@code execute(...)} - is a no-op, because the
     * event object is a TYPED MARKER here, not a handler.
     */
    private static final class EventInvocationHandler implements InvocationHandler {

        private final Class<?> eventClass;

        private EventInvocationHandler(Class<?> eventClass) {
            this.eventClass = eventClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "equals":
                    return args != null && args.length == 1 && proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "WaterEvent[" + eventClass.getName() + "]";
                default:
                    // the event carries no behaviour: the listeners are the delivery channel
                    return defaultValueFor(method.getReturnType());
            }
        }

        /**
         * Guards the (unexpected) case of an event interface declaring a primitive-returning
         * method: returning null there would blow up on auto-unboxing inside the proxy.
         * <p>
         * Every primitive needs its OWN case: {@link java.lang.reflect.InvocationHandler#invoke}
         * requires the value returned for a primitive-returning method to be an instance of the
         * EXACT corresponding wrapper class, so a single {@code return 0} (an {@code Integer})
         * would make a {@code byte}- or {@code short}-returning method fail with a
         * {@code ClassCastException} instead of yielding the benign default.
         */
        private Object defaultValueFor(Class<?> returnType) {
            if (!returnType.isPrimitive() || void.class.equals(returnType)) {
                return null;
            }
            if (boolean.class.equals(returnType)) {
                return false;
            }
            if (char.class.equals(returnType)) {
                return (char) 0;
            }
            if (long.class.equals(returnType)) {
                return 0L;
            }
            if (float.class.equals(returnType)) {
                return 0f;
            }
            if (double.class.equals(returnType)) {
                return 0d;
            }
            if (byte.class.equals(returnType)) {
                return (byte) 0;
            }
            if (short.class.equals(returnType)) {
                return (short) 0;
            }
            return 0;
        }
    }
}
