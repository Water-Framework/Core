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

import it.water.core.api.model.events.ApplicationEventListener;
import it.water.core.api.model.events.Event;
import it.water.core.api.traffic.model.TrafficRecord;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test double used by {@link TrafficReporterEgressHarnessTest} to prove that
 * {@link InMemoryTrafficPublisher} dispatches traffic events end-to-end through the real
 * {@code ComponentRegistry}. Registered manually from the harness test's {@code @BeforeAll} (NOT
 * via {@code @FrameworkComponent} classpath scanning), mirroring the established
 * "register a test double post-startup" pattern already used by
 * {@code WaterRepositoryServiceTest#initializeTestFramework} in the Repository module.
 */
class TestTrafficEventListener implements ApplicationEventListener<TrafficRecord> {

    static final List<Captured> CAPTURED = new CopyOnWriteArrayList<>();

    static void reset() {
        CAPTURED.clear();
    }

    @Override
    public void consumerEvent(TrafficRecord resource, Event event) {
        CAPTURED.add(new Captured(resource, event));
    }

    @Override
    public void consumerDetailedEvent(TrafficRecord beforeResource, TrafficRecord afterResource, Event event) {
        // not used by the traffic pipeline: TrafficReporter/InMemoryTrafficPublisher only ever call
        // consumerEvent(...) for traffic records.
    }

    static final class Captured {
        final TrafficRecord record;
        final Event event;

        Captured(TrafficRecord record, Event event) {
            this.record = record;
            this.event = event;
        }
    }
}
