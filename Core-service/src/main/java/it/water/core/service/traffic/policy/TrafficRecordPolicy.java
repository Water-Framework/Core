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

package it.water.core.service.traffic.policy;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.service.cluster.ClusterNodeOptions;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficRecord;
import it.water.core.service.traffic.TrafficCorrelationContext;
import it.water.core.service.traffic.WaterTrafficCallRecord;
import it.water.core.service.traffic.WaterTrafficRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * @Author Aristide Cittadino.
 * Cross-cutting policies applied to EVERY traffic record on its way out (F4). It sits behind the
 * {@code TrafficReporter}, which is the one choke point all three capture sources funnel through,
 * so a source never has to know about identity, tenancy, correlation, privacy or sampling - and the
 * three of them cannot drift apart in how they apply them.
 * <p>
 * Three concerns, applied in this order:
 * <ol>
 *   <li><b>Enrichment</b> - fills the common fields the sources cannot know or should not have to
 *       repeat: identity and tenant from the {@link SecurityContext} (lenient: null when anonymous
 *       or when multitenancy is off), node id from {@link ClusterNodeOptions}, service name from
 *       configuration, and the {@code correlationId} from the current
 *       {@link TrafficCorrelationContext} scope. Enrichment NEVER overwrites a value the source
 *       already set - the source is closer to the truth.</li>
 *   <li><b>Redaction</b> (ADR-4) - privacy by default: the error message is truncated, and unless
 *       {@code water.traffic.payload.capture} is explicitly enabled the payload/args reference is
 *       dropped. No payload ever leaves this pipeline by accident.</li>
 *   <li><b>Sampling</b> (ADR-5) - head-based and DETERMINISTIC on the {@code correlationId}, so a
 *       whole request tree shares one verdict: a sampled REST call is never published with its
 *       service-to-service children missing (ADR-7). Which is also why sampling must run AFTER
 *       enrichment, when the correlation id is known.</li>
 * </ol>
 * <p>
 * Fail-open on the record, fail-closed on nothing: any unexpected error while applying a policy
 * leaves the record as it was rather than dropping it, because telemetry that silently eats records
 * is worse than telemetry that occasionally under-enriches one.
 */
public class TrafficRecordPolicy {

    private static final Logger log = LoggerFactory.getLogger(TrafficRecordPolicy.class);

    public static final String PROP_SAMPLING_RATE = "water.traffic.sampling.rate";
    public static final String PROP_SAMPLING_SCOPE = "water.traffic.sampling.scope";
    public static final String PROP_PAYLOAD_CAPTURE = "water.traffic.payload.capture";
    public static final String PROP_SERVICE_NAME = "water.traffic.service.name";

    static final String SCOPE_PER_TYPE = "per-type";
    static final int ERROR_MESSAGE_MAX_LENGTH = 200;
    /**
     * Sampling granularity: a rate is resolved against this many buckets, so rates down to 0.0001
     * are honoured exactly instead of being rounded to nothing.
     */
    static final int SAMPLING_BUCKETS = 10000;

    private final ApplicationProperties applicationProperties;
    private final Runtime waterRuntime;
    private final ClusterNodeOptions clusterNodeOptions;

    public TrafficRecordPolicy(ApplicationProperties applicationProperties, Runtime waterRuntime,
                               ClusterNodeOptions clusterNodeOptions) {
        this.applicationProperties = applicationProperties;
        this.waterRuntime = waterRuntime;
        this.clusterNodeOptions = clusterNodeOptions;
    }

    /**
     * Enriches and redacts the record. Returns a NEW instance of the same concrete type, or the
     * record untouched when its implementation is not one of the framework's own (a third-party
     * {@link TrafficRecord} cannot be rebuilt, so it is passed through rather than rejected).
     *
     * @param record the record as produced by a capture source
     * @param <T>    concrete record type, preserved across the rebuild
     * @return the normalized record
     */
    @SuppressWarnings("unchecked")
    public <T extends TrafficRecord> T normalize(T record) {
        if (!(record instanceof WaterTrafficRecord)) {
            return record;
        }
        try {
            WaterTrafficRecord waterRecord = (WaterTrafficRecord) record;
            WaterTrafficRecord.WaterTrafficRecordBuilder<?, ?> builder = waterRecord.toBuilder();
            enrich(waterRecord, builder);
            redact(waterRecord, builder);
            return (T) builder.build();
        } catch (Exception e) {
            log.warn("Traffic record normalization failed, reporting the record as captured: {}", e.getMessage());
            return record;
        }
    }

