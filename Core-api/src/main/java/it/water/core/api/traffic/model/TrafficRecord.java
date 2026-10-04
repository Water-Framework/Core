
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

package it.water.core.api.traffic.model;

import it.water.core.api.model.Resource;

import java.time.Instant;
import java.util.Map;

/**
 * @Author Aristide Cittadino.
 * Base contract shared by all traffic records. Extends {@link Resource} so that a record
 * can travel on the Water event bus. Carries the normalized, serializable set of fields
 * common to any kind of monitored traffic.
 */
public interface TrafficRecord extends Resource {
    String recordId();

    RecordType recordType();

    Instant timestamp();

    String serviceName();

    String moduleId();

    String nodeId();

    String correlationId();

    String traceId();

    String identity();

    Long tenantId();

    Outcome outcome();

    String errorType();

    String errorMessage();

    long durationMillis();

    Map<String, String> metadata();

    int schemaVersion();
}
