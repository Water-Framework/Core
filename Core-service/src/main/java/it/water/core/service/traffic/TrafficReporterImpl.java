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
import it.water.core.api.bundle.Runtime;
import it.water.core.api.service.cluster.ClusterNodeOptions;
import it.water.core.api.traffic.TrafficPublisher;
import it.water.core.api.traffic.TrafficReporter;
import it.water.core.api.traffic.TrafficReporterStats;
import it.water.core.api.traffic.events.TrafficEvent;
import it.water.core.api.traffic.model.TrafficCallRecord;
import it.water.core.api.traffic.model.TrafficDomainEventRecord;
import it.water.core.api.traffic.model.TrafficRecord;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.service.traffic.policy.TrafficRecordPolicy;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author Aristide Cittadino.
 * Default {@link TrafficReporter}: the cross-cutting entry point of the traffic pipeline. It
 * short-circuits when reporting is disabled (ADR-11) and otherwise wraps the incoming record in a
 * traffic event and hands it to the configured {@link TrafficPublisher} for fire-and-forget egress.
 * The reporter itself performs no blocking or I/O work.
 */
@FrameworkComponent(services = {TrafficReporter.class, TrafficReporterStats.class})
public class TrafficReporterImpl implements TrafficReporter, TrafficReporterStats {

    private static final Logger log = LoggerFactory.getLogger(TrafficReporterImpl.class);

    private static final String PROP_ENABLED = "water.traffic.enabled";

    /**
     * Monotonic counters of the choke point (F5). Plain atomics: written on every reported record, read
     * only by a monitoring consumer, so contention is irrelevant and a lock would be the wrong trade.
     */
    private final AtomicLong recordsReceived = new AtomicLong();
    private final AtomicLong recordsReported = new AtomicLong();
    private final AtomicLong recordsSampledOut = new AtomicLong();
    private final AtomicLong recordsFailed = new AtomicLong();

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    @Inject
    @Setter
    private TrafficPublisher trafficPublisher;

    @Inject
    @Setter
    private Runtime waterRuntime;

    @Inject
    @Setter
    private ClusterNodeOptions clusterNodeOptions;

    @Override
    public boolean isEnabled() {
        return Boolean.parseBoolean(applicationProperties.getPropertyOrDefault(PROP_ENABLED, "false"));
    }

    @Override
    public void report(TrafficRecord record) {
        report(record, null);
    }

    @Override
    public void reportCall(TrafficCallRecord record) {
        reportCall(record, null);
    }

    @Override
    public void reportDomainEvent(TrafficDomainEventRecord record) {
        reportDomainEvent(record, null);
    }

    @Override
    public <P> void report(TrafficRecord record, P payload) {
        publishNormalized(record, normalized -> new WaterTrafficEvent<>(normalized, payload, payloadTypeOf(payload)));
    }

    @Override
    public <P> void reportCall(TrafficCallRecord record, P payload) {
        publishNormalized(record, normalized -> new WaterTrafficCallEvent<>(normalized, payload, payloadTypeOf(payload)));
    }

    @Override
    public <P> void reportDomainEvent(TrafficDomainEventRecord record, P payload) {
        publishNormalized(record, normalized -> new WaterTrafficDomainEvent<>(normalized, payload, payloadTypeOf(payload)));
    }

    /**
     * The one path all three report methods share: count, normalize, publish - and never let a failure
     * escape.
     * <p>
     * Swallowing here rather than propagating is deliberate: every caller is a capture point wrapped
     * around someone else's business call, and a telemetry failure must not surface there. The price of
     * silence is that a broken pipeline would look like an idle one, which is exactly what
     * {@link #recordsFailed()} is for.
     */
    private <T extends TrafficRecord> void publishNormalized(T record, java.util.function.Function<T, TrafficEvent<?>> eventFactory) {
        if (record == null || !isEnabled()) {
            return;
        }
        recordsReceived.incrementAndGet();
        try {
            T normalized = applyPolicies(record);
            if (normalized == null) {
                recordsSampledOut.incrementAndGet();
                return;
            }
            trafficPublisher.publish(eventFactory.apply(normalized));
            recordsReported.incrementAndGet();
        } catch (Exception e) {
            recordsFailed.incrementAndGet();
            log.warn("Traffic reporting failed for record {}, dropping it: {}", record.recordId(), e.getMessage(), e);
        }
    }

    @Override
    public long recordsReceived() {
        return recordsReceived.get();
    }

    @Override
    public long recordsReported() {
        return recordsReported.get();
    }

    @Override
    public long recordsSampledOut() {
        return recordsSampledOut.get();
    }

    @Override
    public long recordsFailed() {
        return recordsFailed.get();
    }

    /**
     * The single gate every record goes through, whatever the capture source: master switch,
     * enrichment + redaction, then the sampling verdict (in that order - sampling is head-based on
     * the correlation id, which enrichment is what fills in).
     *
     * @return the record to publish, or null when it must be dropped
     */
    private <T extends TrafficRecord> T applyPolicies(T record) {
        if (record == null || !isEnabled()) {
            return null;
        }
        TrafficRecordPolicy policy = policy();
        T normalized = policy.normalize(record);
        return policy.shouldReport(normalized) ? normalized : null;
    }

    /**
     * Built per report and not cached: the collaborators are {@code @Inject}-ed lazily on each
     * proxied call, so a policy pinned at construction time could capture nulls (or stale
     * components after a re-registration). The object is a thin, stateless holder.
     */
    private TrafficRecordPolicy policy() {
        return new TrafficRecordPolicy(applicationProperties, waterRuntime, clusterNodeOptions);
    }

    /**
     * Resolves the runtime payload type, guarding against a null payload. The unchecked cast is safe:
     * {@code payload} is a {@code P}, hence its runtime class is a {@code Class<? extends P>} which we
     * expose as {@code Class<P>} on the event.
     */
    @SuppressWarnings("unchecked")
    private <P> Class<P> payloadTypeOf(P payload) {
        return payload != null ? (Class<P>) payload.getClass() : null;
    }
}