    /**
     * Head-based sampling decision (ADR-5).
     *
     * @param record the NORMALIZED record - its correlation id is what the decision hashes on
     * @return true when the record must be published
     */
    public boolean shouldReport(TrafficRecord record) {
        try {
            double rate = samplingRate(record);
            if (rate >= 1.0d) {
                return true;
            }
            if (rate <= 0.0d) {
                return false;
            }
            return bucketOf(samplingKey(record)) < (int) (rate * SAMPLING_BUCKETS);
        } catch (Exception e) {
            // an unusable sampling configuration must not silently blackhole the telemetry
            log.warn("Traffic sampling decision failed, reporting the record: {}", e.getMessage());
            return true;
        }
    }

    private void enrich(WaterTrafficRecord record, WaterTrafficRecord.WaterTrafficRecordBuilder<?, ?> builder) {
        SecurityContext securityContext = securityContext();
        if (record.identity() == null && securityContext != null) {
            builder.identity(securityContext.getLoggedUsername());
        }
        // lenient multitenancy: null whenever tenancy is off or the session is not tenant-scoped
        if (record.tenantId() == null && securityContext != null) {
            builder.tenantId(securityContext.getActiveCompanyId());
        }
        if (record.correlationId() == null) {
            builder.correlationId(TrafficCorrelationContext.current());
        }
        if (record.nodeId() == null && clusterNodeOptions != null) {
            builder.nodeId(clusterNodeOptions.getNodeId());
        }
        if (record.serviceName() == null) {
            builder.serviceName(serviceName());
        }
    }

    private void redact(WaterTrafficRecord record, WaterTrafficRecord.WaterTrafficRecordBuilder<?, ?> builder) {
        if (record.errorMessage() != null) {
            builder.errorMessage(truncate(record.errorMessage()));
        }
        // ADR-4: no payload/args unless explicitly opted in
        if (!payloadCaptureEnabled() && record instanceof WaterTrafficCallRecord
                && builder instanceof WaterTrafficCallRecord.WaterTrafficCallRecordBuilder) {
            ((WaterTrafficCallRecord.WaterTrafficCallRecordBuilder<?, ?>) builder).argsRef(null);
        }
    }

    /**
     * Resolves the rate to apply: with {@code scope=per-type} a
     * {@code water.traffic.sampling.rate.<recordType>} override wins for that layer (e.g.
     * {@code water.traffic.sampling.rate.rest=0.1} to thin out only the REST firehose), otherwise
     * the global rate applies.
     */
    private double samplingRate(TrafficRecord record) {
        double globalRate = doubleProperty(PROP_SAMPLING_RATE, 1.0d);
        if (!SCOPE_PER_TYPE.equalsIgnoreCase(stringProperty(PROP_SAMPLING_SCOPE, SCOPE_PER_TYPE))) {
            return globalRate;
        }
        RecordType recordType = record != null ? record.recordType() : null;
        if (recordType == null) {
            return globalRate;
        }
        return doubleProperty(PROP_SAMPLING_RATE + "." + recordType.name().toLowerCase(Locale.ROOT), globalRate);
    }

    /**
     * The correlation id is the sampling key so that every record of one request tree shares the
     * same verdict; a record captured outside any correlation scope falls back to its own id, which
     * makes the decision random-but-stable for that single record.
     */
    private String samplingKey(TrafficRecord record) {
        if (record == null) {
            return "";
        }
        return record.correlationId() != null ? record.correlationId() : String.valueOf(record.recordId());
    }

    private int bucketOf(String key) {
        // Math.abs(Integer.MIN_VALUE) is negative: mask the sign bit instead
        return (key.hashCode() & Integer.MAX_VALUE) % SAMPLING_BUCKETS;
    }

    private boolean payloadCaptureEnabled() {
        return Boolean.parseBoolean(stringProperty(PROP_PAYLOAD_CAPTURE, "false"));
    }

    private String serviceName() {
        String configured = stringProperty(PROP_SERVICE_NAME, "");
        return configured != null && !configured.isBlank() ? configured.trim() : null;
    }

    private SecurityContext securityContext() {
        try {
            return waterRuntime != null ? waterRuntime.getSecurityContext() : null;
        } catch (Exception e) {
            log.debug("No security context available while enriching a traffic record");
            return null;
        }
    }

    private String truncate(String message) {
        return message.length() <= ERROR_MESSAGE_MAX_LENGTH
                ? message
                : message.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }

    private String stringProperty(String name, String defaultValue) {
        return applicationProperties != null ? applicationProperties.getPropertyOrDefault(name, defaultValue) : defaultValue;
    }

    private double doubleProperty(String name, double defaultValue) {
        String raw = stringProperty(name, null);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid value for {}, falling back to {}", name, defaultValue);
            return defaultValue;
        }
    }
}
