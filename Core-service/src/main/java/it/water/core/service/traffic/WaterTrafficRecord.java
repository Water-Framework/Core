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

import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficRecord;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * @Author Aristide Cittadino.
 * Immutable, vendor-agnostic implementation of {@link TrafficRecord}, built through a
 * {@code @SuperBuilder}. Accessors are fluent (e.g. {@code recordId()}) to match the
 * {@link TrafficRecord} contract. {@link WaterTrafficCallRecord} extends this class to
 * reuse the common fields without duplication.
 */
@Getter
@Accessors(fluent = true)
@SuperBuilder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class WaterTrafficRecord implements TrafficRecord {

    private final String recordId;
    private final RecordType recordType;
    private final Instant timestamp;
    private final String serviceName;
    private final String moduleId;
    private final String nodeId;
    private final String correlationId;
    private final String traceId;
    private final String identity;
    private final Long tenantId;
    private final Outcome outcome;
    private final String errorType;
    private final String errorMessage;
    private final long durationMillis;
    private final Map<String, String> metadata;
    @Builder.Default
    private final int schemaVersion = 1;
}
