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
import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.api.model.events.ApplicationEventListener;
import it.water.core.api.model.events.Event;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.traffic.TrafficPublisher;
import it.water.core.api.traffic.TrafficPublisherComponentProperties;
import it.water.core.api.traffic.TrafficPublisherStats;
import it.water.core.api.traffic.events.TrafficEvent;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author Aristide Cittadino.
 * Default, in-memory {@link TrafficPublisher}. It buffers incoming {@link TrafficEvent}s in a
 * bounded {@link ArrayBlockingQueue} and drains them on a single daemon worker thread, dispatching
 * each event to every registered {@link ApplicationEventListener}.
 * <p>
 * Registered with {@code priority = 0} (below the framework default of 1) so it acts as the
 * fall-back egress: any dedicated {@code TrafficPublisher} (broker, HTTP, ...) declared with a
 * higher priority transparently overrides it.
 * <p>
 * Contract guarantees:
 * <ul>
 *   <li>{@link #publish(TrafficEvent)} is non-blocking and NEVER throws: on a full queue it counts
 *       an overflow drop, on any unexpected error it counts an error drop.</li>
 *   <li>Listener dispatch errors are isolated per listener (counted as error drops, logged at WARN)
 *       so one failing listener never stops delivery to the others.</li>
 * </ul>
 * <p>
 * KNOWN LIMITATION: due to Java generics erasure, {@code findComponents(ApplicationEventListener.class, ...)}
 * returns ALL registered listeners regardless of their {@code <T extends Resource>} bound, so every
 * listener receives every traffic record. Exceptions are always isolated, so a listener that does not
 * understand a record cannot destabilize the pipeline. Note the consequence for
 * {@code TrafficDomainEventListener}, which is itself an {@code ApplicationEventListener}: it receives
 * back every record published here and MUST discard it (it does, via an explicit loop guard), otherwise
 * the pipeline would endlessly feed on its own output.
 */
@FrameworkComponent(priority = 0, properties = {
        TrafficPublisherComponentProperties.TRAFFIC_PUBLISHER_IMPLEMENTATION_PROP + "="
                + TrafficPublisherComponentProperties.TRAFFIC_PUBLISHER_IN_MEMORY_IMPLEMENTATION})
public class InMemoryTrafficPublisher implements TrafficPublisher, TrafficPublisherStats {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTrafficPublisher.class);

    private static final String PROP_QUEUE_SIZE = "water.traffic.publisher.queue.size";
    private static final int DEFAULT_QUEUE_SIZE = 10000;
    private static final long POLL_TIMEOUT_SECONDS = 1L;

    /**
     * Backs the {@code implementation} component property. In the Spring runtime component
     * properties are bound to real BEAN properties, so a component declaring one MUST expose a
     * matching getter/setter with a plain (dot-free) name - otherwise the whole application
     * context fails to start with a {@code NotWritablePropertyException}. Same convention as
     * {@code PermissionManagerDefault} and {@code ServiceDiscoveryRegistryInMemoryServer}.
     */
    @Getter
    @Setter
    private String implementation = TrafficPublisherComponentProperties.TRAFFIC_PUBLISHER_IN_MEMORY_IMPLEMENTATION;

    @Inject
    @Setter
    private ComponentRegistry componentRegistry;

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    private final AtomicLong enqueued = new AtomicLong();
    private final AtomicLong published = new AtomicLong();
    private final AtomicLong droppedOverflow = new AtomicLong();
    private final AtomicLong droppedError = new AtomicLong();

    private volatile ArrayBlockingQueue<TrafficEvent<?>> queue;
    private volatile int capacity;
    private volatile boolean running;
    private Thread workerThread;

    @OnDeactivate
    public void deactivate() {
        shutdown();
    }

    /**
     * Idempotent, thread-safe worker startup. Invoked lazily from {@link #publish(TrafficEvent)}
     * on the first event: startup is deliberately NOT tied to {@code @OnActivate}, because the
     * lifecycle callback runs on the raw, un-proxied instance before Water's per-call field
     * (re-)injection has populated {@link #applicationProperties}. Deferring to the first
     * (proxied) {@code publish} guarantees the injected properties — hence the configured queue
     * capacity — are available when {@link #resolveCapacity()} runs.
     */
    private synchronized void start() {
        if (running) {
            return;
        }
        this.capacity = resolveCapacity();
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.running = true;
        this.workerThread = new Thread(this::drainLoop, "water-traffic-inmemory-publisher");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
        log.debug("InMemoryTrafficPublisher started with queue capacity {}", capacity);
    }

    private synchronized void shutdown() {
        this.running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
        log.debug("InMemoryTrafficPublisher stopping (enqueued={}, published={}, droppedOverflow={}, droppedError={})",
                enqueued.get(), published.get(), droppedOverflow.get(), droppedError.get());
    }

    private void ensureStarted() {
        if (!running) {
            start();
        }
    }

    private int resolveCapacity() {
        if (applicationProperties == null) {
            return DEFAULT_QUEUE_SIZE;
        }
        try {
            int value = Integer.parseInt(
                    applicationProperties.getPropertyOrDefault(PROP_QUEUE_SIZE, String.valueOf(DEFAULT_QUEUE_SIZE)).trim());
            return value > 0 ? value : DEFAULT_QUEUE_SIZE;
        } catch (NumberFormatException e) {
            log.warn("Invalid value for {}, falling back to default {}", PROP_QUEUE_SIZE, DEFAULT_QUEUE_SIZE);
            return DEFAULT_QUEUE_SIZE;
        }
    }

    @Override
    public void publish(TrafficEvent<?> event) {
        // Fire-and-forget: must never block and must never propagate an exception to the caller.
        if (event == null) {
            return;
        }
        try {
            ensureStarted();
            if (queue.offer(event)) {
                enqueued.incrementAndGet();
            } else {
                droppedOverflow.incrementAndGet();
            }
        } catch (Exception e) {
            droppedError.incrementAndGet();
            log.warn("InMemoryTrafficPublisher.publish failed, dropping event: {}", e.getMessage());
        }
    }

    private void drainLoop() {
        while (running || (queue != null && !queue.isEmpty())) {
            try {
                TrafficEvent<?> event = queue.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (event != null) {
                    dispatch(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void dispatch(TrafficEvent<?> event) {
        try {
            if (componentRegistry != null) {
                List<ApplicationEventListener> listeners =
                        componentRegistry.findComponents(ApplicationEventListener.class, null);
                if (listeners != null) {
                    for (ApplicationEventListener listener : listeners) {
                        try {
                            // Per-listener isolation: one failing listener must not stop the others.
                            listener.consumerEvent(event.record(), (Event) event);
                        } catch (Exception e) {
                            droppedError.incrementAndGet();
                            log.warn("Traffic listener {} failed handling event: {}",
                                    listener.getClass().getName(), e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Never let a dispatch failure kill the worker thread.
            droppedError.incrementAndGet();
            log.warn("InMemoryTrafficPublisher dispatch failed: {}", e.getMessage());
        } finally {
            published.incrementAndGet();
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String publisherType() {
        return "memory";
    }

    @Override
    public long enqueued() {
        return enqueued.get();
    }

    @Override
    public long published() {
        return published.get();
    }

    @Override
    public long droppedOverflow() {
        return droppedOverflow.get();
    }

    @Override
    public long droppedError() {
        return droppedError.get();
    }

    @Override
    public int queueSize() {
        return queue != null ? queue.size() : 0;
    }

    @Override
    public int queueCapacity() {
        return capacity;
    }
}
