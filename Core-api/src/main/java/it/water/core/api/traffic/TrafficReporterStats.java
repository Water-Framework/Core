
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

package it.water.core.api.traffic;

/**
 * @Author Aristide Cittadino.
 * Counters of the {@link TrafficReporter}, the choke point every captured record crosses whatever its
 * source (REST, service-to-service, domain events).
 * <p>
 * Complements {@link TrafficPublisherStats}, which counts what happens on the way OUT (queueing,
 * overflow, delivery). Together they answer the question a silent pipeline always raises: is nothing
 * arriving, or is something dropping it? A reporter that received nothing points at the capture
 * (disabled switch, scope, exclusions); records received but not reported point at the sampling; records
 * reported but not published point at the egress.
 * <p>
 * Optional diagnostic contract, deliberately kept off the base {@link TrafficReporter} surface: the
 * reporter's job is to report, and a monitoring consumer resolves this interface from the registry when
 * it wants the numbers. Counters are monotonic and cheap (plain atomic increments) - they are read
 * far less often than they are written.
 */
public interface TrafficReporterStats {

    /**
     * Records handed to the reporter while reporting was enabled. Does NOT count what the capture never
     * produced: a call outside the traced layers, or excluded, never reaches the reporter at all.
     */
    long recordsReceived();

    /**
     * Records that made it through the policy and were handed to the publisher.
     */
    long recordsReported();

    /**
     * Records dropped by the sampling decision. Expected to be non-zero whenever
     * {@code water.traffic.sampling.rate} is below 1: this is throughput being traded for volume, not
     * an error.
     */
    long recordsSampledOut();

    /**
     * Records lost to a failure inside the reporter itself (policy or publish). Always a defect worth
     * investigating: telemetry is fail-safe, so these are swallowed to protect the caller and would be
     * invisible without this counter.
     */
    long recordsFailed();
}
