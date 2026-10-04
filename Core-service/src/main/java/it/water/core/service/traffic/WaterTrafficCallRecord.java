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

import it.water.core.api.traffic.model.TrafficCallRecord;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * @Author Aristide Cittadino.
 * Immutable implementation of {@link TrafficCallRecord} for method-style invocations
 * (REST, Api, SystemApi, persistence). Extends {@link WaterTrafficRecord} via
 * {@code @SuperBuilder} so all common traffic fields are inherited; adds the
 * call-specific (and HTTP-specific) fields, which may be null for non-REST records.
 */
@Getter
@Accessors(fluent = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class WaterTrafficCallRecord extends WaterTrafficRecord implements TrafficCallRecord {

    private final String source;
    private final String destination;
    private final String operation;
    private final String httpMethod;
    private final String path;
    private final Integer statusCode;
    private final String clientIp;
    private final String argsRef;
    /**
     * Call-tree position: {@code parentId} is null on the outermost call of a request, {@code depth}
     * is 0 there and grows with nesting. Both are filled by the S2S capture, which is the only layer
     * that knows the call stack; a REST record is by definition an entry point and leaves them at
     * their defaults.
     */
    private final String parentId;
    private final int depth;
}
