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

import it.water.core.api.traffic.model.ChangeOperation;
import it.water.core.api.traffic.model.ChangePhase;
import it.water.core.api.traffic.model.TrafficDomainEventRecord;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * @Author Aristide Cittadino.
 * Immutable implementation of {@link TrafficDomainEventRecord}. Extends
 * {@link WaterTrafficRecord} via {@code @SuperBuilder} so all common traffic fields are
 * inherited; adds the domain-event-specific ones. {@code beforeRef}/{@code afterRef} stay null
 * for non-detailed events, which carry a single resource image.
 */
@Getter
@Accessors(fluent = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class WaterTrafficDomainEventRecord extends WaterTrafficRecord implements TrafficDomainEventRecord {

    private final String eventClass;
    private final String resourceType;
    private final String resourceId;
    private final ChangePhase changePhase;
    private final ChangeOperation changeOperation;
    private final String beforeRef;
    private final String afterRef;
}
